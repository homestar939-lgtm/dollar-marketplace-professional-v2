import { Router } from 'express'
import * as controller from './notifications.controller.js'
import { authenticate } from '../../middleware/authentication.js'
import { requirePermission } from '../../middleware/authorization.js'

export const notificationsRouter = Router()

notificationsRouter.use(authenticate(), requirePermission('notification.read'))
notificationsRouter.get('/', controller.list)
notificationsRouter.post('/read-all', controller.readAll)
notificationsRouter.post('/:id/read', controller.read)
