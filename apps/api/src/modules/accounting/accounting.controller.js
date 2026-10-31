import * as service from './accounting.service.js'
import { asyncHandler } from '../../lib/async-handler.js'

export const buyers = asyncHandler(async (req, res) => res.json(await service.listBuyersWithBalances(req.query)))
export const statement = asyncHandler(async (req, res) => res.json(await service.buyerStatement({ buyerId: req.params.buyerId, ...req.query })))
export const matching = asyncHandler(async (req, res) => res.json(await service.matchingQueue()))
