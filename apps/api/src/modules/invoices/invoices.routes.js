import { Router } from 'express'
import * as controller from './invoices.controller.js'
import { authenticate } from '../../middleware/authentication.js'
import { requirePermission } from '../../middleware/authorization.js'
import { validate } from '../../middleware/validation.js'
import { createInvoiceSchema, updateInvoiceSchema, rejectInvoiceSchema, listInvoicesSchema } from './invoices.validation.js'

export const invoicesRouter = Router()

invoicesRouter.use(authenticate())
invoicesRouter.get('/', validate(listInvoicesSchema, 'query'), controller.list)
invoicesRouter.get('/:id', controller.get)
invoicesRouter.post('/', requirePermission('invoice.create'), validate(createInvoiceSchema), controller.create)
invoicesRouter.patch('/:id', requirePermission('invoice.create'), validate(updateInvoiceSchema), controller.update)
invoicesRouter.post('/:id/send', requirePermission('invoice.send'), controller.send)
invoicesRouter.post('/:id/approve', requirePermission('invoice.approve'), controller.approve)
invoicesRouter.post('/:id/reject', requirePermission('invoice.reject'), validate(rejectInvoiceSchema), controller.reject)
invoicesRouter.post('/:id/void', requirePermission('invoice.void'), controller.voidInvoice)
