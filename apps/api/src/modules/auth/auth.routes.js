import { Router } from 'express'
import * as controller from './auth.controller.js'
import { authenticate } from '../../middleware/authentication.js'
import { validate } from '../../middleware/validation.js'
import {
  registerSchema, loginSchema, tokenSchema, resetPasswordSchema,
  changePasswordSchema, googleSchema, refreshSchema, emailOnlySchema
} from './auth.validation.js'

export const authRouter = Router()

authRouter.post('/register', validate(registerSchema), controller.register)
authRouter.post('/verify-email', validate(tokenSchema), controller.verifyEmail)
authRouter.post('/login', validate(loginSchema), controller.login)
authRouter.post('/google', validate(googleSchema), controller.google)
authRouter.post('/refresh', validate(refreshSchema), controller.refresh)
authRouter.post('/forgot-password', validate(emailOnlySchema), controller.forgotPassword)
authRouter.post('/reset-password', validate(resetPasswordSchema), controller.resetPassword)
authRouter.post('/logout', authenticate(), controller.logout)
authRouter.get('/me', authenticate(), controller.me)
authRouter.get('/sessions', authenticate(), controller.sessions)
authRouter.post('/change-password', authenticate(), validate(changePasswordSchema), controller.changePassword)
