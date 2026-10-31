import * as service from './users.service.js'
import { asyncHandler } from '../../lib/async-handler.js'

export const list = asyncHandler(async (req, res) => res.json(await service.listUsers(req.query)))
export const get = asyncHandler(async (req, res) => res.json({ user: await service.getUser({ userId: req.params.id }) }))
export const create = asyncHandler(async (req, res) => res.status(201).json({ user: await service.createStaff({ data: req.body, actor: req.user, req }) }))
export const update = asyncHandler(async (req, res) => res.json({ user: await service.updateUser({ userId: req.params.id, data: req.body, actor: req.user, req }) }))
