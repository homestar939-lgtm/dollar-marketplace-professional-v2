import { randomUUID } from 'node:crypto'

export function requestContext(req, res, next) {
  req.id = req.headers['x-request-id'] || randomUUID()
  res.setHeader('x-request-id', req.id)
  req.clientIp = (req.headers['x-forwarded-for'] || '').split(',')[0].trim() || req.ip || null
  req.userAgent = req.headers['user-agent'] || null
  next()
}
