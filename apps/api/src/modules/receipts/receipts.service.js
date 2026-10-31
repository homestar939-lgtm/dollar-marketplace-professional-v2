import { prisma } from '../../lib/prisma.js'
import { forbidden, notFound } from '../../lib/errors.js'
import { str } from '../../lib/money.js'
import { commissionSummary } from '../../services/commission-service.js'

const STAFF = ['ADMIN', 'ACCOUNTANT', 'COLLECTOR', 'SUPPORT']

const present = (receipt) => ({
  ...receipt,
  amount: str(receipt.amount),
  commissionRate: receipt.commissionRate?.toFixed(4) ?? null,
  commissionAmount: str(receipt.commissionAmount)
})

export async function findReceipts({ viewer, status, buyerId, collectorId, q, page, limit }) {
  const where = {
    // Collectors see their own receipts; buyers see receipts issued to them.
    ...(viewer.role === 'COLLECTOR' ? { collectorId: viewer.id } : {}),
    ...(viewer.role === 'BUYER' ? { buyerId: viewer.id } : {}),
    ...(STAFF.includes(viewer.role) && collectorId ? { collectorId } : {}),
    ...(STAFF.includes(viewer.role) && buyerId ? { buyerId } : {}),
    ...(status ? { status } : {}),
    ...(q ? { receiptNumber: { contains: q, mode: 'insensitive' } } : {})
  }

  const [items, total] = await Promise.all([
    prisma.receipt.findMany({
      where,
      include: {
        buyer: { select: { id: true, name: true, email: true } },
        collector: { select: { id: true, name: true } },
        allocations: { include: { invoice: { select: { invoiceNumber: true } } } }
      },
      orderBy: { createdAt: 'desc' },
      skip: (page - 1) * limit,
      take: limit
    }),
    prisma.receipt.count({ where })
  ])
  return { data: items.map(present), meta: { page, limit, total, pages: Math.max(1, Math.ceil(total / limit)) } }
}

export async function findReceipt({ receiptId, viewer }) {
  const receipt = await prisma.receipt.findUnique({
    where: { id: receiptId },
    include: {
      buyer: { select: { id: true, name: true, email: true } },
      collector: { select: { id: true, name: true } },
      allocations: { include: { invoice: { select: { invoiceNumber: true, totalAmount: true } } } }
    }
  })
  if (!receipt) throw notFound('Receipt not found')
  const allowed = STAFF.includes(viewer.role) || receipt.buyerId === viewer.id || receipt.collectorId === viewer.id
  if (!allowed) throw forbidden('This receipt is not yours')
  return present(receipt)
}

export async function myCommission({ collectorId, from, to }) {
  const summary = await commissionSummary(prisma, { collectorId, from, to })
  return {
    ...summary,
    collected: str(summary.collected),
    commission: str(summary.commission),
    effectiveRate: summary.effectiveRate.toFixed(4),
    lines: summary.lines.map((l) => ({ ...l, amount: str(l.amount), commissionRate: l.commissionRate.toFixed(4), commissionAmount: str(l.commissionAmount) }))
  }
}
