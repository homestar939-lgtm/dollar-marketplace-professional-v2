import express from 'express'
import cors from 'cors'
import { env } from './config/env.js'
import { prisma } from './lib/prisma.js'
import { requestContext } from './middleware/request-context.js'
import { errorHandler, notFoundHandler } from './middleware/error-handler.js'
import { authRouter } from './modules/auth/auth.routes.js'
import { usersRouter } from './modules/users/users.routes.js'
import { agenciesRouter } from './modules/agencies/agencies.routes.js'
import { productsRouter } from './modules/products/products.routes.js'
import { ordersRouter } from './modules/orders/orders.routes.js'
import { invoicesRouter } from './modules/invoices/invoices.routes.js'
import { receiptsRouter } from './modules/receipts/receipts.routes.js'
import { accountingRouter } from './modules/accounting/accounting.routes.js'
import { reportsRouter } from './modules/reports/reports.routes.js'
import { settingsRouter } from './modules/settings/settings.routes.js'
import { notificationsRouter } from './modules/notifications/notifications.routes.js'
// ✅ V3: وحدات جديدة (إضافة فقط — لا تعديل على الوحدات القائمة)
import { phoneAuthRouter } from './modules-v3/phone-auth/phone-auth.routes.js'
import { deliveryRouter } from './modules-v3/delivery/delivery.routes.js'
import { auditV3Router } from './modules-v3/audit-v3/audit-v3.routes.js'
import { demoAuthRouter } from './modules-v3/demo-auth/demo-auth.routes.js'

export function createApp({ googleVerifier } = {}) {
  const app = express()
  app.set('trust proxy', true)
  app.locals.googleVerifier = googleVerifier

  app.use(cors({ origin: env.corsOrigins === '*' ? true : env.corsOrigins, credentials: true }))
  app.use(express.json({ limit: '1mb' }))
  app.use(requestContext)

  app.get('/health', async (req, res) => {
    try {
      await prisma.$queryRaw`SELECT 1`
      res.json({ status: 'ok', database: 'up', uptime: process.uptime(), version: '2.0.0' })
    } catch (error) {
      res.status(503).json({ status: 'degraded', database: 'down', message: error.message })
    }
  })

  app.use('/api/auth', authRouter)
  app.use('/api/users', usersRouter)
  app.use('/api/agencies', agenciesRouter)
  app.use('/api/products', productsRouter)
  app.use('/api/orders', ordersRouter)
  app.use('/api/invoices', invoicesRouter)
  app.use('/api/receipts', receiptsRouter)
  app.use('/api/accounting', accountingRouter)
  app.use('/api/reports', reportsRouter)
  app.use('/api/settings', settingsRouter)
  app.use('/api/notifications', notificationsRouter)

  // ✅ V3: مسارات جديدة تُضاف بعد المسارات القائمة
  app.use('/api/v3/auth/phone', phoneAuthRouter)
  app.use('/api/v3', deliveryRouter)
  app.use('/api/v3', auditV3Router)
  app.use('/api/v3', demoAuthRouter)

  app.use(notFoundHandler)
  app.use(errorHandler)
  return app
}
