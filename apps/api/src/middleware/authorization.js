import { forbidden } from '../lib/errors.js'

/**
 * Permissions live on the server. Hiding a button in the client is a UX
 * decision; these checks are the security boundary.
 */
export const ROLE_PERMISSIONS = {
  ADMIN: ['*'],
  WAREHOUSE: [
    'product.read', 'product.write', 'agency.read',
    'order.read', 'order.prepare', 'order.ship',
    'invoice.read', 'invoice.create', 'invoice.send',
    'notification.read'
  ],
  COLLECTOR: [
    'agency.read', 'product.read', 'buyer.read',
    'receipt.read', 'receipt.create', 'receipt.send',
    'ledger.read', 'commission.read', 'reports.collection',
    'notification.read'
  ],
  ACCOUNTANT: [
    'agency.read', 'product.read', 'order.read', 'buyer.read',
    'invoice.read', 'invoice.approve', 'invoice.reject', 'invoice.void',
    'receipt.read', 'receipt.approve', 'receipt.reject', 'receipt.void',
    'ledger.read', 'reports.financial', 'reports.collection',
    'commission.read', 'audit.read',
    'notification.read'
  ],
  BUYER: [
    'agency.read', 'product.read',
    'order.create', 'order.read.own',
    'invoice.read.own', 'receipt.read.own', 'ledger.read.own',
    'notification.read'
  ],
  SELLER: ['agency.read', 'product.read', 'product.write', 'notification.read'],
  SUPPORT: ['agency.read', 'product.read', 'order.read', 'invoice.read', 'receipt.read', 'notification.read']
}

export function permissionsForRole(role) {
  return ROLE_PERMISSIONS[role] || []
}

export function hasPermission(user, permission) {
  if (!user) return false
  const granted = permissionsForRole(user.role)
  return granted.includes('*') || granted.includes(permission)
}

export function requirePermission(...permissions) {
  return function authorizationMiddleware(req, res, next) {
    if (!req.user) return next(forbidden('Authentication required'))
    const missing = permissions.filter((p) => !hasPermission(req.user, p))
    if (missing.length) {
      return next(forbidden(`Missing permission(s): ${missing.join(', ')}`))
    }
    next()
  }
}

export function requireRole(...roles) {
  return function roleMiddleware(req, res, next) {
    if (!req.user) return next(forbidden('Authentication required'))
    if (!roles.includes(req.user.role)) return next(forbidden(`Requires role: ${roles.join(' or ')}`))
    next()
  }
}

export function requireSelfOrPermission(permission) {
  return function selfOrPermission(req, res, next) {
    if (String(req.params.userId) === String(req.user?.id) || hasPermission(req.user, permission)) return next()
    next(forbidden())
  }
}
