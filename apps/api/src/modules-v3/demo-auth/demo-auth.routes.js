import { Router } from 'express'
import * as controller from './demo-auth.controller.js'
import { env } from '../../config/env.js'
import { notFound } from '../../lib/errors.js'

// يُركّب في app.js على /api/v3 فيصبح المسار /api/v3/demo/login.
export const demoAuthRouter = Router()

// بوابة DEMO_MODE لكل مسار (لا `router.use`) لأن هذا الراوتر يتشارك البادئة
// /api/v3 مع راوترات أخرى؛ `use` كان سيجعل أي مسار مجهول تحت /api/v3 يُعالج
// هنا بدل notFoundHandler. عند التعطيل يمر الخطأ عبر error-handler فيرجع 404.
const gate = (req, res, next) => {
  if (!env.demoMode) return next(notFound('Demo mode is disabled'))
  next()
}

demoAuthRouter.post('/demo/login', gate, controller.login)
