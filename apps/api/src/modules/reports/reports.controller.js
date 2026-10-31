import * as service from './reports.service.js'
import { asyncHandler } from '../../lib/async-handler.js'

export const collections = asyncHandler(async (req, res) => res.json(await service.collectionsReport(req.query)))
export const commissions = asyncHandler(async (req, res) => res.json(await service.commissionsReport(req.query)))
export const financial = asyncHandler(async (req, res) => res.json(await service.financialOverview(req.query)))
export const outstanding = asyncHandler(async (req, res) => res.json(await service.outstandingReport(req.query)))
