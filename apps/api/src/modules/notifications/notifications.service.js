import { prisma } from '../../lib/prisma.js'
import { notFound } from '../../lib/errors.js'

export async function listNotifications({ userId, unreadOnly, page, limit }) {
  const where = { userId, ...(unreadOnly ? { read: false } : {}) }
  const [items, total, unread] = await Promise.all([
    prisma.notification.findMany({ where, orderBy: { createdAt: 'desc' }, skip: (page - 1) * limit, take: limit }),
    prisma.notification.count({ where }),
    prisma.notification.count({ where: { userId, read: false } })
  ])
  return { data: items, meta: { page, limit, total, pages: Math.max(1, Math.ceil(total / limit)), unread } }
}

export async function markRead({ notificationId, userId }) {
  const notification = await prisma.notification.findUnique({ where: { id: notificationId } })
  if (!notification || notification.userId !== userId) throw notFound('Notification not found')
  return prisma.notification.update({ where: { id: notificationId }, data: { read: true } })
}

export async function markAllRead({ userId }) {
  const result = await prisma.notification.updateMany({ where: { userId, read: false }, data: { read: true } })
  return { updated: result.count }
}
