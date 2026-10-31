import { z } from 'zod'

export const createOrderSchema = z.object({
  items: z.array(z.object({
    productId: z.string().min(1),
    quantity: z.coerce.number().int().positive()
  })).min(1).max(100),
  notes: z.string().trim().max(2000).optional()
})

export const listOrdersSchema = z.object({
  status: z.enum(['PENDING', 'CONFIRMED', 'PREPARED', 'INVOICED', 'PROCESSING', 'SHIPPED', 'IN_TRANSIT', 'DELIVERED', 'CANCELLED', 'RETURNED', 'REFUNDED']).optional(),
  buyerId: z.string().optional(),
  q: z.string().trim().max(120).optional(),
  page: z.coerce.number().int().positive().default(1),
  limit: z.coerce.number().int().positive().max(100).default(20)
})

export const prepareOrderSchema = z.object({
  items: z.array(z.object({
    orderItemId: z.string().min(1),
    preparedQuantity: z.coerce.number().int().nonnegative()
  })).min(1),
  note: z.string().trim().max(1000).optional()
})

export const shipOrderSchema = z.object({
  courierPhone: z.string().trim().min(3).max(32),
  courierName: z.string().trim().max(120).optional(),
  courierChannel: z.string().trim().max(120).optional()
})
