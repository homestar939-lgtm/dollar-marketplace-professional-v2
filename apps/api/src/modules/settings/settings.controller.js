import * as service from './settings.service.js'
import { asyncHandler } from '../../lib/async-handler.js'

export const get = asyncHandler(async (req, res) => res.json({ settings: await service.getSettings() }))
export const update = asyncHandler(async (req, res) => res.json({ settings: await service.updateSettings({ data: req.body, actor: req.user, req }) }))
