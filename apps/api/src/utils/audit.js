/**
 * Audit trail writer. Always pass the surrounding transaction when the audited
 * action is financial, otherwise the log can survive a rolled-back mutation.
 */
export async function recordAudit(client, { actorId = null, action, entityType, entityId = null, before = null, after = null, req = null }) {
  return client.auditLog.create({
    data: {
      actorId,
      action,
      entityType,
      entityId,
      before: before ?? undefined,
      after: after ?? undefined,
      ipAddress: req?.clientIp ?? null,
      userAgent: req?.userAgent ?? null
    }
  })
}
