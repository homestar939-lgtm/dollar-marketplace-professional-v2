import { prisma } from '../../lib/prisma.js'
import { money, str } from '../../lib/money.js'
import { conflict, notFound, unprocessable } from '../../lib/errors.js'
import { recordAudit } from '../../utils/audit.js'
import { assertCurrency } from '../../utils/currency.js'

const BUYER_VISIBLE = (product) => {
  const imageUrls = (product.images || []).map((i) => i.url)
  return {
    id: product.id,
    name: product.name,
    nameEn: product.nameEn,
    sku: product.sku,
    agency: product.agency ? { id: product.agency.id, name: product.agency.name, slug: product.agency.slug } : undefined,
    price: str(product.price),
    currency: product.currency,
    unit: product.unit,
    minimumOrderQuantity: product.minimumOrderQuantity,
    availability: product.stockQuantity == null ? 'UNKNOWN' : product.stockQuantity > 0 ? 'IN_STOCK' : 'OUT_OF_STOCK',
    status: product.status,
    description: product.showDescription ? product.description : undefined,
    expiryDate: product.showExpiry ? product.expiryDate : undefined,
    countryOfOrigin: product.showCountry ? product.countryOfOrigin : undefined,
    specs: product.showSpecs ? product.specs : undefined,
    imageUrl: product.showImages ? product.imageUrl : undefined,
    images: product.showImages ? imageUrls : undefined
  }
}

const STAFF_VIEW = (product) => ({
  ...product,
  price: str(product.price),
  images: (product.images || []).map((i) => i.url)
})

export async function listProducts({ q, agencyId, status, minPrice, maxPrice, currency, page, limit, viewer }) {
  const isStaff = ['ADMIN', 'WAREHOUSE', 'ACCOUNTANT', 'COLLECTOR', 'SUPPORT'].includes(viewer.role)
  const where = {
    ...(agencyId ? { agencyId } : {}),
    // Buyers only ever see live products regardless of what they send.
    ...(isStaff && status ? { status } : { status: 'ACTIVE' }),
    ...(currency ? { currency } : {}),
    ...(minPrice != null || maxPrice != null
      ? { price: { ...(minPrice != null ? { gte: money(minPrice) } : {}), ...(maxPrice != null ? { lte: money(maxPrice) } : {}) } }
      : {}),
    ...(q
      ? {
          OR: [
            { name: { contains: q, mode: 'insensitive' } },
            { nameEn: { contains: q, mode: 'insensitive' } },
            { sku: { contains: q, mode: 'insensitive' } },
            { agency: { name: { contains: q, mode: 'insensitive' } } }
          ]
        }
      : {})
  }

  const [items, total] = await Promise.all([
    prisma.product.findMany({
      where,
      include: { images: { orderBy: { sortOrder: 'asc' } }, agency: { select: { id: true, name: true, slug: true } } },
      orderBy: { createdAt: 'desc' },
      skip: (page - 1) * limit,
      take: limit
    }),
    prisma.product.count({ where })
  ])

  return {
    data: items.map((p) => (isStaff ? STAFF_VIEW(p) : BUYER_VISIBLE(p))),
    meta: { page, limit, total, pages: Math.max(1, Math.ceil(total / limit)) }
  }
}

export async function getProduct({ productId, viewer }) {
  const product = await prisma.product.findUnique({
    where: { id: productId },
    include: { images: { orderBy: { sortOrder: 'asc' } }, agency: { select: { id: true, name: true, slug: true, status: true } } }
  })
  if (!product) throw notFound('Product not found')
  const isStaff = ['ADMIN', 'WAREHOUSE', 'ACCOUNTANT', 'COLLECTOR', 'SUPPORT'].includes(viewer.role)
  if (!isStaff && product.status !== 'ACTIVE') throw notFound('Product not found')
  return isStaff ? STAFF_VIEW(product) : BUYER_VISIBLE(product)
}

export async function createProduct({ data, actor, req }) {
  const agency = await prisma.agency.findUnique({ where: { id: data.agencyId } })
  if (!agency) throw unprocessable('Agency does not exist')
  assertCurrency(data.currency || 'USD')

  const duplicate = await prisma.product.findFirst({ where: { agencyId: data.agencyId, sku: data.sku } })
  if (duplicate) throw conflict(`SKU ${data.sku} already exists for this agency`)

  const { images = [], ...rest } = data
  const product = await prisma.product.create({
    data: {
      ...rest,
      price: money(data.price),
      specs: data.specs ?? undefined,
      expiryDate: data.expiryDate ? new Date(data.expiryDate) : null,
      createdById: actor.id,
      images: images.length ? { create: images.map((url, index) => ({ url, sortOrder: index })) } : undefined
    },
    include: { images: true }
  })
  await recordAudit(prisma, { actorId: actor.id, action: 'product.create', entityType: 'Product', entityId: product.id, after: { sku: product.sku, price: str(product.price) }, req })
  return STAFF_VIEW(product)
}

export async function updateProduct({ productId, data, actor, req }) {
  const product = await prisma.product.findUnique({ where: { id: productId } })
  if (!product) throw notFound('Product not found')
  if (data.currency) assertCurrency(data.currency)

  const { images, ...rest } = data
  const updated = await prisma.$transaction(async (tx) => {
    if (images) {
      await tx.productImage.deleteMany({ where: { productId } })
    }
    return tx.product.update({
      where: { id: productId },
      data: {
        ...rest,
        ...(data.price != null ? { price: money(data.price) } : {}),
        ...(data.expiryDate !== undefined ? { expiryDate: data.expiryDate ? new Date(data.expiryDate) : null } : {}),
        ...(images ? { images: { create: images.map((url, index) => ({ url, sortOrder: index })) } } : {})
      },
      include: { images: { orderBy: { sortOrder: 'asc' } }, agency: { select: { id: true, name: true, slug: true } } }
    })
  })

  await recordAudit(prisma, {
    actorId: actor.id, action: 'product.update', entityType: 'Product', entityId: productId,
    before: { price: str(product.price), status: product.status }, after: { price: str(updated.price), status: updated.status }, req
  })
  return STAFF_VIEW(updated)
}

export { BUYER_VISIBLE, STAFF_VIEW }
