export class AppError extends Error {
  constructor(status, code, message, details) {
    super(message)
    this.name = 'AppError'
    this.status = status
    this.code = code
    this.details = details
    this.expected = true
  }
}

export const badRequest = (m, d) => new AppError(400, 'BAD_REQUEST', m, d)
export const unauthenticated = (m = 'Authentication required') => new AppError(401, 'UNAUTHENTICATED', m)
export const forbidden = (m = 'You do not have permission to perform this action') => new AppError(403, 'FORBIDDEN', m)
export const notFound = (m = 'Resource not found') => new AppError(404, 'NOT_FOUND', m)
export const conflict = (m, d) => new AppError(409, 'CONFLICT', m, d)
export const unprocessable = (m, d) => new AppError(422, 'UNPROCESSABLE', m, d)
export const tooMany = (m = 'Too many attempts') => new AppError(429, 'TOO_MANY_REQUESTS', m)
