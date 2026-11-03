import { z } from 'zod'
import * as service from './phone-auth.service.js'

// نفس قواعد التحقق: هاتف سعودي يبدأ بـ 05 وطوله ١٠ أرقام، ورمز من ٦ أرقام.
const phoneSchema = z.string().trim().regex(/^05\d{8}$/, 'رقم الهاتف يجب أن يطابق الصيغة 05XXXXXXXX')
const codeSchema = z.string().trim().regex(/^\d{6}$/, 'رمز التحقق يجب أن يكون ٦ أرقام')

const requestOtpSchema = z.object({ phone: phoneSchema })
const verifyOtpSchema = z.object({ phone: phoneSchema, code: codeSchema })

// مُصدَّران ليُركَّبا كـ middleware عبر `validate` (تتحول أخطاء zod إلى 422 بدل 500).
export { requestOtpSchema, verifyOtpSchema }

export const requestOtp = async (req, res, next) => {
  try {
    const { phone } = requestOtpSchema.parse(req.body)
    res.json(await service.requestOtp({ phone, req }))
  } catch (error) {
    next(error)
  }
}

export const verifyOtp = async (req, res, next) => {
  try {
    const { phone, code } = verifyOtpSchema.parse(req.body)
    res.json(await service.verifyOtp({ phone, code, req }))
  } catch (error) {
    next(error)
  }
}
