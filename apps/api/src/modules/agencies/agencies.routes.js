import { Router } from 'express'
import * as controller from './agencies.controller.js'
import { authenticate } from '../../middleware/authentication.js'
import { requirePermission } from '../../middleware/authorization.js'
import { validate } from '../../middleware/validation.js'
import { listAgenciesSchema, createAgencySchema, updateAgencySchema } from './agencies.validation.js'

export const agenciesRouter = Router()

agenciesRouter.use(authenticate())
agenciesRouter.get('/', requirePermission('agency.read'), validate(listAgenciesSchema, 'query'), controller.list)
agenciesRouter.get('/:id', requirePermission('agency.read'), controller.get)
agenciesRouter.post('/', requirePermission('agency.write'), validate(createAgencySchema), controller.create)
agenciesRouter.patch('/:id', requirePermission('agency.write'), validate(updateAgencySchema), controller.update)
