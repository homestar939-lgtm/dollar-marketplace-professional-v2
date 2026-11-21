import bcrypt from 'bcryptjs'
import { PrismaClient } from '@prisma/client'

const prisma = new PrismaClient()
const round = Number(process.env.BCRYPT_ROUNDS || 12)
const accounts = [
  { username: 'admin', name: 'مدير المنصة', role: 'ADMIN', passwordEnv: 'SEED_ADMIN_PASSWORD' },
  { username: 'warehouse', name: 'مسؤول المخزن', role: 'WAREHOUSE', passwordEnv: 'SEED_WAREHOUSE_PASSWORD' },
  { username: 'collector', name: 'محصل ميداني', role: 'COLLECTOR', passwordEnv: 'SEED_COLLECTOR_PASSWORD', commissionRate: '0.0250' },
  { username: 'accountant', name: 'المحاسب', role: 'ACCOUNTANT', passwordEnv: 'SEED_ACCOUNTANT_PASSWORD' },
  { username: 'buyer', name: 'مؤسسة النور التجارية', role: 'BUYER', passwordEnv: 'SEED_BUYER_PASSWORD' }
]

function readPasswords(minimum) {
  return accounts.map((account) => {
    const password = process.env[account.passwordEnv]
    if (!password || password.length < minimum) {
      throw new Error(`${account.passwordEnv} must be set to a unique password of at least ${minimum} characters`)
    }
    return { ...account, password }
  })
}

async function seedAccounts(passwords, production) {
  const created = {}
  for (const account of passwords) {
    const passwordHash = await bcrypt.hash(account.password, round)
    const { password: _secret, passwordEnv: _env, ...safe } = account
    const user = await prisma.user.upsert({
      where: { username: account.username },
      create: { ...safe, passwordHash, status: 'ACTIVE', locale: 'ar' },
      update: production ? {} : { name: account.name, role: account.role, passwordHash, status: 'ACTIVE', commissionRate: account.commissionRate ?? null }
    })
    created[account.role.toLowerCase()] = { id: user.id, username: user.username, role: user.role }
  }
  return created
}

async function main() {
  const production = process.env.NODE_ENV === 'production'
  const seededUsers = await seedAccounts(readPasswords(production ? 24 : 20), production)
  if (production) {
    console.log(JSON.stringify({ seeded: true, production: true, usernames: Object.values(seededUsers) }, null, 2))
    return
  }

  const admin = await prisma.user.findUnique({ where: { username: 'admin' } })
  const warehouse = await prisma.user.findUnique({ where: { username: 'warehouse' } })
  const collector = await prisma.user.findUnique({ where: { username: 'collector' } })
  await prisma.systemSettings.upsert({
    where: { id: 'default' },
    create: {
      id: 'default', companyName: 'دولار للتجارة', companyNameEn: 'Dollar Trading',
      defaultCurrency: 'USD', paymentTerms: 30,
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
  console.log(JSON.stringify({ seeded: true, demoMode: false, users: Object.values(seededUsers), agency: agency.name, products: products.length }, null, 2))
}

main().catch((error) => { console.error(error); process.exitCode = 1 }).finally(() => prisma.$disconnect())
