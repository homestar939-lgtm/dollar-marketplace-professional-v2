import { Router } from 'express'
import * as controller from './users.controller.js'
import { authenticate } from '../../middleware/authentication.js'
import { requirePermission } from '../../middleware/authorization.js'
import { validate } from '../../middleware/validation.js'
import { listUsersSchema, createStaffSchema, updateUserSchema } from './users.validation.js'

export const usersRouter = Router()

usersRouter.use(authenticate())
usersRouter.get('/', requirePermission('users.read'), validate(listUsersSchema, 'query'), controller.list)
usersRouter.post('/', requirePermission('users.manage'), validate(createStaffSchema), controller.create)
usersRouter.get('/:id', requirePermission('users.read'), controller.get)
usersRouter.patch('/:id', requirePermission('users.manage'), validate(updateUserSchema), controller.update)
