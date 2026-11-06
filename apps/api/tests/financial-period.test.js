import test from 'node:test'
import assert from 'node:assert/strict'
import { financialPeriod } from '../src/lib/financial-period.js'

test('weekly financial period starts Monday and ends Sunday in UTC', () => {
  const period = financialPeriod('WEEKLY', '2026-11-06')
  assert.equal(period.from, '2026-11-02T00:00:00.000Z')
  assert.equal(period.to, '2026-11-08T23:59:59.999Z')
  assert.equal(period.endExclusive.toISOString(), '2026-11-09T00:00:00.000Z')
})

test('monthly period handles variable month length and leap-year February', () => {
  const period = financialPeriod('MONTHLY', '2024-02-29')
  assert.equal(period.from, '2024-02-01T00:00:00.000Z')
  assert.equal(period.to, '2024-02-29T23:59:59.999Z')
})

test('yearly period returns the complete UTC calendar year', () => {
  const period = financialPeriod('YEARLY', '2026-07-15')
  assert.equal(period.from, '2026-01-01T00:00:00.000Z')
  assert.equal(period.to, '2026-12-31T23:59:59.999Z')
})

test('unsupported periods and invalid dates are rejected', () => {
  assert.throws(() => financialPeriod('DAILY', '2026-11-06'), /Unsupported/)
  assert.throws(() => financialPeriod('MONTHLY', 'not-a-date'), /Invalid/)
})
