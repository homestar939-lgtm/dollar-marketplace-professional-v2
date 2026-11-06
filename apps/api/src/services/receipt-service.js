import { prisma } from '../lib/prisma.js'
import { withSerializable } from '../lib/tx.js'
import { money, add, sub, mul, ZERO, str } from '../lib/money.js'
import { conflict, notFound, forbidden, unprocessable } from '../lib/errors.js'
import { recordAudit } from '../utils/audit.js'
import { postCredit } from './financial-ledger.js'
import { nextReceiptNumber } from './numbering.js'
import { resolveCommissionRate } from './commission-service.js'

async function outstandingFor(tx, buyerId) {
  const invoices = await tx.invoice.findMany({
    where: { buyerId, status: { in: ['APPROVED', 'PARTIALLY_PAID'] } },
    include: { allocations: { include: { receipt: { select: { status: true } } } } },
    orderBy: { approvedAt: 'asc' }
  })

  return invoices
    .map((invoice) => {
      const allocated = invoice.allocations
        .filter((a) => a.receipt.status === 'APPROVED')
        .reduce((acc, a) => add(acc, a.amount), ZERO)
      return {
        invoiceId: invoice.id,
        invoiceNumber: invoice.invoiceNumber,
        currency: invoice.currency,
        remaining: sub(invoice.totalAmount, allocated)
      }
    })
    .filter((i) => i.remaining.gt(0))
}

/**
 * A receipt records cash that the collector physically received. It must not
 * move the buyer's balance until an accountant approves it.
 */
export async function createReceipt({
  buyerId, collectorId, amount, currency, paymentMethod = 'CASH', collectionDate,
  invoiceIds = [], notes = null, overpaymentReason = null, allowOverpayment = false, req = null
}) {
  return withSerializable(async (tx) => {
    const collector = await tx.user.findUnique({ where: { id: collectorId } })
    if (!collector) throw notFound('Collector not found')
    if (!['COLLECTOR', 'ADMIN'].includes(collector.role)) throw forbidden('Only collectors can issue receipts')

    const buyer = await tx.user.findUnique({ where: { id: buyerId } })
    if (!buyer) throw notFound('Buyer not found')

    const value = money(amount)
    if (value.lte(0)) throw unprocessable('Receipt amount must be positive')

    const outstanding = await outstandingFor(tx, buyerId)
    const eligible = invoiceIds.length
      ? outstanding.filter((i) => invoiceIds.includes(i.invoiceId))
      : outstanding

    if (invoiceIds.length && eligible.length !== invoiceIds.length) {
      throw unprocessable('One or more invoices are not open for this buyer', { invoiceIds })
    }

    const totalOutstanding = outstanding.reduce((acc, i) => add(acc, i.remaining), ZERO)
    const overpaying = value.gt(totalOutstanding)
    if (overpaying) {
      if (!allowOverpayment) {
        throw unprocessable('Receipt exceeds the outstanding balance', {
          outstanding: str(totalOutstanding), amount: str(value), requiresOverpaymentApproval: true
        })
      }
      if (!overpaymentReason) throw unprocessable('An overpayment reason is mandatory')
    }

    // FIFO planning: oldest approved invoice first.
    let left = value
    const plan = []
    for (const invoice of eligible) {
      if (left.lte(0)) break
      const take = invoice.remaining.lt(left) ? invoice.remaining : left
      if (take.lte(0)) continue
      plan.push({ invoiceId: invoice.invoiceId, amount: str(take) })
      left = sub(left, take)
    }

    // Provisionally snapshot the rate; it is finalised at approval time.
    const provisionalRate = await resolveCommissionRate(tx, collectorId)
    const receiptNumber = await nextReceiptNumber(tx)

    const receipt = await tx.receipt.create({
      data: {
        receiptNumber,
        buyerId,
        collectorId,
        amount: value,
        currency: currency || 'USD',
        commissionRate: provisionalRate,
        commissionAmount: ZERO,
        status: 'DRAFT',
        paymentMethod,
        collectionDate: collectionDate ? new Date(collectionDate) : new Date(),
        plannedAllocations: plan,
        notes,
        overpaymentReason,
        createdById: collectorId
      }
    })

    await recordAudit(tx, {
      actorId: collectorId, action: 'receipt.create', entityType: 'Receipt', entityId: receipt.id,
      after: { receiptNumber, amount: str(value), planned: plan }, req
    })
    return receipt
  })
}

export async function sendReceipt({ receiptId, actorId, req = null }) {
  return withSerializable(async (tx) => {
    const receipt = await tx.receipt.findUnique({ where: { id: receiptId } })
    if (!receipt) throw notFound('Receipt not found')
    if (!['DRAFT', 'REJECTED'].includes(receipt.status)) throw conflict(`Receipt is ${receipt.status} and cannot be sent`)

    const updated = await tx.receipt.update({
      where: { id: receiptId },
      data: { status: 'SENT', sentAt: new Date(), rejectedAt: null, rejectionReason: null }
    })
    await tx.notification.create({
      data: { userId: receipt.buyerId, title: 'سند قبض جديد', body: `تم تسجيل سند القبض ${receipt.receiptNumber} وسيُعتمد بعد المراجعة`, type: 'RECEIPT' }
    })
    await recordAudit(tx, { actorId, action: 'receipt.send', entityType: 'Receipt', entityId: receiptId, req })
    return updated
  })
}

