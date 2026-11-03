import { prisma } from '../lib/prisma.js'

/**
 * خدمة الإشعارات (V3).
 * التوقيع العام يبقى { userIds, title, message, type, data } لكن الحقول المخزّنة
 * في قاعدة البيانات هي body و read و metadata كما في مخطط Prisma.
 */
class NotificationService {
  async notify({ userIds, title, message, type = 'SYSTEM', data = {} }) {
    const recipients = [...new Set((userIds ?? []).filter(Boolean))]
    if (recipients.length === 0) return { sent: 0 }

    const notifications = recipients.map((userId) => ({
      userId,
      title,
      body: message,
      type,
      metadata: data
    }))

    await prisma.notification.createMany({ data: notifications })
    return { sent: notifications.length }
  }

  async notifyRole({ role, title, message, type, data }) {
    const users = await prisma.user.findMany({
      where: { role, status: 'ACTIVE' },
      select: { id: true }
    })
    return this.notify({ userIds: users.map((user) => user.id), title, message, type, data })
  }

  async getUserNotifications(userId, { page = 1, limit = 20, unreadOnly = false } = {}) {
    const where = { userId, ...(unreadOnly ? { read: false } : {}) }
    const [notifications, total, unreadCount] = await Promise.all([
      prisma.notification.findMany({
        where,
        orderBy: { createdAt: 'desc' },
        skip: (page - 1) * limit,
        take: limit
      }),
      prisma.notification.count({ where }),
      prisma.notification.count({ where: { userId, read: false } })
    ])
    return { notifications, total, unreadCount }
  }

  async markAsRead(userId, notificationIds = null) {
    const ids = Array.isArray(notificationIds) ? notificationIds.filter(Boolean) : null
    const where = { userId, ...(ids && ids.length > 0 ? { id: { in: ids } } : {}) }
    const result = await prisma.notification.updateMany({ where, data: { read: true } })
    return { updated: result.count }
  }
}

export const notificationService = new NotificationService()
export { NotificationService }
