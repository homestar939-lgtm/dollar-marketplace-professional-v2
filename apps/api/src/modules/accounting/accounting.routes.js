import { Router } from 'express'
import * as controller from './accounting.controller.js'
import { authenticate } from '../../middleware/authentication.js'
import { requirePermission } from '../../middleware/authorization.js'
import { validate } from '../../middleware/validation.js'
import { listBuyersSchema, statementSchema } from './accounting.validation.js'

export const accountingRouter = Router()

accountingRouter.use(authenticate())
accountingRouter.get('/me/statement', requirePermission('ledger.read.own'), validate(statementSchema, 'query'), (req, res, next) => {
  req.params.buyerId = req.user.id
  next()
}, controller.statement)
accountingRouter.get('/buyers', requirePermission('buyer.read'), validate(listBuyersSchema, 'query'), controller.buyers)
accountingRouter.get('/buyers/:buyerId/statement', requirePermission('ledger.read'), validate(statementSchema, 'query'), controller.statement)
accountingRouter.get('/matching', requirePermission('reports.financial'), controller.matching)
