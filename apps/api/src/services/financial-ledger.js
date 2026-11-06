import { Prisma } from '@prisma/client'
import { prisma } from '../lib/prisma.js'
import { money, add, sub, ZERO } from '../lib/money.js'
import { conflict } from '../lib/errors.js'

/**
 * The single place where money enters the ledger.
 *
 * Invariants enforced here *and* in the database:
 *  - one DEBIT per invoice, one CREDIT per receipt (unique index),
 *  - amounts are strictly positive,
 *  - rows are append-only; corrections are REVERSAL entries.
 */
export async function postTransaction(tx, { buyerId, type, amount, currency, referenceType, referenceId, description = null, invoiceId = null, receiptId = null }) {
  const value = money(amount)
  if (value.lte(0)) throw conflict('Ledger amounts must be positive')

  try {
    return await tx.financialTransaction.create({
      data: { buyerId, transactionType: type, amount: value, currency, referenceType, referenceId, description, invoiceId, receiptId }
    })
  } catch (error) {
    // Idempotency: a concurrent or repeated approval hits the unique index and
    // must return the existing posting instead of double-charging the buyer.
    if (error instanceof Prisma.PrismaClientKnownRequestError && error.code === 'P2002') {
      const existing = await tx.financialTransaction.findFirst({ where: { referenceType, referenceId, transactionType: type } })
      if (existing) return existing
    }
    throw error
  }
}

export const postDebit = (tx, args) => postTransaction(tx, { ...args, type: 'DEBIT' })
export const postCredit = (tx, args) => postTransaction(tx, { ...args, type: 'CREDIT' })

/** Corrections are explicit compensating entries, never edits or deletes. */
export async function reverseTransaction(tx, { transactionId, reason, actorId = null }) {
  const original = await tx.financialTransaction.findUnique({ where: { id: transactionId } })
  if (!original) throw conflict('Transaction to reverse does not exist')
  const inverted = original.transactionType === 'DEBIT' ? 'CREDIT' : 'DEBIT'
  try {
    return await tx.financialTransaction.create({
      data: {
        buyerId: original.buyerId,
        transactionType: inverted,
        amount: original.amount,
        currency: original.currency,
        referenceType: 'REVERSAL',
        referenceId: original.id,
        description: reason || `Reversal of ${original.id}`,
        invoiceId: original.invoiceId,
        receiptId: original.receiptId,
        reversesTransactionId: original.id
      }
    })
  } catch (error) {
    if (error instanceof Prisma.PrismaClientKnownRequestError && error.code === 'P2002') {
      throw conflict('This transaction has already been reversed')
    }
    throw error
  }
}

export async function ledgerTotals(client, { buyerId, currency } = {}) {
  const where = { buyerId, ...(currency ? { currency } : {}) }
  const grouped = await client.financialTransaction.groupBy({
    by: ['transactionType', 'currency'],
    where,
    _sum: { amount: true }
  })

  const totals = { debit: ZERO, credit: ZERO, balance: ZERO, byCurrency: {} }
  for (const row of grouped) {
    const sum = money(row._sum.amount ?? 0)
    if (row.transactionType === 'DEBIT') totals.debit = add(totals.debit, sum)
    else totals.credit = add(totals.credit, sum)
    totals.byCurrency[row.currency] ??= { debit: ZERO, credit: ZERO }
    totals.byCurrency[row.currency][row.transactionType === 'DEBIT' ? 'debit' : 'credit'] =
      add(totals.byCurrency[row.currency][row.transactionType === 'DEBIT' ? 'debit' : 'credit'], sum)
  }
  totals.balance = sub(totals.debit, totals.credit)
  return totals
}

/** Outstanding = approved invoices minus every receipt amount allocated to them. */
export async function outstandingInvoices(client, { buyerId, currency } = {}) {
  const invoices = await client.invoice.findMany({
    where: { buyerId, status: { in: ['APPROVED', 'PARTIALLY_PAID'] }, ...(currency ? { currency } : {}) },
    include: { allocations: true },
    orderBy: { approvedAt: 'asc' }
  })

  return invoices.map((invoice) => {
    const allocated = invoice.allocations.reduce((acc, a) => add(acc, a.amount), ZERO)
    return {
      invoiceId: invoice.id,
      invoiceNumber: invoice.invoiceNumber,
      currency: invoice.currency,
      total: money(invoice.totalAmount),
      allocated,
      remaining: sub(invoice.totalAmount, allocated),
      approvedAt: invoice.approvedAt
    }
  })
}

export async function buyerBalance(client, buyerId) {
  const totals = await ledgerTotals(client, { buyerId })
  const outstanding = await outstandingInvoices(client, { buyerId })
  return {
    debit: totals.debit,
    credit: totals.credit,
    balance: totals.balance,
    outstanding: outstanding.reduce((acc, i) => add(acc, i.remaining), ZERO),
    openInvoices: outstanding.filter((i) => i.remaining.gt(0))
  }
}

export async function statement(client, { buyerId, from, to, currency }) {
  const where = {
    buyerId,
    ...(currency ? { currency } : {}),
    ...(from || to
      ? { createdAt: { ...(from ? { gte: new Date(from) } : {}), ...(to ? { lte: new Date(to) } : {}) } }
      : {})
  }
  const rows = await client.financialTransaction.findMany({ where, orderBy: { createdAt: 'asc' } })
  let running = ZERO
  const entries = rows.map((row) => {
    running = row.transactionType === 'DEBIT' ? add(running, row.amount) : sub(running, row.amount)
    return { ...row, runningBalance: running }
  })
  return { entries, closingBalance: running }
}

export { prisma }
