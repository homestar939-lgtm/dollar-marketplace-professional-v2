export function parsePagination(query = {}) {
  const page = Math.max(1, Number.parseInt(query.page ?? '1', 10) || 1)
  const limit = Math.min(100, Math.max(1, Number.parseInt(query.limit ?? '20', 10) || 20))
  return { page, limit, skip: (page - 1) * limit, take: limit }
}

export function paginated(items, total, { page, limit }) {
  return {
    data: items,
    meta: { page, limit, total, pages: Math.max(1, Math.ceil(total / limit)) }
  }
}
