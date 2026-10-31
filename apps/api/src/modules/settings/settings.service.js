import { prisma } from '../../lib/prisma.js'
import { assertCurrency } from '../../utils/currency.js'
import { recordAudit } from '../../utils/audit.js'

export async function getSettings() {
  const existing = await prisma.systemSettings.findUnique({ where: { id: 'default' } })
  if (existing) return existing
  return prisma.systemSettings.create({ data: { id: 'default' } })
}

export async function updateSettings({ data, actor, req }) {
  if (data.defaultCurrency) assertCurrency(data.defaultCurrency)
  const before = await getSettings()

  const updated = await prisma.systemSettings.update({
    where: { id: 'default' },
    data: { ...data, updatedById: actor.id }
  })
  await recordAudit(prisma, {
    actorId: actor.id, action: 'settings.update', entityType: 'SystemSettings', entityId: 'default',
    before: { companyName: before.companyName, defaultCurrency: before.defaultCurrency },
    after: { companyName: updated.companyName, defaultCurrency: updated.defaultCurrency }, req
  })
  return updated
}
