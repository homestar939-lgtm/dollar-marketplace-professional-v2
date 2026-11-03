import crypto from 'node:crypto'
import { prisma } from '../../lib/prisma.js'
import { signAccessToken } from '../../lib/jwt.js'
import { randomToken, sha256 } from '../../lib/tokens.js'
import { recordAudit } from '../../utils/audit.js'
import { AppError } from '../../lib/errors.js'

// ثوابت سياسة رموز الدخول بالهاتف.
const OTP_PURPOSE = 'LOGIN'
const OTP_TTL_MINUTES = 5
const OTP_MAX_ATTEMPTS = 3
const OTP_RATE_LIMIT = 3
const OTP_RATE_WINDOW_MS = 60 * 1000
const SESSION_TTL_DAYS = 30

/**
 * نافذة معدّل الطلبات لكل رقم هاتف.
 * مخزّنة في الذاكرة لأن جدول OtpCode يُنظَّف من الرموز القديمة عند كل طلب،
 * فلا يمكن الاعتماد عليه لإحصاء الطلبات داخل النافذة الزمنية.
 */
const rateBuckets = new Map()

const enforceRateLimit = (phone) => {
  const now = Date.now()
  const hits = (rateBuckets.get(phone) ?? []).filter((at) => now - at < OTP_RATE_WINDOW_MS)
  if (hits.length >= OTP_RATE_LIMIT) {
    throw new AppError(429, 'OTP_RATE_LIMITED', 'تم تجاوز الحد المسموح من طلبات الرمز، حاول بعد دقيقة')
  }
  hits.push(now)
  rateBuckets.set(phone, hits)
}

/** توليد رمز تحقق من ٦ أرقام. */
export function generateOtpCode() {
  return String(crypto.randomInt(100000, 999999))
}

/**
 * إرسال رمز التحقق عبر رسالة نصية.
 * خارج الإنتاج يُطبع الرمز في السجل لتسهيل التطوير والاختبار.
 */
export async function sendSms(phone, code) {
  if (process.env.NODE_ENV === 'production') {
    // TODO: اربط مزوّد SMS الفعلي (Twilio / Unifonic / Msegat) قبل الإطلاق.
    return { sent: false, provider: null }
  }
  console.log(`📱 SMS to ${phone}: Your OTP code is ${code}`)
  return { sent: true, provider: 'console' }
}

/**
 * جلب مستخدم الهاتف أو إنشاؤه.
 * ملاحظة إلزامية: عمود User.email إلزامي وفريد في هذا السكيما، ولأن الدخول هنا
 * بالهاتف فقط بلا بريد حقيقي، نشتق بريداً بديلاً ثابتاً من الرقم لتفادي تعارض الفرادة.
 */
const findOrCreatePhoneUser = async (phone) => {
  const existing = await prisma.user.findFirst({ where: { phone } })
  if (existing) return existing

  try {
    return await prisma.user.create({
      data: {
        name: `مستخدم ${phone.slice(-4)}`,
        email: `${phone}@phone.local`,
        phone,
        role: 'BUYER',
        status: 'PENDING_VERIFICATION'
      }
    })
  } catch (error) {
    // سباق إنشاء متزامن: مستخدم آخر أنشأ نفس الرقم للتو.
    if (error?.code === 'P2002') return prisma.user.findFirst({ where: { phone } })
    throw error
  }
}

/** طلب رمز تحقق جديد للدخول بالهاتف. */
export async function requestOtp({ phone, req }) {
  enforceRateLimit(phone)
  const user = await findOrCreatePhoneUser(phone)

  // إبطال كل رموز LOGIN السابقة حتى يبقى الرمز الأخير وحده صالحاً.
  await prisma.otpCode.deleteMany({ where: { userId: user.id, purpose: OTP_PURPOSE } })

  const code = generateOtpCode()
  await prisma.otpCode.create({
    data: {
      userId: user.id,
      code,
      purpose: OTP_PURPOSE,
      expiresAt: new Date(Date.now() + OTP_TTL_MINUTES * 60 * 1000)
    }
  })

  const delivery = await sendSms(phone, code)
  // لا يُعاد الرمز في الاستجابة أبداً؛ يُسلَّم عبر SMS فقط.
  return {
    sent: delivery.sent,
    phone,
    expiresInMinutes: OTP_TTL_MINUTES,
    maxAttempts: OTP_MAX_ATTEMPTS
  }
}

/** التحقق من رمز OTP وإنشاء جلسة عند النجاح. */
export async function verifyOtp({ phone, code, req }) {
  const user = await prisma.user.findFirst({ where: { phone } })
  if (!user) throw new AppError(404, 'USER_NOT_FOUND', 'لا يوجد حساب مرتبط بهذا الرقم')

  const otp = await prisma.otpCode.findFirst({
    where: { userId: user.id, purpose: OTP_PURPOSE, usedAt: null },
    orderBy: { createdAt: 'desc' }
  })
  if (!otp) throw new AppError(400, 'OTP_NOT_FOUND', 'اطلب رمز تحقق جديداً')
  if (otp.expiresAt <= new Date()) throw new AppError(400, 'OTP_EXPIRED', 'انتهت صلاحية رمز التحقق')
  if (otp.attempts >= OTP_MAX_ATTEMPTS) {
    throw new AppError(429, 'OTP_ATTEMPTS_EXCEEDED', 'تجاوزت عدد المحاولات المسموح، اطلب رمزاً جديداً')
  }

  if (otp.code !== code) {
    await prisma.otpCode.update({ where: { id: otp.id }, data: { attempts: { increment: 1 } } })
    throw new AppError(400, 'OTP_INVALID', 'رمز التحقق غير صحيح')
  }

  const verifiedAt = new Date()
  const [, updatedUser] = await prisma.$transaction([
    prisma.otpCode.update({ where: { id: otp.id }, data: { usedAt: verifiedAt } }),
    prisma.user.update({
      where: { id: user.id },
      data: { status: 'ACTIVE', phoneVerifiedAt: verifiedAt }
    })
  ])

  await recordAudit(prisma, {
    actorId: updatedUser.id,
    action: 'auth.phone.verify',
    entityType: 'User',
    entityId: updatedUser.id,
    before: { status: user.status, phoneVerifiedAt: user.phoneVerifiedAt },
    after: { status: updatedUser.status, phoneVerifiedAt: updatedUser.phoneVerifiedAt },
    req
  })

  // جدول Session يخزّن بصمة الرمز فقط: tokenHash = sha256(refreshToken).
  const refreshToken = randomToken()
  const session = await prisma.session.create({
    data: {
      userId: updatedUser.id,
      tokenHash: sha256(refreshToken),
      userAgent: req?.userAgent || null,
      ipAddress: req?.clientIp || null,
      expiresAt: new Date(Date.now() + SESSION_TTL_DAYS * 24 * 60 * 60 * 1000)
    }
  })

  const accessToken = signAccessToken({
    userId: updatedUser.id,
    role: updatedUser.role,
    sessionId: session.id
  })

  return {
    user: {
      id: updatedUser.id,
      name: updatedUser.name,
      phone: updatedUser.phone,
      role: updatedUser.role,
      status: updatedUser.status
    },
    accessToken,
    refreshToken
  }
}

/**
 * واجهة توافقية بصيغة المواصفة (PhoneAuthService) فوق الدوال المُصدَّرة،
 * حتى يعمل `import { phoneAuthService }` كما في التصميم الأصلي.
 */
export const phoneAuthService = { requestOtp, verifyOtp, generateOtpCode, sendSms }
