import { prisma } from '../../lib/prisma.js'
import { AppError } from '../../lib/errors.js'

const MAX_LIMIT = 200

const startOfToday = () => {
  const date = new Date()
  date.setHours(0, 0, 0, 0)
  return date
}

const rangeFilter = (from, to) => {
  const range = {
    ...(from ? { gte: new Date(from) } : {}),
    ...(to ? { lte: new Date(to) } : {})
  }
  return Object.keys(range).length ? range : undefined
}

/** سجل التدقيق الكامل مع ترقيم صفحي وفلترة متقدمة. */
export async function getFullAuditLog(filters = {}) {
  const page = Number(filters.page ?? 1)
  const limit = Number(filters.limit ?? 50)

  if (!Number.isInteger(page) || page < 1) {
    throw new AppError(400, 'INVALID_PAGE', 'page must be a positive integer')
  }
  if (!Number.isInteger(limit) || limit < 1 || limit > MAX_LIMIT) {
    throw new AppError(400, 'INVALID_LIMIT', `limit must be an integer between 1 and ${MAX_LIMIT}`)
  }

  const createdAt = rangeFilter(filters.from, filters.to)
  const where = {
    ...(filters.userId ? { actorId: filters.userId } : {}),
    ...(filters.action ? { action: { contains: filters.action, mode: 'insensitive' } } : {}),
    ...(filters.entityType ? { entityType: filters.entityType } : {}),
    ...(filters.entityId ? { entityId: filters.entityId } : {}),
    ...(createdAt ? { createdAt } : {})
  }

  const [total, logs] = await Promise.all([
    prisma.auditLog.count({ where }),
    prisma.auditLog.findMany({
      where,
      include: { actor: { select: { id: true, name: true, role: true, phone: true } } },
      orderBy: { createdAt: 'desc' },
      skip: (page - 1) * limit,
      take: limit
    })
  ])

  return { logs, pagination: { page, limit, total, totalPages: Math.ceil(total / limit) } }
}

/** إحصاءات التدقيق: الإجماليات وأكثر الإجراءات والمستخدمين نشاطاً. */
export async function getAuditStats({ from, to } = {}) {
  const createdAt = rangeFilter(from, to)
  const where = createdAt ? { createdAt } : {}

  const [totalLogs, todayLogs, topActions, grouped] = await Promise.all([
    prisma.auditLog.count({ where }),
    prisma.auditLog.count({ where: { createdAt: { gte: startOfToday() } } }),
    prisma.auditLog.groupBy({
      by: ['action'],
      _count: { id: true },
      orderBy: { _count: { id: 'desc' } },
      take: 10
    }),
    prisma.auditLog.groupBy({
      by: ['actorId'],
      _count: { id: true },
      orderBy: { _count: { id: 'desc' } },
      take: 10
    })
  ])

  // السجلات النظامية بلا فاعل (actorId = null) تُستبعد من استعلام الأسماء.
  const userIds = [...new Set(grouped.map((row) => row.actorId).filter((id) => typeof id === 'string' && id.length > 0))]
  const userMap = new Map()
  if (userIds.length) {
    const users = await prisma.user.findMany({
      where: { id: { in: userIds } },
      select: { id: true, name: true, role: true }
    })
    for (const user of users) userMap.set(user.id, user)
  }

  return {
    totalLogs,
    todayLogs,
    topActions: topActions.map((row) => ({ action: row.action, count: row._count.id })),
    topUsers: grouped.map((row) => ({
      actorId: row.actorId ?? null,
      count: row._count.id,
      user: userMap.get(row.actorId) ?? null
    }))
  }
}
