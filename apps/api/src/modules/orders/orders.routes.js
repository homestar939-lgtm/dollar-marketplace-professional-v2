import { Router } from 'express'
import * as controller from './orders.controller.js'
import { authenticate } from '../../middleware/authentication.js'
import { requirePermission } from '../../middleware/authorization.js'
import { validate } from '../../middleware/validation.js'
import { createOrderSchema, listOrdersSchema, prepareOrderSchema, shipOrderSchema } from './orders.validation.js'

export const ordersRouter = Router()

ordersRouter.use(authenticate())
ordersRouter.post('/', requirePermission('order.create'), validate(createOrderSchema), controller.create)
ordersRouter.get('/', requirePermission('order.read'), validate(listOrdersSchema, 'query'), controller.list)
ordersRouter.get('/:id', controller.get)
ordersRouter.post('/:id/prepare', requirePermission('order.prepare'), validate(prepareOrderSchema), controller.prepare)
ordersRouter.post('/:id/ship', requirePermission('order.ship'), validate(shipOrderSchema), controller.ship)
ordersRouter.post('/:id/cancel', controller.cancel)
