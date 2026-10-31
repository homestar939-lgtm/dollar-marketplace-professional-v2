import * as service from './receipts.service.js'
import * as workflow from '../../services/receipt-service.js'
import { hasPermission } from '../../middleware/authorization.js'
import { asyncHandler } from '../../lib/async-handler.js'

export const create = asyncHandler(async (req, res) => {
  const receipt = await workflow.createReceipt({
    ...req.body,
    collectorId: req.user.id,
    // Overpayment needs an explicit privileged approval, never a client flag alone.
    allowOverpayment: Boolean(req.body.overpaymentReason) && hasPermission(req.user, 'settings.write'),
    req
  })
  res.status(201).json({ receipt })
})
export const send = asyncHandler(async (req, res) => res.json({ receipt: await workflow.sendReceipt({ receiptId: req.params.id, actorId: req.user.id, req }) }))
export const approve = asyncHandler(async (req, res) => res.json(await workflow.approveReceipt({ receiptId: req.params.id, actorId: req.user.id, req })))
export const reject = asyncHandler(async (req, res) => res.json({ receipt: await workflow.rejectReceipt({ receiptId: req.params.id, reason: req.body.reason, actorId: req.user.id, req }) }))
export const voidReceipt = asyncHandler(async (req, res) => res.json({ receipt: await workflow.voidReceipt({ receiptId: req.params.id, actorId: req.user.id, req }) }))
export const list = asyncHandler(async (req, res) => res.json(await service.findReceipts({ viewer: req.user, ...req.query })))
export const get = asyncHandler(async (req, res) => res.json({ receipt: await service.findReceipt({ receiptId: req.params.id, viewer: req.user }) }))
export const commission = asyncHandler(async (req, res) => res.json({ commission: await service.myCommission({ collectorId: req.user.id, from: req.query.from, to: req.query.to }) }))
