import { z } from 'zod'

export const updateSettingsSchema = z.object({
  companyName: z.string().trim().min(1).max(200).optional(),
  companyNameEn: z.string().trim().max(200).optional(),
  companyLogo: z.string().url().max(500).nullable().optional(),
  companySignature: z.string().url().max(500).nullable().optional(),
  companyAddress: z.string().trim().max(500).optional(),
  companyPhone: z.string().trim().max(32).optional(),
  companyTaxId: z.string().trim().max(64).optional(),
  defaultCurrency: z.string().length(3).optional(),
  paymentTerms: z.coerce.number().int().min(0).max(365).optional(),
  collectorCommissionRates: z.record(z.string(), z.coerce.number().min(0).max(1)).optional(),
  productDisplaySettings: z.record(z.string(), z.any()).optional()
})
