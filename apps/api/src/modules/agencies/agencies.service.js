import { prisma } from '../../lib/prisma.js'
import { conflict, notFound } from '../../lib/errors.js'
import { recordAudit } from '../../utils/audit.js'

export async function listAgencies({ q, status, page, limit, viewer }) {
  const isStaff = ['ADMIN', 'WAREHOUSE', 'ACCOUNTANT', 'COLLECTOR', 'SUPPORT'].includes(viewer.role)
  const where = {
    ...(status && isStaff ? { status } : { status: 'ACTIVE' }),
    ...(q
      ? { OR: [{ name: { contains: q, mode: 'insensitive' } }, { nameEn: { contains: q, mode: 'insensitive' } }] }
      : {})
  }
  const [items, total] = await Promise.all([
    prisma.agency.findMany({ where, orderBy: { name: 'asc' }, skip: (page - 1) * limit, take: limit, include: { _count: { select: { products: true } } } }),
    prisma.agency.count({ where })
  ])
  return { data: items, meta: { page, limit, total, pages: Math.max(1, Math.ceil(total / limit)) } }
}

export async function getAgency({ agencyId, viewer }) {
  const agency = await prisma.agency.findUnique({
    where: { id: agencyId },
    include: { _count: { select: { products: true } } }
  })
  if (!agency) throw notFound('Agency not found')
  if (agency.status !== 'ACTIVE' && !['ADMIN', 'SUPPORT'].includes(viewer.role)) throw notFound('Agency not found')
  return agency
}

export async function createAgency({ data, actor, req }) {
  const existing = await prisma.agency.findUnique({ where: { slug: data.slug } })
  if (existing) throw conflict('An agency with this slug already exists')

  const agency = await prisma.agency.create({ data: { ...data, createdById: actor.id } })
  await recordAudit(prisma, { actorId: actor.id, action: 'agency.create', entityType: 'Agency', entityId: agency.id, after: { name: agency.name, slug: agency.slug }, req })
  return agency
}

export async function updateAgency({ agencyId, data, actor, req }) {
  const agency = await prisma.agency.findUnique({ where: { id: agencyId } })
  if (!agency) throw notFound('Agency not found')

  const updated = await prisma.agency.update({ where: { id: agencyId }, data })
  await recordAudit(prisma, {
    actorId: actor.id, action: 'agency.update', entityType: 'Agency', entityId: agencyId,
    before: { name: agency.name, status: agency.status }, after: { name: updated.name, status: updated.status }, req
  })
  return updated
}
