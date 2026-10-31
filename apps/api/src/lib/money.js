import { Prisma } from '@prisma/client'

/**
 * Money is Decimal(18,4) end to end. Floats never touch a monetary value:
 * every number entering the system is converted through `money()` first.
 */
export const Decimal = Prisma.Decimal
export const ZERO = new Prisma.Decimal(0)

export function money(value) {
  if (value instanceof Prisma.Decimal) return value.toDecimalPlaces(4, Prisma.Decimal.ROUND_HALF_UP)
  if (typeof value === 'number') {
    if (!Number.isFinite(value)) throw new TypeError('money(): non-finite number')
    return new Prisma.Decimal(value.toFixed(4))
  }
  return new Prisma.Decimal(String(value)).toDecimalPlaces(4, Prisma.Decimal.ROUND_HALF_UP)
}

export const add = (a, b) => money(a).plus(money(b))
export const sub = (a, b) => money(a).minus(money(b))
export const mul = (a, b) => money(money(a).times(money(b)))
export const eq = (a, b) => money(a).equals(money(b))
export const gt = (a, b) => money(a).gt(money(b))
export const gte = (a, b) => money(a).gte(money(b))
export const lte = (a, b) => money(a).lte(money(b))
export const min = (a, b) => (lte(a, b) ? money(a) : money(b))

/** Serialise Decimal for JSON without ever producing a JS float. */
export const str = (v) => (v == null ? null : money(v).toFixed(4))

export function serializeMoneyFields(object, fields) {
  if (!object) return object
  const clone = { ...object }
  for (const field of fields) {
    if (clone[field] != null) clone[field] = str(clone[field])
  }
  return clone
}

/**
 * Every Decimal leaves the process as a fixed 4-decimal string. Without this,
 * JSON.stringify falls back to Decimal#toJSON, which trims trailing zeros and
 * makes 340 and 340.0000 indistinguishable in API payloads.
 */
Prisma.Decimal.prototype.toJSON = function toJSON() {
  return this.toFixed(4)
}
