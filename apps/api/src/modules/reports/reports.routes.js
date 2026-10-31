import { Router } from 'express'
import * as controller from './reports.controller.js'
import { authenticate } from '../../middleware/authentication.js'
import { requirePermission } from '../../middleware/authorization.js'
import { validate } from '../../middleware/validation.js'
import { periodSchema } from './reports.validation.js'

export const reportsRouter = Router()

reportsRouter.use(authenticate())
reportsRouter.get('/collections', requirePermission('reports.collection'), validate(periodSchema, 'query'), controller.collections)
reportsRouter.get('/commissions', requirePermission('commission.read'), validate(periodSchema, 'query'), controller.commissions)
reportsRouter.get('/financial', requirePermission('reports.financial'), validate(periodSchema, 'query'), controller.financial)
reportsRouter.get('/outstanding', requirePermission('reports.financial'), validate(periodSchema, 'query'), controller.outstanding)
