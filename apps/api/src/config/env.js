import { z } from 'zod'

const EnvSchema = z.object({
  NODE_ENV: z.enum(['development', 'test', 'production']).default('development'),
  PORT: z.coerce.number().int().positive().default(4000),
  DATABASE_URL: z.string().min(1, 'DATABASE_URL is required'),
  JWT_SECRET: z.string().min(16, 'JWT_SECRET must be at least 16 characters'),
  ACCESS_TOKEN_TTL: z.string().default('15m'),
  REFRESH_TOKEN_TTL_DAYS: z.coerce.number().int().positive().default(30),
  BCRYPT_ROUNDS: z.coerce.number().int().min(10).max(15).default(12),
  CORS_ORIGINS: z.string().default('*')
})

const parsed = EnvSchema.safeParse(process.env)
if (!parsed.success) {
  const detail = parsed.error.issues.map((i) => `${i.path.join('.')}: ${i.message}`).join('; ')
  throw new Error(`Invalid environment configuration -> ${detail}`)
}

if (parsed.data.NODE_ENV === 'production') {
  const problems = []
  if (parsed.data.JWT_SECRET.length < 32) problems.push('JWT_SECRET must be at least 32 characters')
  const origins = parsed.data.CORS_ORIGINS.split(',').map((origin) => origin.trim()).filter(Boolean)
  if (parsed.data.CORS_ORIGINS === '*' || origins.length === 0) {
    problems.push('CORS_ORIGINS must list explicit local-network origins')
  } else if (origins.some((origin) => {
    try {
      const url = new URL(origin)
      return !['http:', 'https:'].includes(url.protocol) || url.origin !== origin ||
        ['localhost', '127.0.0.1'].includes(url.hostname)
    } catch {
      return true
    }
  })) {
    problems.push('CORS_ORIGINS entries must be exact LAN HTTP(S) origins without paths')
  }
  if (problems.length) throw new Error(`Unsafe production environment -> ${problems.join('; ')}`)
}

export const env = {
  ...parsed.data,
  isProd: parsed.data.NODE_ENV === 'production',
  isTest: parsed.data.NODE_ENV === 'test',
  corsOrigins: parsed.data.CORS_ORIGINS === '*'
    ? '*'
    : parsed.data.CORS_ORIGINS.split(',').map((s) => s.trim()).filter(Boolean)
}
