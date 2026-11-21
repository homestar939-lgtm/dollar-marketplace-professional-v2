import bcrypt from 'bcryptjs'
import { randomUUID } from 'node:crypto'
import { prisma } from '../../lib/prisma.js'
import { env } from '../../config/env.js'
import { randomToken, sha256 } from '../../lib/tokens.js'
import { signAccessToken } from '../../lib/jwt.js'
import { forbidden, notFound, unauthenticated } from '../../lib/errors.js'

const now = () => new Date()
const days = (n) => n * 24 * 60 * 60 * 1000

export const publicUser = (user) => ({
  id: user.id,
  name: user.name,
  username: user.username,
  role: user.role,
  status: user.status,
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
      id: randomUUID(), userId: user.id, tokenHash: sha256(refreshToken),
      userAgent: userAgent || req?.userAgent || null, ipAddress: req?.clientIp || null,
      expiresAt: new Date(Date.now() + days(env.REFRESH_TOKEN_TTL_DAYS))
    }
  })
  return {
    accessToken: signAccessToken({ userId: user.id, role: user.role, sessionId: session.id }),
    refreshToken, sessionId: session.id, expiresIn: env.ACCESS_TOKEN_TTL
  }
}

export async function login({ username, password, req }) {
  const user = await prisma.user.findUnique({ where: { username } })
  if (!user?.passwordHash || !(await bcrypt.compare(password, user.passwordHash))) {
    throw unauthenticated('اسم المستخدم أو كلمة المرور غير صحيحين')
  }
  if (user.status !== 'ACTIVE') throw forbidden('الحساب غير نشط')
  const tokens = await issueSession(user, { req })
  await prisma.user.update({ where: { id: user.id }, data: { updatedAt: now() } })
  return { user: publicUser(user), ...tokens }
}

export async function refresh({ refreshToken, req }) {
  const session = await prisma.session.findUnique({ where: { tokenHash: sha256(refreshToken) } })
  if (!session || session.revokedAt || session.expiresAt < now()) throw unauthenticated('رمز الجلسة غير صالح')
  const user = await prisma.user.findUnique({ where: { id: session.userId } })
  if (!user || user.status !== 'ACTIVE') throw forbidden('الحساب غير نشط')
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
    id: s.id, userAgent: s.userAgent, ipAddress: s.ipAddress,
    createdAt: s.createdAt, expiresAt: s.expiresAt, revokedAt: s.revokedAt
  }))
}

export async function changePassword({ userId, currentPassword, newPassword }) {
  const user = await prisma.user.findUnique({ where: { id: userId } })
  if (!user?.passwordHash) throw notFound('الحساب غير موجود')
  if (!(await bcrypt.compare(currentPassword, user.passwordHash))) throw unauthenticated('كلمة المرور الحالية غير صحيحة')
  const passwordHash = await bcrypt.hash(newPassword, env.BCRYPT_ROUNDS)
  await prisma.$transaction([
    prisma.user.update({ where: { id: userId }, data: { passwordHash } }),
    prisma.session.updateMany({ where: { userId, revokedAt: null }, data: { revokedAt: now() } })
  ])
  return { changed: true }
}
