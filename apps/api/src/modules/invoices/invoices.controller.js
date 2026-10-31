import * as service from './invoices.service.js'
import * as workflow from '../../services/invoice-service.js'
import { asyncHandler } from '../../lib/async-handler.js'

export const create = asyncHandler(async (req, res) => {
  const invoice = await workflow.createInvoiceFromOrder({ orderId: req.body.orderId, lines: req.body.lines, actorId: req.user.id, req })
  res.status(201).json({ invoice })
})
export const update = asyncHandler(async (req, res) => {
  const invoice = await workflow.updateDraftInvoice({ invoiceId: req.params.id, lines: req.body.lines, actorId: req.user.id, req })
  res.json({ invoice })
})
export const send = asyncHandler(async (req, res) => res.json({ invoice: await workflow.sendInvoice({ invoiceId: req.params.id, actorId: req.user.id, req }) }))
export const approve = asyncHandler(async (req, res) => res.json(await workflow.approveInvoice({ invoiceId: req.params.id, actorId: req.user.id, req })))
export const reject = asyncHandler(async (req, res) => res.json({ invoice: await workflow.rejectInvoice({ invoiceId: req.params.id, reason: req.body.reason, actorId: req.user.id, req }) }))
export const voidInvoice = asyncHandler(async (req, res) => res.json({ invoice: await workflow.voidInvoice({ invoiceId: req.params.id, actorId: req.user.id, req }) }))
export const list = asyncHandler(async (req, res) => res.json(await service.findInvoices({ viewer: req.user, ...req.query })))
export const get = asyncHandler(async (req, res) => res.json({ invoice: await service.findInvoice({ invoiceId: req.params.id, viewer: req.user }) }))
