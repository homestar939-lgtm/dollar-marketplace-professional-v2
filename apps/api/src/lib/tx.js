import { prisma } from './prisma.js'

const RETRYABLE = new Set(['40001', 'P2034'])

/**
 * SERIALIZABLE transactions with a bounded retry. Financial writes must never
 * interleave, so callers use this instead of the default isolation level.
 */
export async function withSerializable(fn, { retries = 3, client = prisma } = {}) {
  let lastError
  for (let attempt = 0; attempt <= retries; attempt += 1) {
    try {
      return await client.$transaction(fn, { isolationLevel: 'Serializable', timeout: 15000 })
    } catch (error) {
      lastError = error
      const code = error?.code || error?.meta?.code
      const message = String(error?.message || '')
      const transient =
        RETRYABLE.has(code) ||
        message.includes('40001') ||
        message.includes('could not serialize') ||
        message.includes('deadlock detected')
      if (!transient || attempt === retries) throw error
      await new Promise((resolve) => setTimeout(resolve, 25 * (attempt + 1)))
    }
  }
  throw lastError
}
