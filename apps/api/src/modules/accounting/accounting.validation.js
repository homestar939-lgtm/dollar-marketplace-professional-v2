import { z } from 'zod'

export const listBuyersSchema = z.object({
  q: z.string().trim().max(120).optional(),
  onlyWithBalance: z.enum(['true', 'false']).default('false'),
  page: z.coerce.number().int().positive().default(1),
  limit: z.coerce.number().int().positive().max(100).default(20)
})

export const statementSchema = z.object({
  from: z.string().datetime().optional(),
  to: z.string().datetime().optional(),
  currency: z.string().length(3).optional()
})
