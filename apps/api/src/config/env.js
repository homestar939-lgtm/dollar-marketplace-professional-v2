import { z } from 'zod'

/**
 * Environment contract. The API refuses to boot with a half-configured world:
 * a missing JWT_SECRET or DATABASE_URL is a fail-fast, not a 500 at runtime.
 */
const EnvSchema = z.object({
  NODE_ENV: z.enum(['development', 'test', 'production']).default('development'),
  PORT: z.coerce.number().int().positive().default(4000),
  DATABASE_URL: z.string().min(1, 'DATABASE_URL is required'),
  JWT_SECRET: z.string().min(16, 'JWT_SECRET must be at least 16 characters'),
  ACCESS_TOKEN_TTL: z.string().default('15m'),
  REFRESH_TOKEN_TTL_DAYS: z.coerce.number().int().positive().default(30),
  BCRYPT_ROUNDS: z.coerce.number().int().min(4).max(15).default(10),
  EMAIL_VERIFICATION_TTL_MINUTES: z.coerce.number().int().positive().default(1440),
  PASSWORD_RESET_TTL_MINUTES: z.coerce.number().int().positive().default(60),
  GOOGLE_CLIENT_ID: z.string().optional(),
  // Dev/test convenience: return verification/reset tokens in the response body.
  EXPOSE_TOKENS: z.enum(['true', 'false']).default('false'),
  // النسخة التجريبية: مفعّلة افتراضياً — الدخول من قائمة الأدوار بلا تحقق
  // برقم الهاتف (phone-auth يرجع 404). للعودة للوضع الكامل: DEMO_MODE="false".
  DEMO_MODE: z.enum(['true', 'false']).default('true'),
  CORS_ORIGINS: z.string().default('*'),
  SEED_ADMIN_EMAIL: z.string().default('admin@dollar.local'),
  SEED_ADMIN_PASSWORD: z.string().default('Admin@12345')
})

const parsed = EnvSchema.safeParse(process.env)
if (!parsed.success) {
  const detail = parsed.error.issues.map((i) => `${i.path.join('.')}: ${i.message}`).join('; ')
  throw new Error(`Invalid environment configuration -> ${detail}`)
}

export const env = {
  ...parsed.data,
  isProd: parsed.data.NODE_ENV === 'production',
  isTest: parsed.data.NODE_ENV === 'test',
  exposeTokens: parsed.data.EXPOSE_TOKENS === 'true',
  demoMode: parsed.data.DEMO_MODE === 'true',
  corsOrigins: parsed.data.CORS_ORIGINS === '*'
    ? '*'
    : parsed.data.CORS_ORIGINS.split(',').map((s) => s.trim()).filter(Boolean)
}
