import { Router } from 'express'
import * as controller from './settings.controller.js'
import { authenticate } from '../../middleware/authentication.js'
import { requirePermission } from '../../middleware/authorization.js'
import { validate } from '../../middleware/validation.js'
import { updateSettingsSchema } from './settings.validation.js'

export const settingsRouter = Router()

settingsRouter.use(authenticate())
settingsRouter.get('/', controller.get)
settingsRouter.patch('/', requirePermission('settings.write'), validate(updateSettingsSchema), controller.update)
