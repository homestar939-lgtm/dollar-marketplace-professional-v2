import { Router } from 'express'
import * as controller from './receipts.controller.js'
import { authenticate } from '../../middleware/authentication.js'
import { requirePermission } from '../../middleware/authorization.js'
import { validate } from '../../middleware/validation.js'
import { createReceiptSchema, rejectReceiptSchema, listReceiptsSchema } from './receipts.validation.js'

export const receiptsRouter = Router()

receiptsRouter.use(authenticate())
receiptsRouter.get('/', validate(listReceiptsSchema, 'query'), controller.list)
receiptsRouter.get('/commission/me', requirePermission('commission.read'), controller.commission)
receiptsRouter.get('/:id', controller.get)
receiptsRouter.post('/', requirePermission('receipt.create'), validate(createReceiptSchema), controller.create)
receiptsRouter.post('/:id/send', requirePermission('receipt.send'), controller.send)
receiptsRouter.post('/:id/approve', requirePermission('receipt.approve'), controller.approve)
receiptsRouter.post('/:id/reject', requirePermission('receipt.reject'), validate(rejectReceiptSchema), controller.reject)
receiptsRouter.post('/:id/void', requirePermission('receipt.void'), controller.voidReceipt)
