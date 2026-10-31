import { prisma } from '../../lib/prisma.js'
import { money, add, sub, ZERO, str } from '../../lib/money.js'
import { commissionSummary } from '../../services/commission-service.js'
import { ledgerTotals, outstandingInvoices } from '../../services/financial-ledger.js'

const range = (from, to) => (from || to ? { ...(from ? { gte: new Date(from) } : {}), ...(to ? { lte: new Date(to) } : {}) } : undefined)

export async function collectionsReport({ from, to, collectorId, currency }) {
  const where = {
    status: 'APPROVED',
    ...(collectorId ? { collectorId } : {}),
    ...(currency ? { currency } : {}),
    ...(range(from, to) ? { approvedAt: range(from, to) } : {})
  }
  const receipts = await prisma.receipt.findMany({
    where,
    include: { collector: { select: { id: true, name: true } }, buyer: { select: { id: true, name: true } } },
    orderBy: { approvedAt: 'asc' }
  })

  const byCollector = new Map()
  const byCurrency = new Map()
  for (const receipt of receipts) {
    const key = receipt.collectorId
    const entry = byCollector.get(key) || { collectorId: key, collectorName: receipt.collector.name, receipts: 0, collected: ZERO, commission: ZERO }
    entry.receipts += 1
    entry.collected = add(entry.collected, receipt.amount)
    entry.commission = add(entry.commission, receipt.commissionAmount)
    byCollector.set(key, entry)

    const cur = byCurrency.get(receipt.currency) || { currency: receipt.currency, collected: ZERO, count: 0 }
    cur.collected = add(cur.collected, receipt.amount)
    cur.count += 1
    byCurrency.set(receipt.currency, cur)
  }

  return {
    period: { from: from ?? null, to: to ?? null },
    totals: {
      receipts: receipts.length,
      collected: str(receipts.reduce((acc, r) => add(acc, r.amount), ZERO)),
      commission: str(receipts.reduce((acc, r) => add(acc, r.commissionAmount), ZERO))
    },
    byCollector: [...byCollector.values()].map((e) => ({ ...e, collected: str(e.collected), commission: str(e.commission) })),
    byCurrency: [...byCurrency.values()].map((e) => ({ ...e, collected: str(e.collected) })),
    receipts: receipts.map((r) => ({
      id: r.id, receiptNumber: r.receiptNumber, buyer: r.buyer.name, collector: r.collector.name,
      amount: str(r.amount), currency: r.currency, commissionAmount: str(r.commissionAmount), approvedAt: r.approvedAt
    }))
  }
}

export async function commissionsReport({ from, to, collectorId }) {
  const collectors = collectorId
    ? await prisma.user.findMany({ where: { id: collectorId } })
    : await prisma.user.findMany({ where: { role: 'COLLECTOR' } })

  const rows = await Promise.all(collectors.map(async (collector) => {
    const summary = await commissionSummary(prisma, { collectorId: collector.id, from, to })
    return {
      collectorId: collector.id,
      collectorName: collector.name,
      currentRate: collector.commissionRate ? collector.commissionRate.toFixed(4) : '0.0000',
      receipts: summary.receipts,
      collected: str(summary.collected),
      commission: str(summary.commission)
    }
  }))

  return {
    period: { from: from ?? null, to: to ?? null },
    rows,
    totals: {
      collected: str(rows.reduce((acc, r) => add(acc, r.collected), ZERO)),
      commission: str(rows.reduce((acc, r) => add(acc, r.commission), ZERO))
    }
  }
}

export async function financialOverview({ currency }) {
  const [invoiceGroups, receiptGroups, transactionGroups, buyers] = await Promise.all([
    prisma.invoice.groupBy({ by: ['status'], _count: true, _sum: { totalAmount: true } }),
    prisma.receipt.groupBy({ by: ['status'], _count: true, _sum: { amount: true } }),
    prisma.financialTransaction.groupBy({ by: ['transactionType', 'currency'], _sum: { amount: true } }),
    prisma.user.findMany({ where: { role: 'BUYER' }, select: { id: true } })
  ])

  let debit = ZERO
  let credit = ZERO
  for (const row of transactionGroups) {
    if (currency && row.currency !== currency) continue
    if (row.transactionType === 'DEBIT') debit = add(debit, row._sum.amount ?? 0)
    else credit = add(credit, row._sum.amount ?? 0)
  }

  const outstandingRows = await Promise.all(buyers.map((b) => outstandingInvoices(prisma, { buyerId: b.id, currency })))
  const outstandingTotal = outstandingRows.flat().reduce((acc, i) => add(acc, i.remaining), ZERO)

  return {
    invoices: Object.fromEntries(invoiceGroups.map((g) => [g.status, { count: g._count, total: str(g._sum.totalAmount ?? 0) }])),
    receipts: Object.fromEntries(receiptGroups.map((g) => [g.status, { count: g._count, total: str(g._sum.amount ?? 0) }])),
    ledger: { debit: str(debit), credit: str(credit), balance: str(sub(debit, credit)) },
    outstanding: str(outstandingTotal),
    buyers: buyers.length
  }
}

export async function outstandingReport({ currency }) {
  const buyers = await prisma.user.findMany({ where: { role: 'BUYER' }, select: { id: true, name: true, email: true } })
  const rows = []
  for (const buyer of buyers) {
    const invoices = await outstandingInvoices(prisma, { buyerId: buyer.id, currency })
    const total = invoices.reduce((acc, i) => add(acc, i.remaining), ZERO)
    if (money(total).gt(0)) {
      rows.push({
        buyerId: buyer.id, buyerName: buyer.name, email: buyer.email,
        openInvoices: invoices.filter((i) => i.remaining.gt(0)).length,
        outstanding: str(total)
      })
    }
  }
  rows.sort((a, b) => Number(b.outstanding) - Number(a.outstanding))
  return { rows, total: str(rows.reduce((acc, r) => add(acc, r.outstanding), ZERO)) }
}
