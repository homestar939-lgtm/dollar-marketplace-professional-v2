import { Router } from 'express'
import * as controller from './products.controller.js'
import { authenticate } from '../../middleware/authentication.js'
import { requirePermission } from '../../middleware/authorization.js'
import { validate } from '../../middleware/validation.js'
import { listProductsSchema, createProductSchema, updateProductSchema } from './products.validation.js'

export const productsRouter = Router()

productsRouter.use(authenticate())
productsRouter.get('/', requirePermission('product.read'), validate(listProductsSchema, 'query'), controller.list)
productsRouter.get('/:id', requirePermission('product.read'), controller.get)
productsRouter.post('/', requirePermission('product.write'), validate(createProductSchema), controller.create)
productsRouter.patch('/:id', requirePermission('product.write'), validate(updateProductSchema), controller.update)
