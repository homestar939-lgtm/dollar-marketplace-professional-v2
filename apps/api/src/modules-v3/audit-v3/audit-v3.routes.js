import { Router } from 'express'
import { z } from 'zod'
import { authenticate } from '../../middleware/authentication.js'
import { requireRole } from '../../middleware/authorization.js'
import { validate } from '../../middleware/validation.js'
import { getFullAuditLog, getAuditStats } from './audit-v3.service.js'

const auditLogQuerySchema = z.object({
  page: z.coerce.number().int().positive().default(1),
  limit: z.coerce.number().int().positive().max(200).default(50),
  userId: z.string().min(1).optional(),
  action: z.string().min(1).optional(),
  entityType: z.string().min(1).optional(),
  entityId: z.string().min(1).optional(),
  from: z.string().datetime().optional(),
  to: z.string().datetime().optional()
})

const statsQuerySchema = z.object({
  from: z.string().datetime().optional(),
  to: z.string().datetime().optional()
})

export const auditV3Router = Router()

// المصادقة لكل مسار (لا `router.use`) لأن الراوتر يتشارك البادئة /api/v3
// مع راوتر التسليم؛ `use` كان سيرد 401 على أي مسار مجهول تحت /api/v3.
auditV3Router.get('/audit-logs/stats', authenticate(), requireRole('ADMIN'), validate(statsQuerySchema, 'query'), async (req, res, next) => {
  try {
    const stats = await getAuditStats(req.query)
    res.json(stats)
  } catch (error) {
    next(error)
  }
})

auditV3Router.get('/audit-logs', authenticate(), requireRole('ADMIN'), validate(auditLogQuerySchema, 'query'), async (req, res, next) => {
  try {
    const result = await getFullAuditLog(req.query)
    res.json(result)
  } catch (error) {
    next(error)
  }
})
