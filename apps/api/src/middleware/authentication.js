import { prisma } from '../lib/prisma.js'
import { verifyAccessToken } from '../lib/jwt.js'
import { unauthenticated, forbidden } from '../lib/errors.js'

/** Resolves the bearer token into req.user and enforces the session is alive. */
export function authenticate({ optional = false } = {}) {
  return async function authenticationMiddleware(req, res, next) {
    try {
      const header = req.headers.authorization || ''
      const [scheme, token] = header.split(' ')
      if (scheme !== 'Bearer' || !token) {
        if (optional) return next()
        throw unauthenticated('Missing bearer token')
      }

      let payload
      try {
        payload = verifyAccessToken(token)
      } catch {
        throw unauthenticated('Invalid or expired token')
      }

      const session = await prisma.session.findUnique({ where: { id: payload.sid } })
      if (!session || session.revokedAt || session.expiresAt.getTime() <= Date.now()) {
        throw unauthenticated('Session is no longer valid')
      }

      const user = await prisma.user.findUnique({ where: { id: payload.sub } })
      if (!user) throw unauthenticated('User no longer exists')
      if (user.status !== 'ACTIVE') throw forbidden('Account is not active')

      req.user = user
      req.session = session
      next()
    } catch (error) {
      next(error)
    }
  }
}
