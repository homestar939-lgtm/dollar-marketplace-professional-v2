import { Router } from 'express'
import { authenticate } from '../../middleware/authentication.js'
import { unauthenticated } from '../../lib/errors.js'
import { subscribeRealtime } from '../../lib/realtime-hub.js'

export const realtimeRouter = Router()

function authenticateQueryToken(req, res, next) {
  const token = typeof req.query.token === 'string' ? req.query.token : ''
  if (!token) return next(unauthenticated('A session token is required'))
  req.headers.authorization = `Bearer ${token}`
  return authenticate()(req, res, next)
}

realtimeRouter.get('/events', authenticateQueryToken, (req, res) => {
  res.status(200)
  res.set({
    'Content-Type': 'text/event-stream; charset=utf-8',
    'Cache-Control': 'no-cache, no-transform',
    Connection: 'keep-alive',
    'X-Accel-Buffering': 'no'
  })
  res.flushHeaders?.()
  res.write('retry: 5000\n\n')

  const send = (type, payload) => {
    if (res.writableEnded || res.destroyed) return
    res.write(`event: ${type}\ndata: ${JSON.stringify(payload)}\n\n`)
  }

  send('connected', { connected: true, at: new Date().toISOString() })
  const unsubscribe = subscribeRealtime((event) => {
    if (!event.userIds.includes(req.user.id) && !event.roles.includes(req.user.role)) return
    send(event.type, { ...event.data, occurredAt: event.occurredAt })
  })
  const heartbeat = setInterval(() => {
    if (!res.writableEnded && !res.destroyed) res.write(`: heartbeat ${Date.now()}\n\n`)
  }, 25_000)
  heartbeat.unref?.()

  const cleanup = () => {
    clearInterval(heartbeat)
    unsubscribe()
  }
  res.on('close', cleanup)
  res.on('error', cleanup)
})
