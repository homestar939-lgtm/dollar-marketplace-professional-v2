import bcrypt from 'bcryptjs'
import { prisma } from '../../lib/prisma.js'
import { env } from '../../config/env.js'
import { conflict, forbidden, notFound, unprocessable } from '../../lib/errors.js'
import { recordAudit } from '../../utils/audit.js'
import { publicUser } from '../auth/auth.service.js'

export async function listUsers({ role, status, q, page, limit }) {
  const where = {
    ...(role ? { role } : {}),
    ...(status ? { status } : {}),
    ...(q ? { OR: [{ name: { contains: q, mode: 'insensitive' } }, { email: { contains: q, mode: 'insensitive' } }] } : {})
  }
  const [items, total] = await Promise.all([
    prisma.user.findMany({ where, orderBy: { createdAt: 'desc' }, skip: (page - 1) * limit, take: limit }),
    prisma.user.count({ where })
  ])
  return { data: items.map(publicUser), meta: { page, limit, total, pages: Math.max(1, Math.ceil(total / limit)) } }
}

export async function createStaff({ data, actor, req }) {
  const existing = await prisma.user.findUnique({ where: { email: data.email } })
  if (existing) throw conflict('An account with this email already exists')
  if (data.commissionRate != null && data.role !== 'COLLECTOR') {
    throw unprocessable('Only collectors carry a commission rate')
  }

  const passwordHash = await bcrypt.hash(data.password, env.BCRYPT_ROUNDS)
  const user = await prisma.user.create({
    data: {
      name: data.name,
      email: data.email,
      passwordHash,
      role: data.role,
      status: 'ACTIVE',
      emailVerifiedAt: new Date(),
      phone: data.phone ?? null,
      commissionRate: data.commissionRate != null ? data.commissionRate : null,
      agencyId: data.agencyId ?? null
    }
  })
  await recordAudit(prisma, { actorId: actor.id, action: 'user.create', entityType: 'User', entityId: user.id, after: { email: user.email, role: user.role }, req })
  return publicUser(user)
}

/** A non-admin can never escalate privileges: ADMIN is only grantable by an ADMIN. */
export async function updateUser({ userId, data, actor, req }) {
  const user = await prisma.user.findUnique({ where: { id: userId } })
  if (!user) throw notFound('User not found')
  if (user.id === actor.id && data.role && data.role !== 'ADMIN') {
    throw forbidden('You cannot change your own role')
  }
  if (data.role === 'ADMIN' && actor.role !== 'ADMIN') throw forbidden('Only an admin can grant the ADMIN role')

  const updated = await prisma.user.update({
    where: { id: userId },
    data: {
      ...(data.name !== undefined ? { name: data.name } : {}),
      ...(data.phone !== undefined ? { phone: data.phone } : {}),
      ...(data.status !== undefined ? { status: data.status } : {}),
      ...(data.role !== undefined ? { role: data.role } : {}),
      ...(data.commissionRate !== undefined ? { commissionRate: data.commissionRate } : {}),
      ...(data.agencyId !== undefined ? { agencyId: data.agencyId } : {})
    }
  })

  // Deactivating an account must kill its live sessions immediately.
  if (data.status && data.status !== 'ACTIVE') {
    await prisma.session.updateMany({ where: { userId, revokedAt: null }, data: { revokedAt: new Date() } })
  }

  await recordAudit(prisma, {
    actorId: actor.id, action: 'user.update', entityType: 'User', entityId: userId,
    before: { role: user.role, status: user.status }, after: { role: updated.role, status: updated.status }, req
  })
  return publicUser(updated)
}

export async function getBuyersForCollector({ collectorId }) {
  const buyers = await prisma.user.findMany({
    where: { role: 'BUYER' },
    orderBy: { createdAt: 'desc' },
    take: 200
  })
  return buyers.map(publicUser)
}

export async function getUser({ userId }) {
  const user = await prisma.user.findUnique({ where: { id: userId } })
  if (!user) throw notFound('User not found')
  return publicUser(user)
}
