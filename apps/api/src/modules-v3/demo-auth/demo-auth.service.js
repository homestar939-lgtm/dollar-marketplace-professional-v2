import { prisma } from '../../lib/prisma.js'
import { signAccessToken } from '../../lib/jwt.js'
import { randomToken, sha256 } from '../../lib/tokens.js'
import { recordAudit } from '../../utils/audit.js'
import { badRequest } from '../../lib/errors.js'

// الأدوار التجريبية الخمسة فقط. أي دور خارجها يُرفض بـ 400 عبر badRequest
// (لا نستخدم zod enum هنا لأن `validate` يحوّل أخطاء zod إلى 422).
export const DEMO_ROLES = ['ADMIN', 'WAREHOUSE', 'COLLECTOR', 'ACCOUNTANT', 'BUYER']

const SESSION_TTL_MS = 30 * 24 * 60 * 60 * 1000

// نفس شكل `modules/auth`: فقط الحقول التي يحتاجها العميل بعد الدخول التجريبي.
const publicUser = (user) => ({
  id: user.id,
  name: user.name,
  email: user.email,
  role: user.role,
  status: user.status
})

/**
 * دخول تجريبي بلا كلمة مرور: find-or-create لمستخدم ACTIVE بالدور المطلوب،
 * ثم جلسة حقيقية (tokenHash فقط) و access token موقّع مثل أي جلسة أخرى.
 */
export async function login({ role, req }) {
  if (typeof role !== 'string' || !DEMO_ROLES.includes(role)) {
    throw badRequest(`role must be one of: ${DEMO_ROLES.join(', ')}`)
  }

  let user = await prisma.user.findFirst({ where: { role, status: 'ACTIVE' } })
  if (!user) {
    // User.email إلزامي وفريد ⇒ بريد مشتق من الدور.
    user = await prisma.user.create({
      data: {
        email: `demo.${role.toLowerCase()}@dollar.local`,
        name: `حساب ${role} التجريبي`,
        role,
        status: 'ACTIVE',
        emailVerifiedAt: new Date(),
        locale: 'ar'
      }
    })
  }

  const refreshToken = randomToken()
  const session = await prisma.session.create({
    data: {
      userId: user.id,
      tokenHash: sha256(refreshToken),
      userAgent: req?.userAgent || null,
      ipAddress: req?.clientIp || null,
      expiresAt: new Date(Date.now() + SESSION_TTL_MS)
    }
  })

  const accessToken = signAccessToken({ userId: user.id, role: user.role, sessionId: session.id })

  await recordAudit(prisma, {
    actorId: user.id,
    action: 'auth.login.demo',
    entityType: 'USER',
    entityId: user.id,
    after: { role }
  })

  return { user: publicUser(user), accessToken, refreshToken }
}
