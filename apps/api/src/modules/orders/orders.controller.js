import * as service from './orders.service.js'
import { asyncHandler } from '../../lib/async-handler.js'

export const create = asyncHandler(async (req, res) => {
  const order = await service.createOrder({ buyerId: req.user.id, items: req.body.items, notes: req.body.notes, req })
  res.status(201).json({ order })
})
export const list = asyncHandler(async (req, res) => res.json(await service.listOrders({ viewer: req.user, ...req.query })))
export const get = asyncHandler(async (req, res) => res.json({ order: await service.getOrder({ orderId: req.params.id, viewer: req.user }) }))
export const prepare = asyncHandler(async (req, res) => res.json({ order: await service.prepareOrder({ orderId: req.params.id, ...req.body, actor: req.user, req }) }))
export const ship = asyncHandler(async (req, res) => res.json({ order: await service.shipOrder({ orderId: req.params.id, ...req.body, actor: req.user, req }) }))
export const cancel = asyncHandler(async (req, res) => res.json({ order: await service.cancelOrder({ orderId: req.params.id, viewer: req.user, req }) }))
