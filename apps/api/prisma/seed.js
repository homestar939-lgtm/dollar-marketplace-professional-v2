import bcrypt from 'bcryptjs'
import { PrismaClient } from '@prisma/client'

const prisma = new PrismaClient()
const round = Number(process.env.BCRYPT_ROUNDS || 10)

async function upsertUser({ email, name, role, password, status = 'ACTIVE', commissionRate = null }) {
  const passwordHash = await bcrypt.hash(password, round)
  return prisma.user.upsert({
    where: { email },
    create: { email, name, role, status, passwordHash, emailVerifiedAt: status === 'ACTIVE' ? new Date() : null, commissionRate, locale: 'ar' },
    update: { role, status, passwordHash, commissionRate }
  })
}

async function bootstrapProductionAdmin() {
  const name = process.env.SEED_ADMIN_NAME?.trim()
  const email = process.env.SEED_ADMIN_EMAIL?.trim().toLowerCase()
  const password = process.env.SEED_ADMIN_PASSWORD

  if (!name || !email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) || email.endsWith('.local')) {
    throw new Error('Production bootstrap requires SEED_ADMIN_NAME and a real SEED_ADMIN_EMAIL')
  }
  if (!password || password.length < 24 || password === 'Admin@12345') {
    throw new Error('Production bootstrap requires a unique SEED_ADMIN_PASSWORD of at least 24 characters')
  }

  const passwordHash = await bcrypt.hash(password, round)
  const admin = await prisma.$transaction(async (tx) => {
    const existing = await tx.user.findFirst({ where: { role: 'ADMIN' }, select: { id: true } })
    if (existing) throw new Error('Production admin already exists; bootstrap is one-time only')

    return tx.user.create({
      data: {
        name,
        email,
        passwordHash,
        role: 'ADMIN',
        status: 'ACTIVE',
        emailVerifiedAt: new Date(),
        locale: 'ar'
      },
      select: { id: true, email: true, role: true }
    })
  }, { isolationLevel: 'Serializable' })

  console.log(JSON.stringify({ seeded: true, production: true, demoData: false, admin }, null, 2))
}

async function main() {
  if (process.env.NODE_ENV === 'production') {
    await bootstrapProductionAdmin()
    return
  }

  const admin = await upsertUser({ email: 'admin@dollar.local', name: 'مدير المنصة', role: 'ADMIN', password: 'Admin@12345' })
  const warehouse = await upsertUser({ email: 'warehouse@dollar.local', name: 'مسؤول المخزن', role: 'WAREHOUSE', password: 'Warehouse@12345' })
  const collector = await upsertUser({ email: 'collector@dollar.local', name: 'محصل ميداني', role: 'COLLECTOR', password: 'Collector@12345', commissionRate: '0.0250' })
  const accountant = await upsertUser({ email: 'accountant@dollar.local', name: 'المحاسب', role: 'ACCOUNTANT', password: 'Accountant@12345' })
  const buyer = await upsertUser({ email: 'buyer@dollar.local', name: 'مؤسسة النور التجارية', role: 'BUYER', password: 'Buyer@12345' })

  await prisma.systemSettings.upsert({
    where: { id: 'default' },
    create: {
      id: 'default',
      companyName: 'دولار للتجارة',
      companyNameEn: 'Dollar Trading',
      defaultCurrency: 'USD',
      paymentTerms: 30,
      collectorCommissionRates: { [collector.id]: '0.0250' },
      productDisplaySettings: { showExpiry: true, showCountry: true, showDescription: true, showImages: true, showSpecs: true }
    },
    update: {}
  })

  const agency = await prisma.agency.upsert({
    where: { slug: 'al-noor-agency' },
    create: { name: 'وكالة النور', nameEn: 'Al Noor Agency', slug: 'al-noor-agency', description: 'وكالة مواد غذائية بالجملة', createdById: admin.id, status: 'ACTIVE' },
    update: {}
  })

  const products = [
    { sku: 'RICE-25KG', name: 'أرز بسمتي 25 كجم', price: '42.5000', unit: 'bag', minimumOrderQuantity: 5, stockQuantity: 400, countryOfOrigin: 'IN' },
    { sku: 'OIL-18L', name: 'زيت نباتي 18 لتر', price: '31.7500', unit: 'tin', minimumOrderQuantity: 10, stockQuantity: 250, countryOfOrigin: 'TR' },
    { sku: 'SUGAR-50KG', name: 'سكر أبيض 50 كجم', price: '58.0000', unit: 'bag', minimumOrderQuantity: 3, stockQuantity: 120, countryOfOrigin: 'BR' }
  ]

  for (const product of products) {
    await prisma.product.upsert({
      where: { agencyId_sku: { agencyId: agency.id, sku: product.sku } },
      create: { ...product, agencyId: agency.id, status: 'ACTIVE', createdById: warehouse.id, showExpiry: true, showCountry: true },
      update: { price: product.price, status: 'ACTIVE' }
    })
  }

  console.log(JSON.stringify({
    seeded: true,
    users: { admin: admin.email, warehouse: warehouse.email, collector: collector.email, accountant: accountant.email, buyer: buyer.email },
    agency: agency.name,
    products: products.length
  }, null, 2))
}

main()
  .catch((error) => { console.error(error); process.exitCode = 1 })
  .finally(() => prisma.$disconnect())
