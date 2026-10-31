import { AppError } from '../lib/errors.js'

export const SUPPORTED_CURRENCIES = ['USD', 'EUR', 'SAR', 'AED', 'EGP', 'GBP', 'TRY']

export function assertCurrency(code) {
  if (!SUPPORTED_CURRENCIES.includes(code)) {
    throw new AppError(422, 'UNSUPPORTED_CURRENCY', `Currency ${code} is not supported`)
  }
  return code
}
