import { z } from 'zod'

export const createInvoiceSchema = z.object({
  orderId: z.string().min(1),
  lines: z.array(z.object({ orderItemId: z.string().min(1), quantity: z.coerce.number().int().positive() })).optional()
})

export const updateInvoiceSchema = z.object({
  lines: z.array(z.object({ orderItemId: z.string().min(1), quantity: z.coerce.number().int().positive() })).min(1)
})

export const rejectInvoiceSchema = z.object({ reason: z.string().trim().min(3).max(1000) })

export const listInvoicesSchema = z.object({
  status: z.enum(['DRAFT', 'SENT', 'APPROVED', 'REJECTED', 'VOIDED']).optional(),
  buyerId: z.string().optional(),
  q: z.string().trim().max(120).optional(),
  page: z.coerce.number().int().positive().default(1),
  limit: z.coerce.number().int().positive().max(100).default(20)
})
