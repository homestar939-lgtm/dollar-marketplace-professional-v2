import { z } from 'zod'

export const loginSchema = z.object({
  username: z.string().trim().min(1).max(80),
  password: z.string().min(1).max(128)
})
export const changePasswordSchema = z.object({
  currentPassword: z.string().min(1).max(128),
  newPassword: z.string().min(12).max(128)
})
export const refreshSchema = z.object({ refreshToken: z.string().min(20) })
