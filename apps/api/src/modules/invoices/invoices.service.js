import { prisma } from '../../lib/prisma.js'
import { listInvoices } from '../../services/invoice-service.js'
import { forbidden, notFound } from '../../lib/errors.js'
import { str } from '../../lib/money.js'

const STAFF = ['ADMIN', 'WAREHOUSE', 'ACCOUNTANT', 'COLLECTOR', 'SUPPORT']

const present = (invoice) => ({
  ...invoice,
  subtotal: str(invoice.subtotal),
  totalAmount: str(invoice.totalAmount),
  items: invoice.items?.map((i) => ({ ...i, unitPrice: str(i.unitPrice), lineTotal: str(i.lineTotal) }))
})

export async function findInvoices({ viewer, status, buyerId, q, page, limit }) {
  const isStaff = STAFF.includes(viewer.role)
  const where = {
    ...(isStaff ? (buyerId ? { buyerId } : {}) : { buyerId: viewer.id }),
    ...(status ? { status } : {}),
    ...(q ? { invoiceNumber: { contains: q, mode: 'insensitive' } } : {})
  }
  const { items, total } = await listInvoices(prisma, { where, skip: (page - 1) * limit, take: limit })
  return { data: items.map(present), meta: { page, limit, total, pages: Math.max(1, Math.ceil(total / limit)) } }
}

export async function findInvoice({ invoiceId, viewer }) {
  const invoice = await prisma.invoice.findUnique({
    where: { id: invoiceId },
    include: { items: true, order: { select: { orderNumber: true } }, allocations: { include: { receipt: { select: { receiptNumber: true, status: true } } } } }
  })
  if (!invoice) throw notFound('Invoice not found')
  if (!STAFF.includes(viewer.role) && invoice.buyerId !== viewer.id) throw forbidden('This invoice belongs to another buyer')
  return present(invoice)
}
