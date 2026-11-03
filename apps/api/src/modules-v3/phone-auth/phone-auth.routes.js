import { Router } from 'express'
import * as controller from './phone-auth.controller.js'
import { validate } from '../../middleware/validation.js'

// مسارات عامة (بلا مصادقة) تُركَّب على /api/v3/auth/phone في app.js.
// `validate` يحوّل أخطاء zod إلى 422 UNPROCESSABLE عبر error-handler بدل 500.
export const phoneAuthRouter = Router()

phoneAuthRouter.post('/request-otp', validate(controller.requestOtpSchema), controller.requestOtp)
phoneAuthRouter.post('/verify-otp', validate(controller.verifyOtpSchema), controller.verifyOtp)
