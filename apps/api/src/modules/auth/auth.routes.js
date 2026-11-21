import { Router } from 'express'
import * as controller from './auth.controller.js'
import { authenticate } from '../../middleware/authentication.js'
import { validate } from '../../middleware/validation.js'
import { loginSchema, changePasswordSchema, refreshSchema } from './auth.validation.js'

export const authRouter = Router()

authRouter.post('/login', validate(loginSchema), controller.login)
authRouter.post('/refresh', validate(refreshSchema), controller.refresh)
authRouter.post('/logout', authenticate(), controller.logout)
authRouter.get('/me', authenticate(), controller.me)
authRouter.get('/sessions', authenticate(), controller.sessions)
authRouter.post('/change-password', authenticate(), validate(changePasswordSchema), controller.changePassword)
