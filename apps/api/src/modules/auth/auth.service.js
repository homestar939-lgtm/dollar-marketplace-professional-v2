import bcrypt from 'bcryptjs'
import { randomUUID } from 'node:crypto'
import { prisma } from '../../lib/prisma.js'
import { env } from '../../config/env.js'
import { randomToken, sha256 } from '../../lib/tokens.js'
import { signAccessToken } from '../../lib/jwt.js'
import { conflict, forbidden, notFound, unauthenticated, unprocessable } from '../../lib/errors.js'

const now = () => new Date()
const minutes = (n) => n * 60 * 1000
const days = (n) => n * 24 * 60 * 60 * 1000

const publicUser = (user) => ({
  id: user.id,
  name: user.name,
  email: user.email,
  role: user.role,
  status: user.status,
  emailVerifiedAt: user.emailVerifiedAt,
  phone: user.phone,
  locale: user.locale,
  avatarUrl: user.avatarUrl,
  commissionRate: user.commissionRate ? user.commissionRate.toFixed(4) : null,
  createdAt: user.createdAt
})

async function issueSession(user, { req, userAgent }) {
  const refreshToken = randomToken(48)
  const session = await prisma.session.create({
    data: {
      id: randomUUID(),
      userId: user.id,
      tokenHash: sha256(refreshToken),
      userAgent: userAgent || req?.userAgent || null,
      ipAddress: req?.clientIp || null,
      expiresAt: new Date(Date.now() + days(env.REFRESH_TOKEN_TTL_DAYS))
    }
  })
  return {
    accessToken: signAccessToken({ userId: user.id, role: user.role, sessionId: session.id }),
    refreshToken,
    sessionId: session.id,
    expiresIn: env.ACCESS_TOKEN_TTL
  }
}

export async function register({ name, email, password, phone, locale, req }) {
  const existing = await prisma.user.findUnique({ where: { email } })
  if (existing) throw conflict('An account with this email already exists')

  const passwordHash = await bcrypt.hash(password, env.BCRYPT_ROUNDS)
  const user = await prisma.user.create({
    data: { name, email, passwordHash, phone: phone || null, locale, role: 'BUYER', status: 'PENDING_VERIFICATION' }
  })

  const token = randomToken(32)
  await prisma.emailVerificationToken.create({
    data: { userId: user.id, tokenHash: sha256(token), expiresAt: new Date(Date.now() + minutes(env.EMAIL_VERIFICATION_TTL_MINUTES)) }
  })

  return {
    user: publicUser(user),
    verification: { sentTo: user.email, expiresInMinutes: env.EMAIL_VERIFICATION_TTL_MINUTES, ...(env.exposeTokens ? { token } : {}) }
  }
}

export async function verifyEmail({ token }) {
  const record = await prisma.emailVerificationToken.findUnique({ where: { tokenHash: sha256(token) } })
  if (!record || record.usedAt || record.expiresAt < now()) throw unprocessable('Verification link is invalid or has expired')

  const [user] = await prisma.$transaction([
    prisma.user.update({ where: { id: record.userId }, data: { status: 'ACTIVE', emailVerifiedAt: now() } }),
    prisma.emailVerificationToken.update({ where: { id: record.id }, data: { usedAt: now() } })
  ])
  return publicUser(user)
}

export async function login({ email, password, req }) {
  const user = await prisma.user.findUnique({ where: { email } })
  if (!user || !user.passwordHash) throw unauthenticated('Invalid email or password')
  const valid = await bcrypt.compare(password, user.passwordHash)
  if (!valid) throw unauthenticated('Invalid email or password')

  if (user.status === 'PENDING_VERIFICATION') throw forbidden('Please verify your email address before signing in')
  if (user.status !== 'ACTIVE') throw forbidden(`Account is ${user.status.toLowerCase()}`)

  const tokens = await issueSession(user, { req })
  await prisma.user.update({ where: { id: user.id }, data: { updatedAt: now() } })
  return { user: publicUser(user), ...tokens }
}

