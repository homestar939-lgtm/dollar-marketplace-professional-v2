import { Router } from 'express'
import { deliveryService } from './delivery.service.js'
import { authenticate } from '../../middleware/authentication.js'
import { requireRole } from '../../middleware/authorization.js'
import { asyncHandler } from '../../lib/async-handler.js'

/** يُركّب في app.js على /api/v3 فيصبح المسار /api/v3/orders/:id/deliver */
export const deliveryRouter = Router()

// ملاحظة: المصادقة تُوضع لكل مسار (لا `router.use`) لأن هذا الراوتر يتشارك
// البادئة /api/v3 مع راوتر آخر؛ `use` كان سيعترض المسارات المجهولة بـ 401 بدل 404.
deliveryRouter.post(
  '/orders/:id/deliver',
  authenticate(),
  requireRole('BUYER'),
  asyncHandler(async (req, res) => {
    const order = await deliveryService.confirmDelivery(req.params.id, req.user.id, req.body?.proof)
    res.json({ order })
  })
)
