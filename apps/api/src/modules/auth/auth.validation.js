import { z } from 'zod'

export const registerSchema = z.object({
  name: z.string().trim().min(2).max(120),
  email: z.string().trim().toLowerCase().email(),
  password: z.string().min(8).max(128),
  phone: z.string().trim().max(32).optional(),
  locale: z.enum(['ar', 'en']).default('ar')
})

export const loginSchema = z.object({
  email: z.string().trim().toLowerCase().email(),
  password: z.string().min(1).max(128)
})

export const tokenSchema = z.object({ token: z.string().min(10) })

export const resetPasswordSchema = z.object({
  token: z.string().min(10),
  password: z.string().min(8).max(128)
})

export const changePasswordSchema = z.object({
  currentPassword: z.string().min(1),
  newPassword: z.string().min(8).max(128)
})

export const googleSchema = z.object({
  idToken: z.string().min(20)
})

export const refreshSchema = z.object({
  refreshToken: z.string().min(20)
})

export const emailOnlySchema = z.object({ email: z.string().trim().toLowerCase().email() })