export async function refresh({ refreshToken, req }) {
  const session = await prisma.session.findUnique({ where: { tokenHash: sha256(refreshToken) } })
  if (!session || session.revokedAt || session.expiresAt < now()) throw unauthenticated('Refresh token is invalid')

  const user = await prisma.user.findUnique({ where: { id: session.userId } })
  if (!user || user.status !== 'ACTIVE') throw forbidden('Account is not active')

  await prisma.session.update({ where: { id: session.id }, data: { revokedAt: now() } })
  return { user: publicUser(user), ...(await issueSession(user, { req })) }
}

export async function logout({ sessionId }) {
  await prisma.session.updateMany({ where: { id: sessionId, revokedAt: null }, data: { revokedAt: now() } })
  return { revoked: true }
}

export async function listSessions(userId) {
  const sessions = await prisma.session.findMany({ where: { userId }, orderBy: { createdAt: 'desc' } })
  return sessions.map((s) => ({
    id: s.id, userAgent: s.userAgent, ipAddress: s.ipAddress, createdAt: s.createdAt,
    expiresAt: s.expiresAt, revokedAt: s.revokedAt
  }))
}

export async function forgotPassword({ email }) {
  const user = await prisma.user.findUnique({ where: { email } })
  // Always answer the same way: never leak whether an account exists.
  if (!user) return { delivered: true }

  const token = randomToken(32)
  await prisma.passwordResetToken.create({
    data: { userId: user.id, tokenHash: sha256(token), expiresAt: new Date(Date.now() + minutes(env.PASSWORD_RESET_TTL_MINUTES)) }
  })
  return { delivered: true, ...(env.exposeTokens ? { token } : {}) }
}

export async function resetPassword({ token, password }) {
  const record = await prisma.passwordResetToken.findUnique({ where: { tokenHash: sha256(token) } })
  if (!record || record.usedAt || record.expiresAt < now()) throw unprocessable('Reset link is invalid or has expired')

  const passwordHash = await bcrypt.hash(password, env.BCRYPT_ROUNDS)
  await prisma.$transaction([
    prisma.user.update({ where: { id: record.userId }, data: { passwordHash } }),
    prisma.passwordResetToken.update({ where: { id: record.id }, data: { usedAt: now() } }),
    prisma.session.updateMany({ where: { userId: record.userId, revokedAt: null }, data: { revokedAt: now() } })
  ])
  return { reset: true }
}

export async function changePassword({ userId, currentPassword, newPassword }) {
  const user = await prisma.user.findUnique({ where: { id: userId } })
  if (!user || !user.passwordHash) throw notFound('Account not found')
  const valid = await bcrypt.compare(currentPassword, user.passwordHash)
  if (!valid) throw unauthenticated('Current password is incorrect')

  const passwordHash = await bcrypt.hash(newPassword, env.BCRYPT_ROUNDS)
  await prisma.user.update({ where: { id: userId }, data: { passwordHash } })
  return { changed: true }
}

/**
 * Google Sign-In. Identity is established by the verified ID token only; an
 * existing email account is linked on first use, a new account starts ACTIVE
 * because Google already proved ownership of the mailbox.
 */
export async function loginWithGoogle({ profile, req }) {
  const { subject, email, name, picture } = profile

  let user = await prisma.user.findUnique({ where: { googleSubject: subject } })
  if (!user) {
    const byEmail = await prisma.user.findUnique({ where: { email } })
    if (byEmail) {
      user = await prisma.user.update({
        where: { id: byEmail.id },
        data: {
          googleSubject: subject,
          status: byEmail.status === 'PENDING_VERIFICATION' ? 'ACTIVE' : byEmail.status,
          emailVerifiedAt: byEmail.emailVerifiedAt ?? now(),
          avatarUrl: byEmail.avatarUrl ?? picture
        }
      })
    } else {
      user = await prisma.user.create({
        data: {
          name: name || email, email, googleSubject: subject, avatarUrl: picture,
          role: 'BUYER', status: 'ACTIVE', emailVerifiedAt: now()
        }
      })
    }
  }

  if (user.status !== 'ACTIVE') throw forbidden(`Account is ${user.status.toLowerCase()}`)
  return { user: publicUser(user), ...(await issueSession(user, { req })) }
}

export { publicUser }
