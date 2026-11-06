import { randomUUID } from 'node:crypto'
import { EventEmitter } from 'node:events'

const bus = new EventEmitter()
bus.setMaxListeners(0)
const EVENT_NAME = 'realtime:event'

/** Publish only after the database transaction has committed. */
export function publishRealtimeEvent({ type, data, userIds = [], roles = [] }) {
  const event = {
    id: randomUUID(),
    type,
    data,
    userIds: [...new Set(userIds.filter(Boolean))],
    roles: [...new Set(roles)],
    occurredAt: new Date().toISOString()
  }
  bus.emit(EVENT_NAME, event)
  return event.id
}

export function subscribeRealtime(listener) {
  bus.on(EVENT_NAME, listener)
  return () => bus.off(EVENT_NAME, listener)
}
