import { Prisma } from '@prisma/client'
import { AppError } from '../lib/errors.js'
import { env } from '../config/env.js'

export function notFoundHandler(req, res) {
  res.status(404).json({ error: { code: 'NOT_FOUND', message: `Route ${req.method} ${req.path} not found` } })
}

export function errorHandler(error, req, res, _next) {
  if (error instanceof AppError) {
    return res.status(error.status).json({
      error: { code: error.code, message: error.message, details: error.details ?? undefined },
      requestId: req.id
    })
  }

  if (error instanceof Prisma.PrismaClientKnownRequestError) {
    if (error.code === 'P2002') {
      return res.status(409).json({
        error: { code: 'CONFLICT', message: 'A record with the same unique value already exists', details: error.meta },
        requestId: req.id
      })
    }
    if (error.code === 'P2025') {
      return res.status(404).json({ error: { code: 'NOT_FOUND', message: 'Record not found' }, requestId: req.id })
    }
    if (error.code === 'P2003') {
      return res.status(409).json({
        error: { code: 'FOREIGN_KEY', message: 'Related record is missing or still referenced' },
        requestId: req.id
      })
    }
  }

  // Serialization failures from SERIALIZABLE transactions must be retried by
  // the caller, never silently swallowed.
  if (error instanceof Prisma.PrismaClientUnknownRequestError || error?.code === '40001') {
    return res.status(409).json({
      error: { code: 'CONCURRENT_MODIFICATION', message: 'The operation conflicted with another transaction, please retry' },
      requestId: req.id
    })
  }

  if (env.isTest) console.error('[error]', error)
  else console.error('[error]', error?.stack || error)

  return res.status(500).json({
    error: { code: 'INTERNAL_ERROR', message: 'Unexpected server error' },
    requestId: req.id
  })
}
