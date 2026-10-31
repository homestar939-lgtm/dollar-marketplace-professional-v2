import { prisma } from '../../lib/prisma.js'
import { withSerializable } from '../../lib/tx.js'
import { money, add, mul, ZERO, str } from '../../lib/money.js'
import { conflict, forbidden, notFound, unprocessable } from '../../lib/errors.js'
import { recordAudit } from '../../utils/audit.js'
import { nextDocumentNumber } from '../../services/numbering.js'

const STAFF_ROLES = ['ADMIN', 'WAREHOUSE', 'ACCOUNTANT', 'SUPPORT']

/** Totals are always derived on the server from stored prices, never from the client. */
export async function createOrder({ buyerId, items, notes, req }) {
  return withSerializable(async (tx) => {
    const productIds = items.map((i) => i.productId)
    const products = await tx.product.findMany({ where: { id: { in: productIds } }, include: { agency: true } })
    if (products.length !== new Set(productIds).size) throw unprocessable('One or more products do not exist')

    const byId = new Map(products.map((p) => [p.id, p]))
    const lines = []
    let subtotal = ZERO
    let currency = null

    for (const item of items) {
      const product = byId.get(item.productId)
      if (!product) throw unprocessable(`Product ${item.productId} does not exist`)
      if (product.status !== 'ACTIVE') throw conflict(`Product ${product.name} is not available`)
      if (product.agency.status !== 'ACTIVE') throw conflict(`Agency ${product.agency.name} is not active`)
      if (item.quantity < product.minimumOrderQuantity) {
        throw unprocessable(`Minimum order quantity for ${product.name} is ${product.minimumOrderQuantity}`)
      }
      if (product.stockQuantity != null && item.quantity > product.stockQuantity) {
        throw conflict(`Only ${product.stockQuantity} units of ${product.name} are available`)
      }
      currency ??= product.currency
      if (product.currency !== currency) throw unprocessable('All order items must share the same currency')

      const lineTotal = mul(product.price, money(item.quantity))
      subtotal = add(subtotal, lineTotal)
      lines.push({
        productId: product.id,
        productName: product.name,
        productSku: product.sku,
        unit: product.unit,
        unitPrice: money(product.price),
        quantity: item.quantity,
        currency: product.currency,
        lineTotal
      })
    }

    const orderNumber = await nextDocumentNumber(tx, 'order_number_seq', 'ORD')
    const order = await tx.order.create({
      data: {
        orderNumber, buyerId, status: 'PENDING', currency: currency || 'USD',
        subtotal, totalAmount: subtotal, notes: notes ?? null, createdById: buyerId,
        items: { create: lines }
      },
      include: { items: true }
    })

    const recipients = await tx.user.findMany({ where: { role: { in: ['WAREHOUSE', 'ADMIN'] }, status: 'ACTIVE' }, select: { id: true } })
    if (recipients.length) {
      await tx.notification.createMany({
        data: recipients.map((r) => ({
          userId: r.id,
          title: 'طلب جديد',
          body: `طلب جديد ${orderNumber} بقيمة ${str(subtotal)} ${order.currency}`,
          type: 'ORDER',
          metadata: { orderId: order.id }
        }))
      })
    }

    await recordAudit(tx, { actorId: buyerId, action: 'order.create', entityType: 'Order', entityId: order.id, after: { orderNumber, total: str(subtotal) }, req })
    return order
  })
}

export async function listOrders({ viewer, status, buyerId, q, page, limit }) {
  const isStaff = STAFF_ROLES.includes(viewer.role)
  const where = {
    // A buyer can only ever read their own orders; this cannot be overridden by query params.
    ...(isStaff ? (buyerId ? { buyerId } : {}) : { buyerId: viewer.id }),
    ...(status ? { status } : {}),
    ...(q ? { orderNumber: { contains: q, mode: 'insensitive' } } : {})
  }

  const [items, total] = await Promise.all([
    prisma.order.findMany({
      where,
      include: {
        items: true,
        buyer: { select: { id: true, name: true, email: true } },
        invoices: { select: { id: true, invoiceNumber: true, status: true } }
      },
      orderBy: { createdAt: 'desc' },
      skip: (page - 1) * limit,
      take: limit
    }),
    prisma.order.count({ where })
  ])

  return { data: items, meta: { page, limit, total, pages: Math.max(1, Math.ceil(total / limit)) } }
}

export async function getOrder({ orderId, viewer }) {
  const order = await prisma.order.findUnique({
    where: { id: orderId },
    include: {
      items: true,
      buyer: { select: { id: true, name: true, email: true } },
      invoices: { include: { items: true } },
      preparedByOrder: true
    }
  })
  if (!order) throw notFound('Order not found')
  if (!STAFF_ROLES.includes(viewer.role) && order.buyerId !== viewer.id) throw forbidden('This order belongs to another buyer')
  return order
}

