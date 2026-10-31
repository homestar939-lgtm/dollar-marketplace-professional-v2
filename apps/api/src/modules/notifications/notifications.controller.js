import * as service from './notifications.service.js'
import { asyncHandler } from '../../lib/async-handler.js'
import { parsePagination } from '../../utils/pagination.js'

export const list = asyncHandler(async (req, res) => {
  const { page, limit } = parsePagination(req.query)
  res.json(await service.listNotifications({ userId: req.user.id, unreadOnly: req.query.unread === 'true', page, limit }))
})
export const read = asyncHandler(async (req, res) => res.json({ notification: await service.markRead({ notificationId: req.params.id, userId: req.user.id }) }))
export const readAll = asyncHandler(async (req, res) => res.json(await service.markAllRead({ userId: req.user.id })))
