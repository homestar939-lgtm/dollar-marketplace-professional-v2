import * as service from './auth.service.js'
import { asyncHandler } from '../../lib/async-handler.js'

export const login = asyncHandler(async (req, res) => {
  res.json(await service.login({ ...req.body, req }))
})
export const refresh = asyncHandler(async (req, res) => {
  res.json(await service.refresh({ ...req.body, req }))
})
export const logout = asyncHandler(async (req, res) => {
  res.json(await service.logout({ sessionId: req.session.id }))
})
export const me = asyncHandler(async (req, res) => {
  res.json({ user: service.publicUser(req.user) })
})
export const sessions = asyncHandler(async (req, res) => {
  res.json({ data: await service.listSessions(req.user.id) })
})
export const changePassword = asyncHandler(async (req, res) => {
  res.json(await service.changePassword({ userId: req.user.id, ...req.body }))
})
