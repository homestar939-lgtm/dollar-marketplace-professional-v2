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
  // Dev/test convenience only. Production defaults to the authenticated flow.
  DEMO_MODE: z.enum(['true', 'false']).default(process.env.NODE_ENV === 'production' ? 'false' : 'true'),
  CORS_ORIGINS: z.string().default('*'),
  SEED_ADMIN_EMAIL: z.string().default('admin@dollar.local'),
  SEED_ADMIN_PASSWORD: z.string().default('Admin@12345')
})

const parsed = EnvSchema.safeParse(process.env)
if (!parsed.success) {
  const detail = parsed.error.issues.map((i) => `${i.path.join('.')}: ${i.message}`).join('; ')
  throw new Error(`Invalid environment configuration -> ${detail}`)
}

if (parsed.data.NODE_ENV === 'production') {
  const problems = []
  if (parsed.data.DEMO_MODE !== 'false') problems.push('DEMO_MODE must be false')
  if (parsed.data.EXPOSE_TOKENS !== 'false') problems.push('EXPOSE_TOKENS must be false')
  if (parsed.data.JWT_SECRET.length < 32) problems.push('JWT_SECRET must be at least 32 characters')

  const origins = parsed.data.CORS_ORIGINS.split(',').map((origin) => origin.trim()).filter(Boolean)
  if (parsed.data.CORS_ORIGINS === '*' || origins.length === 0) {
    problems.push('CORS_ORIGINS must list explicit HTTPS origins')
  } else if (origins.some((origin) => {
    try {
      const url = new URL(origin)
      return url.protocol !== 'https:' || url.origin !== origin
    } catch {
      return true
    }
  })) {
    problems.push('CORS_ORIGINS entries must be exact HTTPS origins without paths')
  }

  if (problems.length) throw new Error(`Unsafe production environment -> ${problems.join('; ')}`)
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
