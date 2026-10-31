import { z } from 'zod'

export const periodSchema = z.object({
  from: z.string().datetime().optional(),
  to: z.string().datetime().optional(),
  collectorId: z.string().optional(),
  currency: z.string().length(3).optional()
})
