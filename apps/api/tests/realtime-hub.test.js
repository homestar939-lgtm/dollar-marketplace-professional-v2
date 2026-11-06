import test from 'node:test'
import assert from 'node:assert/strict'
import { publishRealtimeEvent, subscribeRealtime } from '../src/lib/realtime-hub.js'

test('publishes a uniquely identified event with deduplicated audiences', () => {
  let received
  const unsubscribe = subscribeRealtime((event) => { received = event })
  const id = publishRealtimeEvent({
    type: 'order.created',
    data: { orderId: 'order-1' },
    userIds: ['buyer-1', 'buyer-1', null],
    roles: ['ADMIN', 'ADMIN']
  })
  unsubscribe()

  assert.equal(received.id, id)
  assert.equal(received.type, 'order.created')
  assert.deepEqual(received.data, { orderId: 'order-1' })
  assert.deepEqual(received.userIds, ['buyer-1'])
  assert.deepEqual(received.roles, ['ADMIN'])
  assert.ok(Number.isFinite(Date.parse(received.occurredAt)))
})

test('unsubscribed listeners do not receive future events', () => {
  let count = 0
  const unsubscribe = subscribeRealtime(() => { count += 1 })
  unsubscribe()
  publishRealtimeEvent({ type: 'invoice.approved', data: { invoiceId: 'invoice-1' } })
  assert.equal(count, 0)
})
