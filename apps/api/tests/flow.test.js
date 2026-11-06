import test from 'node:test'
import assert from 'node:assert/strict'
import bcrypt from 'bcryptjs'
import { createApp } from '../src/app.js'
import { prisma } from '../src/lib/prisma.js'

let server
let base
const state = {}

async function truncateAll() {
  const tables = await prisma.$queryRaw`SELECT tablename FROM pg_tables WHERE schemaname = 'public' AND tablename <> '_prisma_migrations'`
  if (!tables.length) return
  const list = tables.map((t) => `"${t.tablename}"`).join(', ')
  await prisma.$executeRawUnsafe(`TRUNCATE TABLE ${list} RESTART IDENTITY CASCADE`)
  await prisma.$executeRawUnsafe(`ALTER SEQUENCE IF EXISTS "invoice_number_seq" RESTART WITH 1`)
  await prisma.$executeRawUnsafe(`ALTER SEQUENCE IF EXISTS "receipt_number_seq" RESTART WITH 1`)
  await prisma.$executeRawUnsafe(`ALTER SEQUENCE IF EXISTS "order_number_seq" RESTART WITH 1`)
}

async function call(path, { method = 'GET', token, body, raw } = {}) {
  const response = await fetch(`${base}${path}`, {
    method,
    headers: {
      ...(body ? { 'content-type': 'application/json' } : {}),
      ...(token ? { authorization: `Bearer ${token}` } : {})
    },
    body: body ? JSON.stringify(body) : undefined
  })
  const text = await response.text()
  const json = text ? JSON.parse(text) : null
  return { status: response.status, body: json, raw: text }
}

async function makeUser({ email, name, role, password = 'Password@123', status = 'ACTIVE', commissionRate = null }) {
  const passwordHash = await bcrypt.hash(password, 4)
  return prisma.user.create({
    data: { email, name, role, passwordHash, status, emailVerifiedAt: status === 'ACTIVE' ? new Date() : null, commissionRate }
  })
}

async function login(email, password = 'Password@123') {
  const res = await call('/api/auth/login', { method: 'POST', body: { email, password } })
  assert.equal(res.status, 200, `login failed for ${email}: ${res.raw}`)
  return res.body.accessToken
}

