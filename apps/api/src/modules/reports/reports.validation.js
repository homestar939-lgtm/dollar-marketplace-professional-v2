import { z } from 'zod'

export const periodSchema = z.object({
  from: z.string().datetime().optional(),
  to: z.string().datetime().optional(),
  collectorId: z.string().optional(),
  currency: z.string().length(3).optional(),
  period: z.enum(['WEEKLY', 'MONTHLY', 'YEARLY']).optional(),
  date: z.string().regex(/^\d{4}-\d{2}-\d{2}$/).optional()
}).superRefine((query, context) => {
  if (query.date && !query.period) context.addIssue({ code: 'custom', path: ['period'], message: 'Period is required when date is provided' })
  if (query.period && (query.from || query.to)) context.addIssue({ code: 'custom', path: ['period'], message: 'Use either a named period or explicit from/to dates' })
  if (query.date) {
    const parsed = new Date(`${query.date}T00:00:00.000Z`)
    if (Number.isNaN(parsed.getTime()) || parsed.toISOString().slice(0, 10) !== query.date) {
      context.addIssue({ code: 'custom', path: ['date'], message: 'Invalid calendar date' })
    }
  }
})
