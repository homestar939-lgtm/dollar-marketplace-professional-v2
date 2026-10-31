import jwt from 'jsonwebtoken'
import { env } from '../config/env.js'

export function signAccessToken({ userId, role, sessionId }) {
  return jwt.sign({ sub: userId, role, sid: sessionId, typ: 'access' }, env.JWT_SECRET, {
    expiresIn: env.ACCESS_TOKEN_TTL
  })
}

export function verifyAccessToken(token) {
  const payload = jwt.verify(token, env.JWT_SECRET)
  if (payload.typ !== 'access') throw new Error('unexpected token type')
  return payload
}
