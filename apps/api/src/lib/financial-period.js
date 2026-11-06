const PERIODS = new Set(['WEEKLY', 'MONTHLY', 'YEARLY'])

/** Returns UTC calendar boundaries as [start, endExclusive). Weeks begin on Monday. */
export function financialPeriod(period, date = new Date()) {
  if (!PERIODS.has(period)) throw new TypeError(`Unsupported financial period: ${period}`)
  const anchor = date instanceof Date ? new Date(date) : new Date(`${date}T00:00:00.000Z`)
  if (Number.isNaN(anchor.getTime())) throw new TypeError('Invalid financial period date')
  anchor.setUTCHours(0, 0, 0, 0)

  const start = new Date(anchor)
  let endExclusive
  if (period === 'WEEKLY') {
    const daysFromMonday = (start.getUTCDay() + 6) % 7
    start.setUTCDate(start.getUTCDate() - daysFromMonday)
    endExclusive = new Date(start)
    endExclusive.setUTCDate(endExclusive.getUTCDate() + 7)
  } else if (period === 'MONTHLY') {
    start.setUTCDate(1)
    endExclusive = new Date(start)
    endExclusive.setUTCMonth(endExclusive.getUTCMonth() + 1)
  } else {
    start.setUTCMonth(0, 1)
    endExclusive = new Date(start)
    endExclusive.setUTCFullYear(endExclusive.getUTCFullYear() + 1)
  }

  return {
    period,
    start,
    endExclusive,
    from: start.toISOString(),
    to: new Date(endExclusive.getTime() - 1).toISOString()
  }
}

export { PERIODS }
