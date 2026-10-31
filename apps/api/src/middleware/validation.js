import { unprocessable } from '../lib/errors.js'

/** Zod-backed request validation; the parsed value replaces the raw input. */
export function validate(schema, source = 'body') {
  return function validationMiddleware(req, res, next) {
    const result = schema.safeParse(req[source] ?? {})
    if (!result.success) {
      const details = result.error.issues.map((issue) => ({
        path: issue.path.join('.') || source,
        message: issue.message,
        code: issue.code
      }))
      return next(unprocessable('Request validation failed', details))
    }
    // Express 5 exposes req.query through a getter, so a plain assignment
    // throws. Redefine the property on the request instance instead.
    Object.defineProperty(req, source, {
      value: result.data,
      writable: true,
      configurable: true,
      enumerable: true
    })
    next()
  }
}

export const uuidLike = (value) => typeof value === 'string' && value.length >= 8 && value.length <= 64
