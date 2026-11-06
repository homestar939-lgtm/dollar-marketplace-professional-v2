import { prisma } from '../lib/prisma.js'
import { withSerializable } from '../lib/tx.js'
import { money, add, mul, ZERO } from '../lib/money.js'
import { conflict, notFound, unprocessable } from '../lib/errors.js'
import { recordAudit } from '../utils/audit.js'
import { postDebit } from './financial-ledger.js'
import { nextInvoiceNumber } from './numbering.js'

const LIVE_STATUSES = ['DRAFT', 'SENT', 'APPROVED', 'PARTIALLY_PAID', 'PAID']

function priceLine(quantity, unitPrice) {
  return mul(unitPrice, money(quantity))
}

/** Only prepared quantities may be invoiced; the price comes from the order snapshot. */
export function buildInvoiceLines(order, overrides = []) {
  const overrideMap = new Map(overrides.map((o) => [o.orderItemId, o.quantity]))
  const lines = []

  for (const item of order.items) {
    const requested = overrideMap.has(item.id) ? overrideMap.get(item.id) : item.preparedQuantity
    if (requested == null) continue
    if (requested <= 0) continue
    if (requested > item.preparedQuantity) {
      throw unprocessable(`Quantity ${requested} exceeds prepared quantity ${item.preparedQuantity} for ${item.productName}`)
    }
    if (requested > item.quantity) {
      throw unprocessable(`Quantity ${requested} exceeds ordered quantity ${item.quantity} for ${item.productName}`)
    }
    lines.push({
      orderItemId: item.id,
      productId: item.productId,
      description: item.productName,
      quantity: requested,
      unitPrice: money(item.unitPrice),
      lineTotal: priceLine(requested, item.unitPrice)
    })
  }

  if (!lines.length) throw unprocessable('No prepared quantities to invoice')
  return lines
}

const totalsOf = (lines) => lines.reduce((acc, l) => add(acc, l.lineTotal), ZERO)

export async function createInvoiceFromOrder({ orderId, actorId, lines: overrides = [], req = null }) {
  return withSerializable(async (tx) => {
    const order = await tx.order.findUnique({ where: { id: orderId }, include: { items: true, invoices: true } })
    if (!order) throw notFound('Order not found')
    if (['CANCELLED', 'RETURNED', 'REFUNDED'].includes(order.status)) throw conflict(`Order is ${order.status}`)

    const live = order.invoices.find((inv) => LIVE_STATUSES.includes(inv.status))
    if (live) throw conflict(`Order already has a live invoice (${live.invoiceNumber}, ${live.status})`)

    const preparedTotal = order.items.reduce((acc, i) => acc + (i.preparedQuantity || 0), 0)
    if (preparedTotal <= 0) throw unprocessable('Order has no prepared quantities yet')

    const lines = buildInvoiceLines(order, overrides)
    const subtotal = totalsOf(lines)
    const invoiceNumber = await nextInvoiceNumber(tx)

    const invoice = await tx.invoice.create({
      data: {
        invoiceNumber,
        orderId: order.id,
        buyerId: order.buyerId,
        status: 'DRAFT',
        subtotal,
        totalAmount: subtotal,
        currency: order.currency,
        issuedAt: new Date(),
        createdById: actorId,
        items: { create: lines }
      },
      include: { items: true }
    })

    await recordAudit(tx, {
      actorId, action: 'invoice.create', entityType: 'Invoice', entityId: invoice.id,
      after: { invoiceNumber, total: invoice.totalAmount.toString() }, req
    })
    return invoice
  })
}

export async function updateDraftInvoice({ invoiceId, actorId, lines, req = null }) {
  return withSerializable(async (tx) => {
    const invoice = await tx.invoice.findUnique({ where: { id: invoiceId }, include: { items: true, order: { include: { items: true } } } })
    if (!invoice) throw notFound('Invoice not found')
    if (invoice.status !== 'DRAFT') throw conflict(`Only DRAFT invoices can be edited (current: ${invoice.status})`)

    const rebuilt = buildInvoiceLines(invoice.order, lines)
    const subtotal = totalsOf(rebuilt)
    await tx.invoiceItem.deleteMany({ where: { invoiceId } })
    const updated = await tx.invoice.update({
      where: { id: invoiceId },
      data: { subtotal, totalAmount: subtotal, items: { create: rebuilt } },
      include: { items: true }
    })
    await recordAudit(tx, {
      actorId, action: 'invoice.update', entityType: 'Invoice', entityId: invoiceId,
      before: { total: invoice.totalAmount.toString() }, after: { total: updated.totalAmount.toString() }, req
    })
    return updated
  })
}

