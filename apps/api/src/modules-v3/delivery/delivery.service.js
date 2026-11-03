import { prisma } from '../../lib/prisma.js'
import { recordAudit } from '../../utils/audit.js'
import { notificationService } from '../../services-v3/notification.service.js'
import { AppError } from '../../lib/errors.js'

/** خدمة تأكيد تسليم الطلب من قبل المشتري (V3). */
class DeliveryService {
  async confirmDelivery(orderId, userId, proof = null) {
    const order = await prisma.order.findUnique({
      where: { id: orderId },
      include: { items: true, buyer: true }
    })

    if (!order) throw new AppError(404, 'ORDER_NOT_FOUND', 'الطلب غير موجود')
    if (order.status !== 'SHIPPED') throw new AppError(409, 'INVALID_STATUS', 'لا يمكن تأكيد تسليم طلب حالته ليست SHIPPED')
    if (order.buyerId !== userId) throw new AppError(403, 'FORBIDDEN', 'تأكيد التسليم متاح لمشتري الطلب فقط')

    const updatedOrder = await prisma.order.update({
      where: { id: orderId },
      data: {
        status: 'DELIVERED',
        deliveredAt: new Date(),
        deliveredBy: userId
      },
      include: { items: true, buyer: true }
    })

    await recordAudit(prisma, {
      actorId: userId,
      action: 'order.deliver',
      entityType: 'ORDER',
      entityId: orderId,
      before: { status: 'SHIPPED' },
      after: { status: 'DELIVERED', deliveredAt: updatedOrder.deliveredAt }
    })

    await notificationService.notify({
      userIds: await this.getWarehouseAndAdminIds(),
      title: 'تم تسليم الطلب',
      message: `تم تسليم الطلب رقم ${order.orderNumber} من قبل المشتري`,
      type: 'ORDER',
      data: { orderId }
    })

    return updatedOrder
  }

  async getWarehouseAndAdminIds() {
    const users = await prisma.user.findMany({
      where: { role: { in: ['WAREHOUSE', 'ADMIN'] } },
      select: { id: true }
    })
    return users.map((user) => user.id)
  }
}

export const deliveryService = new DeliveryService()
export { DeliveryService }
