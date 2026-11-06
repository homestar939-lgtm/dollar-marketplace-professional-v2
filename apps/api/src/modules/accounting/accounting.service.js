import { prisma } from '../../lib/prisma.js'
import { buyerBalance, statement, ledgerTotals, outstandingInvoices } from '../../services/financial-ledger.js'
import { str } from '../../lib/money.js'
import { notFound } from '../../lib/errors.js'

export async function listBuyersWithBalances({ q, onlyWithBalance, page, limit }) {
  const where = {
    role: 'BUYER',
    ...(q ? { OR: [{ name: { contains: q, mode: 'insensitive' } }, { email: { contains: q, mode: 'insensitive' } }] } : {})
  }
  const buyers = await prisma.user.findMany({
    where,
    orderBy: { name: 'asc' },
    skip: (page - 1) * limit,
    take: limit,
    select: { id: true, name: true, email: true, phone: true, status: true, createdAt: true }
  })

  const rows = await Promise.all(
    buyers.map(async (buyer) => {
      const balance = await buyerBalance(prisma, buyer.id)
      return {
        ...buyer,
        debit: str(balance.debit),
        credit: str(balance.credit),
        balance: str(balance.balance),
        outstanding: str(balance.outstanding),
        openInvoices: balance.openInvoices.length
      }
    })
  )

  const filtered = onlyWithBalance === 'true' ? rows.filter((r) => Number(r.outstanding) > 0) : rows
  const total = await prisma.user.count({ where })
  return { data: filtered, meta: { page, limit, total, pages: Math.max(1, Math.ceil(total / limit)) } }
}

export async function buyerStatement({ buyerId, from, to, currency }) {
  const buyer = await prisma.user.findUnique({ where: { id: buyerId }, select: { id: true, name: true, email: true } })
  if (!buyer) throw notFound('Buyer not found')

  const [totals, entries, invoices, receipts] = await Promise.all([
    ledgerTotals(prisma, { buyerId, currency }),
    statement(prisma, { buyerId, from, to, currency }),
    outstandingInvoices(prisma, { buyerId, currency }),
    prisma.receipt.findMany({ where: { buyerId, ...(currency ? { currency } : {}) }, orderBy: { createdAt: 'desc' }, take: 100 })
  ])

  return {
    buyer,
    totals: { debit: str(totals.debit), credit: str(totals.credit), balance: str(totals.balance) },
    outstanding: invoices.filter((i) => i.remaining.gt(0)).map((i) => ({
      invoiceId: i.invoiceId, invoiceNumber: i.invoiceNumber, currency: i.currency,
      total: str(i.total), allocated: str(i.allocated), remaining: str(i.remaining)
    })),
    statement: entries.entries.map((e) => ({
      id: e.id, type: e.transactionType, amount: str(e.amount), currency: e.currency,
      referenceType: e.referenceType, referenceId: e.referenceId, description: e.description,
      createdAt: e.createdAt, runningBalance: str(e.runningBalance)
    })),
    closingBalance: str(entries.closingBalance),
    receipts: receipts.map((r) => ({ id: r.id, receiptNumber: r.receiptNumber, amount: str(r.amount), status: r.status, createdAt: r.createdAt }))
  }
}

/** Reconciliation worklist: anything that does not tie out on both sides. */
export async function matchingQueue() {
  const [approvedInvoices, approvedReceipts, transactions, allocations] = await Promise.all([
    prisma.invoice.findMany({ where: { status: { in: ['APPROVED', 'PARTIALLY_PAID', 'PAID'] } }, select: { id: true, invoiceNumber: true, totalAmount: true, buyerId: true } }),
    prisma.receipt.findMany({ where: { status: 'APPROVED' }, select: { id: true, receiptNumber: true, amount: true, buyerId: true } }),
    prisma.financialTransaction.findMany({ select: { id: true, referenceType: true, referenceId: true, transactionType: true, amount: true } }),
    prisma.paymentAllocation.findMany({ select: { receiptId: true, invoiceId: true, amount: true } })
  ])

  const debitRefs = new Set(transactions.filter((t) => t.transactionType === 'DEBIT').map((t) => t.referenceId))
  const creditRefs = new Set(transactions.filter((t) => t.transactionType === 'CREDIT').map((t) => t.referenceId))
  const allocatedInvoiceIds = new Set(allocations.map((a) => a.invoiceId))

  const invoicesWithoutDebit = approvedInvoices.filter((i) => !debitRefs.has(i.id)).map((i) => ({ invoiceId: i.id, invoiceNumber: i.invoiceNumber, total: str(i.totalAmount) }))
  const receiptsWithoutCredit = approvedReceipts.filter((r) => !creditRefs.has(r.id)).map((r) => ({ receiptId: r.id, receiptNumber: r.receiptNumber, amount: str(r.amount) }))
  const unallocatedReceipts = approvedReceipts.filter((r) => !allocations.some((a) => a.receiptId === r.id)).map((r) => ({ receiptId: r.id, receiptNumber: r.receiptNumber, amount: str(r.amount) }))
  const partiallyAllocated = approvedInvoices
    .filter((i) => allocatedInvoiceIds.has(i.id))
    .map((i) => {
      const sum = allocations.filter((a) => a.invoiceId === i.id).reduce((acc, a) => acc + Number(a.amount), 0)
      return { invoiceId: i.id, invoiceNumber: i.invoiceNumber, total: str(i.totalAmount), allocated: sum.toFixed(4) }
    })
    .filter((row) => Number(row.allocated) < Number(row.total))

  return { invoicesWithoutDebit, receiptsWithoutCredit, unallocatedReceipts, partiallyAllocated }
}