/** Partial preparation is a first-class state: quantities are tracked, not assumed. */
export async function prepareOrder({ orderId, items, note, actor, req }) {
  return withSerializable(async (tx) => {
    const order = await tx.order.findUnique({ where: { id: orderId }, include: { items: true } })
    if (!order) throw notFound('Order not found')
    if (['SHIPPED', 'DELIVERED', 'CANCELLED', 'RETURNED', 'REFUNDED'].includes(order.status)) {
      throw conflict(`Order is ${order.status} and can no longer be prepared`)
    }

    const index = new Map(order.items.map((i) => [i.id, i]))
    for (const line of items) {
      const item = index.get(line.orderItemId)
      if (!item) throw unprocessable(`Order item ${line.orderItemId} does not belong to this order`)
      if (line.preparedQuantity > item.quantity) {
        throw unprocessable(`Prepared quantity ${line.preparedQuantity} exceeds ordered ${item.quantity} for ${item.productName}`)
      }
      await tx.orderItem.update({ where: { id: item.id }, data: { preparedQuantity: line.preparedQuantity } })
    }

    const refreshed = await tx.orderItem.findMany({ where: { orderId } })
    const fullyPrepared = refreshed.every((i) => i.preparedQuantity >= i.quantity)
    const anyPrepared = refreshed.some((i) => i.preparedQuantity > 0)

    const updated = await tx.order.update({
      where: { id: orderId },
      data: {
        status: fullyPrepared ? 'PREPARED' : anyPrepared ? 'CONFIRMED' : 'PENDING',
        preparedAt: fullyPrepared ? new Date() : null
      },
      include: { items: true }
    })

    await tx.orderPreparation.upsert({
      where: { orderId },
      create: { orderId, preparedById: actor.id, note: note ?? null, completedAt: fullyPrepared ? new Date() : null },
      update: { preparedById: actor.id, note: note ?? null, completedAt: fullyPrepared ? new Date() : null }
    })

    const preparedUnits = refreshed.reduce((acc, i) => acc + i.preparedQuantity, 0)
    const orderedUnits = refreshed.reduce((acc, i) => acc + i.quantity, 0)
    await tx.notification.create({
      data: {
        userId: order.buyerId,
        title: fullyPrepared ? 'طلبك جاهز' : 'تجهيز جزئي لطلبك',
        body: `تم تجهيز ${preparedUnits} من ${orderedUnits} وحدة في الطلب ${order.orderNumber}`,
        type: 'ORDER',
        metadata: { orderId, preparedUnits, orderedUnits }
      }
    })

    await recordAudit(tx, {
      actorId: actor.id, action: 'order.prepare', entityType: 'Order', entityId: orderId,
      after: { status: updated.status, preparedUnits, orderedUnits }, req
    })
    return updated
  })
}

/** Shipping requires a sent (or approved) invoice; the clock is the server's. */
export async function shipOrder({ orderId, courierPhone, courierName, courierChannel, actor, req }) {
  return withSerializable(async (tx) => {
    const order = await tx.order.findUnique({ where: { id: orderId }, include: { invoices: true } })
    if (!order) throw notFound('Order not found')
    if (order.status === 'SHIPPED' || order.status === 'DELIVERED') throw conflict('Order has already shipped')

    const invoice = order.invoices.find((i) => ['SENT', 'APPROVED'].includes(i.status))
    if (!invoice) throw conflict('An invoice must be created and sent before shipping')

    const now = new Date()
    const updated = await tx.order.update({
      where: { id: orderId },
      data: { status: 'SHIPPED', shippedAt: now, courierPhone, courierName: courierName ?? null, courierChannel: courierChannel ?? null, trackedAt: now },
      include: { items: true, invoices: true }
    })

    await tx.notification.create({
      data: {
        userId: order.buyerId,
        title: 'تم إرسال طلبك',
        body: `الطلب ${order.orderNumber} في الطريق. رقم المندوب: ${courierPhone}`,
        type: 'SHIPMENT',
        metadata: { orderId, courierPhone, shippedAt: now.toISOString() }
      }
    })

    await recordAudit(tx, { actorId: actor.id, action: 'order.ship', entityType: 'Order', entityId: orderId, after: { courierPhone, shippedAt: now.toISOString() }, req })
    return updated
  })
}

export async function cancelOrder({ orderId, viewer, req }) {
  return withSerializable(async (tx) => {
    const order = await tx.order.findUnique({ where: { id: orderId }, include: { invoices: true } })
    if (!order) throw notFound('Order not found')
    if (viewer.role === 'BUYER' && order.buyerId !== viewer.id) throw forbidden('This order belongs to another buyer')
    if (order.status !== 'PENDING') throw conflict(`Only PENDING orders can be cancelled (current: ${order.status})`)
    if (order.invoices.some((i) => i.status === 'APPROVED')) throw conflict('Order has an approved invoice and cannot be cancelled')

    const updated = await tx.order.update({ where: { id: orderId }, data: { status: 'CANCELLED' } })
    await recordAudit(tx, { actorId: viewer.id, action: 'order.cancel', entityType: 'Order', entityId: orderId, req })
    return updated
  })
}
