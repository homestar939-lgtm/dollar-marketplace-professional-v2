import { money, add, mul, ZERO } from '../lib/money.js'

export const DEFAULT_COMMISSION_RATE = '0.0000'

/**
 * Commission is computed on APPROVED receipts only, using the rate snapshotted
 * on the receipt at approval time. Changing a collector's rate later must never
 * rewrite history.
 */
export async function resolveCommissionRate(client, collectorId) {
  const collector = await client.user.findUnique({ where: { id: collectorId }, select: { commissionRate: true, role: true } })
  if (!collector) return money(DEFAULT_COMMISSION_RATE)
  if (collector.commissionRate != null) return money(collector.commissionRate)

  const settings = await client.systemSettings.findUnique({ where: { id: 'default' } })
  const table = settings?.collectorCommissionRates
  if (table && typeof table === 'object' && table[collectorId] != null) return money(table[collectorId])
  return money(DEFAULT_COMMISSION_RATE)
}

export function commissionFor(amount, rate) {
  return mul(amount, rate)
}

export async function commissionSummary(client, { collectorId, from, to, status = 'APPROVED' }) {
  const receipts = await client.receipt.findMany({
    where: {
      collectorId,
      status,
      ...(from || to
        ? { approvedAt: { ...(from ? { gte: new Date(from) } : {}), ...(to ? { lte: new Date(to) } : {}) } }
        : {})
    },
    orderBy: { approvedAt: 'asc' }
  })

  const collected = receipts.reduce((acc, r) => add(acc, r.amount), ZERO)
  const commission = receipts.reduce((acc, r) => add(acc, r.commissionAmount), ZERO)
  return {
    collectorId,
    receipts: receipts.length,
    collected,
    commission,
    effectiveRate: collected.gt(0) && money(commission).gt(0) ? money(commission).div(collected).toDecimalPlaces(4) : ZERO,
    period: { from: from ?? null, to: to ?? null },
    lines: receipts.map((r) => ({
      receiptId: r.id,
      receiptNumber: r.receiptNumber,
      amount: r.amount,
      commissionRate: r.commissionRate,
      commissionAmount: r.commissionAmount,
      approvedAt: r.approvedAt
    }))
  }
}