export async function sendInvoice({ invoiceId, actorId, req = null }) {
  return withSerializable(async (tx) => {
    const invoice = await tx.invoice.findUnique({ where: { id: invoiceId } })
    if (!invoice) throw notFound('Invoice not found')
    if (!['DRAFT', 'REJECTED'].includes(invoice.status)) throw conflict(`Invoice is ${invoice.status} and cannot be sent`)

    const updated = await tx.invoice.update({
      where: { id: invoiceId },
      data: { status: 'SENT', sentAt: new Date(), rejectionReason: null, rejectedAt: null }
    })
    await tx.notification.create({
      data: { userId: invoice.buyerId, title: 'فاتورة جديدة', body: `تم إرسال الفاتورة ${invoice.invoiceNumber} للمراجعة`, type: 'INVOICE' }
    })
    await recordAudit(tx, { actorId, action: 'invoice.send', entityType: 'Invoice', entityId: invoiceId, req })
    return updated
  })
}

/** Approval posts the DEBIT that creates the buyer's liability — exactly once. */
export async function approveInvoice({ invoiceId, actorId, req = null }) {
  return withSerializable(async (tx) => {
    const invoice = await tx.invoice.findUnique({ where: { id: invoiceId } })
    if (!invoice) throw notFound('Invoice not found')
    if (invoice.status === 'APPROVED') throw conflict('Invoice is already approved')
    if (invoice.status !== 'SENT') throw conflict(`Only SENT invoices can be approved (current: ${invoice.status})`)

    const updated = await tx.invoice.update({
      where: { id: invoiceId },
      data: { status: 'APPROVED', approvedAt: new Date(), approvedById: actorId }
    })

    const posting = await postDebit(tx, {
      buyerId: invoice.buyerId,
      amount: invoice.totalAmount,
      currency: invoice.currency,
      referenceType: 'INVOICE',
      referenceId: invoice.id,
      invoiceId: invoice.id,
      description: `Invoice ${invoice.invoiceNumber} approved`
    })
    await tx.user.update({ where: { id: invoice.buyerId }, data: { debitBalance: { increment: invoice.totalAmount } } })

    await tx.notification.create({
      data: { userId: invoice.buyerId, title: 'تم اعتماد الفاتورة', body: `تم اعتماد الفاتورة ${invoice.invoiceNumber} وإضافتها إلى رصيدك`, type: 'INVOICE' }
    })
    await recordAudit(tx, {
      actorId, action: 'invoice.approve', entityType: 'Invoice', entityId: invoiceId,
      after: { transactionId: posting.id, amount: invoice.totalAmount.toString() }, req
    })
    return { invoice: updated, transaction: posting }
  })
}

export async function rejectInvoice({ invoiceId, actorId, reason, req = null }) {
  return withSerializable(async (tx) => {
    const invoice = await tx.invoice.findUnique({ where: { id: invoiceId } })
    if (!invoice) throw notFound('Invoice not found')
    if (invoice.status !== 'SENT') throw conflict(`Only SENT invoices can be rejected (current: ${invoice.status})`)

    const updated = await tx.invoice.update({
      where: { id: invoiceId },
      data: { status: 'REJECTED', rejectedAt: new Date(), rejectionReason: reason }
    })
    await tx.notification.create({
      data: {
        userId: invoice.createdById || invoice.buyerId,
        title: 'إعادة فاتورة للتصحيح',
        body: `الفاتورة ${invoice.invoiceNumber} أُعيدت للتصحيح: ${reason}`,
        type: 'INVOICE'
      }
    })
    await recordAudit(tx, { actorId, action: 'invoice.reject', entityType: 'Invoice', entityId: invoiceId, after: { reason }, req })
    return updated
  })
}

export async function voidInvoice({ invoiceId, actorId, req = null }) {
  return withSerializable(async (tx) => {
    const invoice = await tx.invoice.findUnique({ where: { id: invoiceId } })
    if (!invoice) throw notFound('Invoice not found')
    if (['APPROVED', 'PARTIALLY_PAID', 'PAID'].includes(invoice.status)) throw conflict('Approved invoices must be reversed through the ledger, not voided')
    if (invoice.status === 'VOIDED') throw conflict('Invoice is already voided')

    const updated = await tx.invoice.update({ where: { id: invoiceId }, data: { status: 'VOIDED', voidedAt: new Date() } })
    await recordAudit(tx, { actorId, action: 'invoice.void', entityType: 'Invoice', entityId: invoiceId, req })
    return updated
  })
}

export async function listInvoices(client, { where = {}, skip = 0, take = 20 }) {
  const [items, total] = await Promise.all([
    client.invoice.findMany({ where, include: { items: true, order: { select: { orderNumber: true } } }, orderBy: { createdAt: 'desc' }, skip, take }),
    client.invoice.count({ where })
  ])
  return { items, total }
}

export { prisma }
