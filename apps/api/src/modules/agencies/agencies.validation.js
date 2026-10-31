import { z } from 'zod'

export const listAgenciesSchema = z.object({
  q: z.string().trim().max(120).optional(),
  status: z.enum(['ACTIVE', 'INACTIVE', 'SUSPENDED']).optional(),
  page: z.coerce.number().int().positive().default(1),
  limit: z.coerce.number().int().positive().max(100).default(20)
})

export const createAgencySchema = z.object({
  name: z.string().trim().min(2).max(160),
  nameEn: z.string().trim().max(160).optional(),
  slug: z.string().trim().min(2).max(80).regex(/^[a-z0-9-]+$/, 'slug must be lowercase letters, digits and dashes'),
  description: z.string().trim().max(2000).optional(),
  logoUrl: z.string().url().max(500).optional(),
  contactPhone: z.string().trim().max(32).optional(),
  contactEmail: z.string().trim().email().optional(),
  status: z.enum(['ACTIVE', 'INACTIVE', 'SUSPENDED']).default('ACTIVE')
})

export const updateAgencySchema = createAgencySchema.partial()