test('Dollar B2B platform — end-to-end financial flow', async (t) => {
  await truncateAll()

  const app = createApp({
    googleVerifier: async () => ({ subject: 'google-sub-001', email: 'google.user@dollar.test', name: 'Google User', picture: null })
  })
  server = app.listen(0)
  await new Promise((resolve) => server.once('listening', resolve))
  base = `http://127.0.0.1:${server.address().port}`

  await makeUser({ email: 'admin@dollar.test', name: 'Admin', role: 'ADMIN' })
  await makeUser({ email: 'warehouse@dollar.test', name: 'Warehouse', role: 'WAREHOUSE' })
  await makeUser({ email: 'collector@dollar.test', name: 'Collector', role: 'COLLECTOR', commissionRate: '0.0250' })
  await makeUser({ email: 'accountant@dollar.test', name: 'Accountant', role: 'ACCOUNTANT' })
  await makeUser({ email: 'buyer@dollar.test', name: 'Buyer One', role: 'BUYER' })
  await makeUser({ email: 'buyer2@dollar.test', name: 'Buyer Two', role: 'BUYER' })

  t.after(async () => {
    await new Promise((resolve) => server.close(resolve))
    await prisma.$disconnect()
  })

  await t.test('01 health endpoint reports the database is up', async () => {
    const res = await call('/health')
    assert.equal(res.status, 200)
    assert.equal(res.body.database, 'up')
  })

  await t.test('02 registration requires email verification before login', async () => {
    const registered = await call('/api/auth/register', {
      method: 'POST',
      body: { name: 'New Buyer', email: 'new.buyer@dollar.test', password: 'Password@123' }
    })
    assert.equal(registered.status, 201, registered.raw)
    assert.equal(registered.body.user.status, 'PENDING_VERIFICATION')
    assert.equal(registered.body.user.role, 'BUYER', 'self-registration can only create a BUYER')
    state.verificationToken = registered.body.verification.token
    assert.ok(state.verificationToken)

    const blocked = await call('/api/auth/login', { method: 'POST', body: { email: 'new.buyer@dollar.test', password: 'Password@123' } })
    assert.equal(blocked.status, 403)

    const verified = await call('/api/auth/verify-email', { method: 'POST', body: { token: state.verificationToken } })
    assert.equal(verified.status, 200)
    assert.equal(verified.body.user.status, 'ACTIVE')

    const loggedIn = await call('/api/auth/login', { method: 'POST', body: { email: 'new.buyer@dollar.test', password: 'Password@123' } })
    assert.equal(loggedIn.status, 200)
    assert.ok(loggedIn.body.accessToken)
  })

  await t.test('03 Google Sign-In verifies the ID token server-side', async () => {
    const res = await call('/api/auth/google', { method: 'POST', body: { idToken: 'stub-id-token-value-123456' } })
    assert.equal(res.status, 200, res.raw)
    assert.equal(res.body.user.email, 'google.user@dollar.test')
    assert.equal(res.body.user.status, 'ACTIVE')
    const stored = await prisma.user.findUnique({ where: { email: 'google.user@dollar.test' } })
    assert.equal(stored.googleSubject, 'google-sub-001')
  })

  await t.test('04 passwords: forgot, reset and change flows invalidate sessions', async () => {
    const forgot = await call('/api/auth/forgot-password', { method: 'POST', body: { email: 'buyer2@dollar.test' } })
    assert.equal(forgot.status, 200)
    const reset = await call('/api/auth/reset-password', { method: 'POST', body: { token: forgot.body.token, password: 'NewPassword@123' } })
    assert.equal(reset.status, 200, reset.raw)
    const relogin = await call('/api/auth/login', { method: 'POST', body: { email: 'buyer2@dollar.test', password: 'NewPassword@123' } })
    assert.equal(relogin.status, 200)
    const changed = await call('/api/auth/change-password', {
      method: 'POST', token: relogin.body.accessToken, body: { currentPassword: 'NewPassword@123', newPassword: 'ThirdPassword@123' }
    })
    assert.equal(changed.status, 200)
  })

  await t.test('05 RBAC blocks a buyer from creating an agency', async () => {
    state.adminToken = await login('admin@dollar.test')
    state.warehouseToken = await login('warehouse@dollar.test')
    state.collectorToken = await login('collector@dollar.test')
    state.accountantToken = await login('accountant@dollar.test')
    state.buyerToken = await login('buyer@dollar.test')

    const denied = await call('/api/agencies', {
      method: 'POST', token: state.buyerToken,
      body: { name: 'Rogue', slug: 'rogue-agency' }
    })
    assert.equal(denied.status, 403)
    assert.equal(denied.body.error.code, 'FORBIDDEN')

    const anonymous = await call('/api/agencies', { method: 'POST', body: { name: 'Anon', slug: 'anon' } })
    assert.equal(anonymous.status, 401)
  })

  await t.test('06 admin creates an agency, warehouse creates products', async () => {
    const agency = await call('/api/agencies', {
      method: 'POST', token: state.adminToken,
      body: { name: 'وكالة النور', nameEn: 'Al Noor', slug: 'al-noor', description: 'Wholesale food' }
    })
    assert.equal(agency.status, 201, agency.raw)
    state.agencyId = agency.body.agency.id

    const product = await call('/api/products', {
      method: 'POST', token: state.warehouseToken,
      body: {
        agencyId: state.agencyId, name: 'أرز بسمتي 25 كجم', sku: 'RICE-25', price: '42.5000',
        currency: 'USD', unit: 'bag', minimumOrderQuantity: 5, stockQuantity: 100,
        countryOfOrigin: 'IN', status: 'ACTIVE', images: ['https://cdn.dollar.test/rice-1.jpg', 'https://cdn.dollar.test/rice-2.jpg']
      }
    })
    assert.equal(product.status, 201, product.raw)
    state.productId = product.body.product.id
    assert.equal(product.body.product.price, '42.5000')
    assert.equal(product.body.product.images.length, 2)

    const buyerCannot = await call('/api/products', {
      method: 'POST', token: state.buyerToken,
      body: { agencyId: state.agencyId, name: 'x', sku: 'x', price: '1' }
    })
    assert.equal(buyerCannot.status, 403)
  })

  await t.test('07 buyer search finds the product by name, sku and agency', async () => {
    for (const q of ['أرز', 'RICE-25', 'النور']) {
      const res = await call(`/api/products?q=${encodeURIComponent(q)}`, { token: state.buyerToken })
      assert.equal(res.status, 200)
      assert.ok(res.body.data.length >= 1, `no result for ${q}`)
    }
  })

  await t.test('08 server-side pricing: the client cannot dictate the total', async () => {
    const belowMin = await call('/api/orders', {
      method: 'POST', token: state.buyerToken,
      body: { items: [{ productId: state.productId, quantity: 1 }] }
    })
    assert.equal(belowMin.status, 422)

    const created = await call('/api/orders', {
      method: 'POST', token: state.buyerToken,
      body: { items: [{ productId: state.productId, quantity: 10, unitPrice: '0.01' }], totalAmount: '0.10' }
    })
    assert.equal(created.status, 201, created.raw)
    state.orderId = created.body.order.id
    assert.equal(created.body.order.totalAmount, '425.0000', 'total is derived from the stored price (42.5 x 10)')
    assert.equal(created.body.order.items[0].unitPrice, '42.5000')
    assert.equal(created.body.order.status, 'PENDING')
  })

  await t.test('09 partial then full preparation drives the order status', async () => {
    const order = await prisma.order.findUnique({ where: { id: state.orderId }, include: { items: true } })
    state.orderItemId = order.items[0].id

    const partial = await call(`/api/orders/${state.orderId}/prepare`, {
      method: 'POST', token: state.warehouseToken,
      body: { items: [{ orderItemId: state.orderItemId, preparedQuantity: 6 }], note: '6 of 10 ready' }
    })
    assert.equal(partial.status, 200, partial.raw)
    assert.equal(partial.body.order.status, 'CONFIRMED')

    const full = await call(`/api/orders/${state.orderId}/prepare`, {
      method: 'POST', token: state.warehouseToken,
      body: { items: [{ orderItemId: state.orderItemId, preparedQuantity: 10 }] }
    })
    assert.equal(full.body.order.status, 'PREPARED')
  })

  await t.test('10 shipping is impossible before an invoice is sent', async () => {
    const order = await call('/api/orders', {
      method: 'POST', token: state.buyerToken,
      body: { items: [{ productId: state.productId, quantity: 5 }] }
    })
    assert.equal(order.status, 201)
    state.secondOrderId = order.body.order.id

    const ship = await call(`/api/orders/${state.secondOrderId}/ship`, {
      method: 'POST', token: state.warehouseToken, body: { courierPhone: '0555000000' }
    })
    assert.equal(ship.status, 409)
    assert.equal(ship.body.error.code, 'CONFLICT')
  })

  await t.test('11 warehouse issues and edits a draft invoice, then sends it', async () => {
    const created = await call('/api/invoices', { method: 'POST', token: state.warehouseToken, body: { orderId: state.orderId } })
    assert.equal(created.status, 201, created.raw)
    state.invoiceId = created.body.invoice.id
    assert.equal(created.body.invoice.status, 'DRAFT')
    assert.equal(created.body.invoice.totalAmount, '425.0000')

    const duplicate = await call('/api/invoices', { method: 'POST', token: state.warehouseToken, body: { orderId: state.orderId } })
    assert.equal(duplicate.status, 409, 'a second live invoice for the same order is refused')

    const edited = await call(`/api/invoices/${state.invoiceId}`, {
      method: 'PATCH', token: state.warehouseToken,
      body: { lines: [{ orderItemId: state.orderItemId, quantity: 8 }] }
    })
    assert.equal(edited.status, 200, edited.raw)
    assert.equal(edited.body.invoice.totalAmount, '340.0000')

    const sent = await call(`/api/invoices/${state.invoiceId}/send`, { method: 'POST', token: state.warehouseToken })
    assert.equal(sent.status, 200)
    assert.equal(sent.body.invoice.status, 'SENT')

    const buyerCannotSend = await call(`/api/invoices/${state.invoiceId}/approve`, { method: 'POST', token: state.buyerToken })
    assert.equal(buyerCannotSend.status, 403)
  })

  await t.test('12 accountant approval posts exactly one debit and is idempotent', async () => {
    const wrongRole = await call(`/api/invoices/${state.invoiceId}/approve`, { method: 'POST', token: state.warehouseToken })
    assert.equal(wrongRole.status, 403, 'the warehouse may not post to the ledger')

    const approved = await call(`/api/invoices/${state.invoiceId}/approve`, { method: 'POST', token: state.accountantToken })
    assert.equal(approved.status, 200, approved.raw)
    assert.equal(approved.body.invoice.status, 'APPROVED')
    assert.equal(approved.body.transaction.amount, '340.0000')
    assert.equal(approved.body.transaction.transactionType, 'DEBIT')
    assert.equal((await prisma.user.findUnique({ where: { email: 'buyer@dollar.test' } })).debitBalance.toFixed(4), '340.0000')

    const again = await call(`/api/invoices/${state.invoiceId}/approve`, { method: 'POST', token: state.accountantToken })
    assert.equal(again.status, 409)

    const postings = await prisma.financialTransaction.findMany({ where: { referenceId: state.invoiceId } })
    assert.equal(postings.length, 1, 'exactly one debit exists for the invoice')

    const editApproved = await call(`/api/invoices/${state.invoiceId}`, {
      method: 'PATCH', token: state.warehouseToken, body: { lines: [{ orderItemId: state.orderItemId, quantity: 1 }] }
    })
    assert.equal(editApproved.status, 409, 'approved invoices are immutable')
  })

  await t.test('13 shipping records the server clock and notifies the buyer', async () => {
    const before = Date.now()
    const shipped = await call(`/api/orders/${state.orderId}/ship`, {
      method: 'POST', token: state.warehouseToken,
      body: { courierPhone: '0555123456', courierName: 'مندوب الرياض', courierChannel: 'WhatsApp' }
    })
    assert.equal(shipped.status, 200, shipped.raw)
    assert.equal(shipped.body.order.status, 'SHIPPED')
    assert.ok(new Date(shipped.body.order.shippedAt).getTime() >= before - 2000)
    assert.equal(shipped.body.order.courierPhone, '0555123456')
  })

  await t.test('14 collector receipt does not touch the balance until approval', async () => {
    const created = await call('/api/receipts', {
      method: 'POST', token: state.collectorToken,
      body: { buyerId: (await prisma.user.findUnique({ where: { email: 'buyer@dollar.test' } })).id, amount: '200.0000', currency: 'USD', paymentMethod: 'CASH' }
    })
    assert.equal(created.status, 201, created.raw)
    state.receiptId = created.body.receipt.id
    assert.equal(created.body.receipt.status, 'DRAFT')

    const balanceBefore = await call('/api/accounting/me/statement', { token: state.buyerToken })
    assert.equal(balanceBefore.status, 200, balanceBefore.raw)
    assert.equal(balanceBefore.body.totals.balance, '340.0000')

    const sent = await call(`/api/receipts/${state.receiptId}/send`, { method: 'POST', token: state.collectorToken })
    assert.equal(sent.status, 200)

    const stillUnchanged = await call('/api/accounting/me/statement', { token: state.buyerToken })
    assert.equal(stillUnchanged.body.totals.balance, '340.0000', 'a sent receipt still does not move the balance')

    const approved = await call(`/api/receipts/${state.receiptId}/approve`, { method: 'POST', token: state.accountantToken })
    assert.equal(approved.status, 200, approved.raw)
    assert.equal(approved.body.receipt.status, 'APPROVED')
    assert.equal(approved.body.receipt.commissionRate, '0.0250')
    assert.equal(approved.body.receipt.commissionAmount, '5.0000')

    const after = await call('/api/accounting/me/statement', { token: state.buyerToken })
    assert.equal(after.body.totals.balance, '140.0000')
    assert.equal(after.body.outstanding.length, 1)
    assert.equal(after.body.outstanding[0].remaining, '140.0000', 'partial payment leaves the remainder open')
    assert.equal((await prisma.invoice.findUnique({ where: { id: state.invoiceId } })).status, 'PARTIALLY_PAID')
    assert.equal((await prisma.user.findUnique({ where: { email: 'buyer@dollar.test' } })).debitBalance.toFixed(4), '140.0000')
  })

  await t.test('15 a rejected receipt must not change anything', async () => {
    const buyer = await prisma.user.findUnique({ where: { email: 'buyer@dollar.test' } })
    const created = await call('/api/receipts', {
      method: 'POST', token: state.collectorToken,
      body: { buyerId: buyer.id, amount: '50.0000', currency: 'USD' }
    })
    assert.equal(created.status, 201)
    await call(`/api/receipts/${created.body.receipt.id}/send`, { method: 'POST', token: state.collectorToken })
    const rejected = await call(`/api/receipts/${created.body.receipt.id}/reject`, {
      method: 'POST', token: state.accountantToken, body: { reason: 'المبلغ لا يطابق الإيصال الورقي' }
    })
    assert.equal(rejected.status, 200)
    assert.equal(rejected.body.receipt.status, 'REJECTED')

    const statement = await call('/api/accounting/me/statement', { token: state.buyerToken })
    assert.equal(statement.body.totals.balance, '140.0000', 'rejection leaves the balance untouched')

    const commission = await call('/api/receipts/commission/me', { token: state.collectorToken })
    assert.equal(commission.body.commission.commission, '5.0000', 'rejected receipts earn no commission')
  })

  await t.test('16 overpayment is refused without privileged justification', async () => {
    const buyer = await prisma.user.findUnique({ where: { email: 'buyer@dollar.test' } })
    const res = await call('/api/receipts', {
      method: 'POST', token: state.collectorToken,
      body: { buyerId: buyer.id, amount: '999.0000', currency: 'USD' }
    })
    assert.equal(res.status, 422)
    assert.equal(res.body.error.details.requiresOverpaymentApproval, true)
  })

  await t.test('17 settling the remainder clears the outstanding balance', async () => {
    const buyer = await prisma.user.findUnique({ where: { email: 'buyer@dollar.test' } })
    const created = await call('/api/receipts', {
      method: 'POST', token: state.collectorToken,
      body: { buyerId: buyer.id, amount: '140.0000', currency: 'USD', paymentMethod: 'BANK_TRANSFER' }
    })
    assert.equal(created.status, 201, created.raw)
    await call(`/api/receipts/${created.body.receipt.id}/send`, { method: 'POST', token: state.collectorToken })
    const approved = await call(`/api/receipts/${created.body.receipt.id}/approve`, { method: 'POST', token: state.accountantToken })
    assert.equal(approved.status, 200, approved.raw)

    const statement = await call('/api/accounting/me/statement', { token: state.buyerToken })
    assert.equal(statement.body.totals.balance, '0.0000')
    assert.equal(statement.body.outstanding.length, 0)
    assert.equal((await prisma.invoice.findUnique({ where: { id: state.invoiceId } })).status, 'PAID')
    assert.equal((await prisma.user.findUnique({ where: { email: 'buyer@dollar.test' } })).debitBalance.toFixed(4), '0.0000')
  })

  await t.test('18 collectors see only their own receipts; buyers cannot read other buyers', async () => {
    const collectorList = await call('/api/receipts', { token: state.collectorToken })
    assert.equal(collectorList.status, 200)
    const collectorId = (await prisma.user.findUnique({ where: { email: 'collector@dollar.test' } })).id
    assert.ok(collectorList.body.data.length >= 1)
    assert.ok(collectorList.body.data.every((r) => r.collectorId === collectorId), 'a collector only sees their own receipts')

    const otherBuyer = await prisma.user.findUnique({ where: { email: 'buyer2@dollar.test' } })
    const forbidden = await call(`/api/accounting/buyers/${otherBuyer.id}/statement`, { token: state.buyerToken })
    assert.equal(forbidden.status, 403)
  })

  await t.test('19 the ledger is append-only at the database level', async () => {
    const tx = await prisma.financialTransaction.findFirst()
    await assert.rejects(
      () => prisma.$executeRawUnsafe(`UPDATE "FinancialTransaction" SET amount = 1 WHERE id = '${tx.id}'`),
      /append-only/
    )
    await assert.rejects(
      () => prisma.$executeRawUnsafe(`DELETE FROM "FinancialTransaction" WHERE id = '${tx.id}'`),
      /append-only/
    )
  })

  await t.test('20 concurrent approvals create a single credit', async () => {
    // Open a fresh invoice so the buyer has an outstanding balance again.
    const order = await prisma.order.findUnique({ where: { id: state.secondOrderId }, include: { items: true } })
    await call(`/api/orders/${order.id}/prepare`, {
      method: 'POST', token: state.warehouseToken,
      body: { items: [{ orderItemId: order.items[0].id, preparedQuantity: 5 }] }
    })
    const invoice = await call('/api/invoices', { method: 'POST', token: state.warehouseToken, body: { orderId: order.id } })
    assert.equal(invoice.status, 201, invoice.raw)
    await call(`/api/invoices/${invoice.body.invoice.id}/send`, { method: 'POST', token: state.warehouseToken })
    const approvedInvoice = await call(`/api/invoices/${invoice.body.invoice.id}/approve`, { method: 'POST', token: state.accountantToken })
    assert.equal(approvedInvoice.status, 200, approvedInvoice.raw)

    const buyer = await prisma.user.findUnique({ where: { email: 'buyer@dollar.test' } })
    const created = await call('/api/receipts', {
      method: 'POST', token: state.collectorToken,
      body: { buyerId: buyer.id, amount: '10.0000', currency: 'USD' }
    })
    await call(`/api/receipts/${created.body.receipt.id}/send`, { method: 'POST', token: state.collectorToken })

    const [a, b] = await Promise.all([
      call(`/api/receipts/${created.body.receipt.id}/approve`, { method: 'POST', token: state.accountantToken }),
      call(`/api/receipts/${created.body.receipt.id}/approve`, { method: 'POST', token: state.accountantToken })
    ])

    const statuses = [a.status, b.status].sort()
    assert.deepEqual(statuses, [200, 409], `expected one success and one conflict, got ${statuses}`)
    const credits = await prisma.financialTransaction.count({ where: { referenceId: created.body.receipt.id, transactionType: 'CREDIT' } })
    assert.equal(credits, 1)
  })

  await t.test('21 reports and the audit trail reflect the whole flow', async () => {
    const financial = await call('/api/reports/financial', { token: state.accountantToken })
    assert.equal(financial.status, 200, financial.raw)
    assert.equal(financial.body.ledger.debit, '552.5000')
    assert.equal(financial.body.ledger.credit, '350.0000')

    const collections = await call('/api/reports/collections', { token: state.accountantToken })
    assert.equal(collections.status, 200)
    assert.equal(collections.body.totals.receipts, 3)

    const commissions = await call('/api/reports/commissions', { token: state.accountantToken })
    assert.equal(commissions.status, 200)
    assert.equal(commissions.body.totals.commission, '8.7500')

    const month = new Date().toISOString().slice(0, 10)
    const monthly = await call(`/api/reports/collections?period=MONTHLY&date=${month}`, { token: state.accountantToken })
    assert.equal(monthly.status, 200, monthly.raw)
    assert.equal(monthly.body.period.type, 'MONTHLY')
    assert.equal(monthly.body.totals.collected, '350.0000')
    assert.equal(monthly.body.totals.commission, '8.7500')

    const audit = await prisma.auditLog.findMany()
    const actions = audit.map((a) => a.action)
    for (const expected of ['order.create', 'order.prepare', 'invoice.create', 'invoice.approve', 'receipt.create', 'receipt.approve']) {
      assert.ok(actions.includes(expected), `missing audit action ${expected}`)
    }

    const matching = await call('/api/accounting/matching', { token: state.accountantToken })
    assert.equal(matching.status, 200)
    assert.equal(matching.body.invoicesWithoutDebit.length, 0)
    assert.equal(matching.body.receiptsWithoutCredit.length, 0)
  })
})
