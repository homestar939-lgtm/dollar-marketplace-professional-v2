import { z } from 'zod'

export const roleEnum = z.enum(['ADMIN', 'WAREHOUSE', 'COLLECTOR', 'ACCOUNTANT', 'BUYER', 'SELLER', 'SUPPORT'])

export const listUsersSchema = z.object({
  role: roleEnum.optional(),
  status: z.enum(['PENDING_VERIFICATION', 'ACTIVE', 'SUSPENDED', 'DISABLED']).optional(),
  q: z.string().trim().max(120).optional(),
  page: z.coerce.number().int().positive().default(1),
  limit: z.coerce.number().int().positive().max(100).default(20)
})

export const createStaffSchema = z.object({
  name: z.string().trim().min(2).max(120),
  username: z.string().trim().min(3).max(40).regex(/^[a-zA-Z0-9_.-]+$/),
  password: z.string().min(12).max(128),
  role: z.enum(['ADMIN', 'WAREHOUSE', 'COLLECTOR', 'ACCOUNTANT', 'BUYER']),
  phone: z.string().trim().max(32).optional(),
  commissionRate: z.coerce.number().min(0).max(1).optional(),
  agencyId: z.string().optional()
})

export const updateUserSchema = z.object({
  name: z.string().trim().min(2).max(120).optional(),
  phone: z.string().trim().max(32).nullable().optional(),
  status: z.enum(['ACTIVE', 'SUSPENDED', 'DISABLED']).optional(),
  role: z.enum(['WAREHOUSE', 'COLLECTOR', 'ACCOUNTANT', 'BUYER', 'SUPPORT']).optional(),
  commissionRate: z.coerce.number().min(0).max(1).nullable().optional(),
  agencyId: z.string().nullable().optional()
})
