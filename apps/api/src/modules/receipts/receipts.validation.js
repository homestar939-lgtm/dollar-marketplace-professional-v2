import { z } from 'zod'

export const createReceiptSchema = z.object({
  buyerId: z.string().min(1),
  amount: z.union([z.string(), z.number()]),
  currency: z.string().length(3).default('USD'),
  paymentMethod: z.enum(['CASH', 'BANK_TRANSFER', 'CHEQUE', 'MOBILE_WALLET', 'CARD', 'OTHER']).default('CASH'),
  collectionDate: z.string().datetime().optional(),
  invoiceIds: z.array(z.string()).max(50).default([]),
  notes: z.string().trim().max(1000).optional(),
  overpaymentReason: z.string().trim().max(500).optional()
})

export const rejectReceiptSchema = z.object({ reason: z.string().trim().min(3).max(1000) })

export const listReceiptsSchema = z.object({
  status: z.enum(['DRAFT', 'SENT', 'APPROVED', 'REJECTED', 'VOIDED']).optional(),
  buyerId: z.string().optional(),
  collectorId: z.string().optional(),
  q: z.string().trim().max(120).optional(),
  page: z.coerce.number().int().positive().default(1),
  limit: z.coerce.number().int().positive().max(100).default(20)
})
