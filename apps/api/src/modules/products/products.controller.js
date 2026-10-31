import * as service from './products.service.js'
import { asyncHandler } from '../../lib/async-handler.js'

export const list = asyncHandler(async (req, res) => res.json(await service.listProducts({ ...req.query, viewer: req.user })))
export const get = asyncHandler(async (req, res) => res.json({ product: await service.getProduct({ productId: req.params.id, viewer: req.user }) }))
export const create = asyncHandler(async (req, res) => res.status(201).json({ product: await service.createProduct({ data: req.body, actor: req.user, req }) }))
export const update = asyncHandler(async (req, res) => res.json({ product: await service.updateProduct({ productId: req.params.id, data: req.body, actor: req.user, req }) }))
