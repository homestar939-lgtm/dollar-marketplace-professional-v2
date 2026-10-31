import * as service from './agencies.service.js'
import { asyncHandler } from '../../lib/async-handler.js'

export const list = asyncHandler(async (req, res) => res.json(await service.listAgencies({ ...req.query, viewer: req.user })))
export const get = asyncHandler(async (req, res) => res.json({ agency: await service.getAgency({ agencyId: req.params.id, viewer: req.user }) }))
export const create = asyncHandler(async (req, res) => res.status(201).json({ agency: await service.createAgency({ data: req.body, actor: req.user, req }) }))
export const update = asyncHandler(async (req, res) => res.json({ agency: await service.updateAgency({ agencyId: req.params.id, data: req.body, actor: req.user, req }) }))
