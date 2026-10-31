import { z } from 'zod'

export const listProductsSchema = z.object({
  q: z.string().trim().max(120).optional(),
  agencyId: z.string().optional(),
  status: z.enum(['DRAFT', 'ACTIVE', 'INACTIVE', 'PENDING_REVIEW']).optional(),
  minPrice: z.coerce.number().nonnegative().optional(),
  maxPrice: z.coerce.number().nonnegative().optional(),
  currency: z.string().length(3).optional(),
  page: z.coerce.number().int().positive().default(1),
  limit: z.coerce.number().int().positive().max(100).default(20)
})

export const createProductSchema = z.object({
  agencyId: z.string().min(1),
  name: z.string().trim().min(2).max(200),
  nameEn: z.string().trim().max(200).optional(),
  sku: z.string().trim().min(1).max(64),
  description: z.string().trim().max(4000).optional(),
  price: z.union([z.string(), z.number()]),
  currency: z.string().length(3).default('USD'),
  unit: z.string().trim().max(32).default('piece'),
  minimumOrderQuantity: z.coerce.number().int().positive().default(1),
  stockQuantity: z.coerce.number().int().nonnegative().nullable().optional(),
  expiryDate: z.string().datetime().nullable().optional(),
  countryOfOrigin: z.string().trim().max(80).nullable().optional(),
  imageUrl: z.string().url().max(500).nullable().optional(),
  images: z.array(z.string().url().max(500)).max(10).optional(),
  specs: z.record(z.string(), z.any()).optional(),
  status: z.enum(['DRAFT', 'ACTIVE', 'INACTIVE', 'PENDING_REVIEW']).default('DRAFT'),
  showExpiry: z.boolean().optional(),
  showCountry: z.boolean().optional(),
  showDescription: z.boolean().optional(),
  showImages: z.boolean().optional(),
  showSpecs: z.boolean().optional()
})

export const updateProductSchema = createProductSchema.partial().omit({ agencyId: true })
