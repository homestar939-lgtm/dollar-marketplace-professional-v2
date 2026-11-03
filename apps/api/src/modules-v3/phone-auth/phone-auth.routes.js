import { Router } from 'express'
import * as controller from './phone-auth.controller.js'
import { validate } from '../../middleware/validation.js'
import { env } from '../../config/env.js'
import { notFound } from '../../lib/errors.js'

// مسارات عامة (بلا مصادقة) تُركَّب على /api/v3/auth/phone في app.js.
// `validate` يحوّل أخطاء zod إلى 422 UNPROCESSABLE عبر error-handler بدل 500.
export const phoneAuthRouter = Router()

// بوابة DEMO_MODE لكل مسار على حدة (لا `router.use`) لأن هذا الراوتر يتشارك
// البادئة /api/v3. عند تفعيل الوضع التجريبي تُعطَّل مسارات phone-auth وترجع 404
// من دون حذف أي كود؛ البوابة قبل `validate` حتى لا تُسرّب أخطاء 422.
const demoGate = (req, res, next) => {
  if (env.demoMode) return next(notFound('Phone authentication is disabled in demo mode'))
  next()
}

phoneAuthRouter.post('/request-otp', demoGate, validate(controller.requestOtpSchema), controller.requestOtp)
phoneAuthRouter.post('/verify-otp', demoGate, validate(controller.verifyOtpSchema), controller.verifyOtp)