/** Approval is the only point where a receipt touches the ledger and the balance. */
export async function approveReceipt({ receiptId, actorId, req = null }) {
  return withSerializable(async (tx) => {
    const receipt = await tx.receipt.findUnique({ where: { id: receiptId } })
    if (!receipt) throw notFound('Receipt not found')
    if (receipt.status === 'APPROVED') throw conflict('Receipt is already approved')
    if (receipt.status !== 'SENT') throw conflict(`Only SENT receipts can be approved (current: ${receipt.status})`)

    const requestedPlan = Array.isArray(receipt.plannedAllocations) ? receipt.plannedAllocations : []
    const openInvoices = await outstandingFor(tx, receipt.buyerId)
    const requestedIds = new Set(requestedPlan.map((allocation) => allocation.invoiceId))
    const eligible = openInvoices.filter((invoice) => requestedIds.has(invoice.invoiceId))
    let remaining = money(receipt.amount)
    const plan = []
    for (const invoice of eligible) {
      if (remaining.lte(0)) break
      const allocation = invoice.remaining.lt(remaining) ? invoice.remaining : remaining
      if (allocation.lte(0)) continue
      plan.push({ invoiceId: invoice.invoiceId, amount: str(allocation) })
      remaining = sub(remaining, allocation)
    }

    // Approval-time commission snapshot: frozen forever afterwards.
    const rate = await resolveCommissionRate(tx, receipt.collectorId)
    const commission = mul(receipt.amount, rate)

    const updated = await tx.receipt.update({
      where: { id: receiptId },
      data: {
        status: 'APPROVED',
        approvedAt: new Date(),
        approvedById: actorId,
        commissionRate: rate,
        commissionAmount: commission
      }
    })

    for (const allocation of plan) {
      await tx.paymentAllocation.create({
        data: { receiptId: receipt.id, invoiceId: allocation.invoiceId, amount: money(allocation.amount) }
      })
    }

    // Keep the cached buyer balance, invoice states, and ledger in one commit.
    await tx.user.update({ where: { id: receipt.buyerId }, data: { debitBalance: { decrement: receipt.amount } } })
    for (const allocation of plan) {
      const invoice = await tx.invoice.findUnique({
        where: { id: allocation.invoiceId },
        include: { allocations: { include: { receipt: { select: { status: true } } } } }
      })
      if (!invoice) throw conflict(`Invoice ${allocation.invoiceId} no longer exists`)
      const allocated = invoice.allocations
        .filter((item) => item.receipt.status === 'APPROVED')
        .reduce((total, item) => add(total, item.amount), ZERO)
      await tx.invoice.update({
        where: { id: invoice.id },
        data: { status: allocated.gte(invoice.totalAmount) ? 'PAID' : 'PARTIALLY_PAID' }
      })
    }

    const posting = await postCredit(tx, {
      buyerId: receipt.buyerId,
      amount: receipt.amount,
      currency: receipt.currency,
      referenceType: 'RECEIPT',
      referenceId: receipt.id,
      receiptId: receipt.id,
      description: `Receipt ${receipt.receiptNumber} approved`
    })

    await tx.notification.create({
      data: { userId: receipt.buyerId, title: 'تم اعتماد سند القبض', body: `تم اعتماد سند القبض ${receipt.receiptNumber} بقيمة ${str(receipt.amount)} ${receipt.currency}`, type: 'RECEIPT' }
    })
    await recordAudit(tx, {
      actorId, action: 'receipt.approve', entityType: 'Receipt', entityId: receiptId,
      after: { transactionId: posting.id, commissionRate: str(rate), commissionAmount: str(commission), allocations: plan }, req
    })

    return { receipt: updated, transaction: posting }
  })
}

export async function rejectReceipt({ receiptId, actorId, reason, req = null }) {
  return withSerializable(async (tx) => {
    const receipt = await tx.receipt.findUnique({ where: { id: receiptId } })
    if (!receipt) throw notFound('Receipt not found')
    if (receipt.status !== 'SENT') throw conflict(`Only SENT receipts can be rejected (current: ${receipt.status})`)

    const updated = await tx.receipt.update({
      where: { id: receiptId },
      data: { status: 'REJECTED', rejectedAt: new Date(), rejectionReason: reason }
    })
    await tx.notification.create({
      data: { userId: receipt.collectorId, title: 'إعادة سند قبض للتصحيح', body: `السند ${receipt.receiptNumber} أُعيد: ${reason}`, type: 'RECEIPT' }
    })
    await recordAudit(tx, { actorId, action: 'receipt.reject', entityType: 'Receipt', entityId: receiptId, after: { reason }, req })
    return updated
  })
}

export async function voidReceipt({ receiptId, actorId, req = null }) {
  return withSerializable(async (tx) => {
    const receipt = await tx.receipt.findUnique({ where: { id: receiptId } })
    if (!receipt) throw notFound('Receipt not found')
    if (receipt.status === 'APPROVED') throw conflict('Approved receipts must be reversed through the ledger, not voided')
    if (receipt.status === 'VOIDED') throw conflict('Receipt is already voided')
    const updated = await tx.receipt.update({ where: { id: receiptId }, data: { status: 'VOIDED', voidedAt: new Date() } })
    await recordAudit(tx, { actorId, action: 'receipt.void', entityType: 'Receipt', entityId: receiptId, req })
    return updated
  })
}

export { prisma, outstandingFor }
