const BASE = (import.meta.env.VITE_API_URL as string | undefined) ?? 'http://localhost:4000/api'
const TOKEN_KEY = 'dollar_token'

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token: string) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY)
}

export class ApiError extends Error {
  status: number
  code: string
  details?: unknown
  constructor(status: number, code: string, message: string, details?: unknown) {
    super(message)
    this.status = status
    this.code = code
    this.details = details
  }
}

type Options = { method?: string; body?: unknown; query?: Record<string, string | number | undefined> }

function toQuery(query?: Options['query']): string {
  if (!query) return ''
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== '') params.set(key, String(value))
  }
  const qs = params.toString()
  return qs ? `?${qs}` : ''
}

export async function request<T>(path: string, { method = 'GET', body, query }: Options = {}): Promise<T> {
  const token = tokenStore.get()
  const response = await fetch(`${BASE}${path}${toQuery(query)}`, {
    method,
    headers: {
      ...(body ? { 'content-type': 'application/json' } : {}),
      ...(token ? { authorization: `Bearer ${token}` } : {})
    },
    body: body ? JSON.stringify(body) : undefined
  })

  const text = await response.text()
  const data = text ? JSON.parse(text) : null

  if (!response.ok) {
    const error = data?.error ?? {}
    throw new ApiError(response.status, error.code ?? 'ERROR', error.message ?? 'Request failed', error.details)
  }
  return data as T
}

export const api = {
  login: (email: string, password: string) =>
    request<{ user: import('./types').User; accessToken: string }>('/auth/login', { method: 'POST', body: { email, password } }),
  register: (name: string, email: string, password: string) =>
    request<{ user: import('./types').User; verification: { sentTo: string; token?: string } }>('/auth/register', {
      method: 'POST',
      body: { name, email, password }
    }),
  me: () => request<{ user: import('./types').User }>('/auth/me'),
  logout: () => request('/auth/logout', { method: 'POST' }),

  agencies: (query?: Record<string, string | number | undefined>) =>
    request<import('./types').Paged<import('./types').Agency>>('/agencies', { query }),
  createAgency: (body: { name: string; slug: string; description?: string }) =>
    request<{ agency: import('./types').Agency }>('/agencies', { method: 'POST', body }),

  products: (query?: Record<string, string | number | undefined>) =>
    request<import('./types').Paged<import('./types').Product>>('/products', { query }),

  orders: (query?: Record<string, string | number | undefined>) =>
    request<import('./types').Paged<import('./types').Order>>('/orders', { query }),
  createOrder: (items: { productId: string; quantity: number }[], notes?: string) =>
    request<{ order: import('./types').Order }>('/orders', { method: 'POST', body: { items, notes } }),
  prepareOrder: (orderId: string, items: { orderItemId: string; preparedQuantity: number }[]) =>
    request<{ order: import('./types').Order }>(`/orders/${orderId}/prepare`, { method: 'POST', body: { items } }),
  shipOrder: (orderId: string, body: { courierPhone: string; courierName?: string; courierChannel?: string }) =>
    request<{ order: import('./types').Order }>(`/orders/${orderId}/ship`, { method: 'POST', body }),

  invoices: (query?: Record<string, string | number | undefined>) =>
    request<import('./types').Paged<import('./types').Invoice>>('/invoices', { query }),
  createInvoice: (orderId: string) =>
    request<{ invoice: import('./types').Invoice }>('/invoices', { method: 'POST', body: { orderId } }),
  sendInvoice: (id: string) => request<{ invoice: import('./types').Invoice }>(`/invoices/${id}/send`, { method: 'POST' }),
  approveInvoice: (id: string) => request<{ invoice: import('./types').Invoice }>(`/invoices/${id}/approve`, { method: 'POST' }),
  rejectInvoice: (id: string, reason: string) =>
    request<{ invoice: import('./types').Invoice }>(`/invoices/${id}/reject`, { method: 'POST', body: { reason } }),

  receipts: (query?: Record<string, string | number | undefined>) =>
    request<import('./types').Paged<import('./types').Receipt>>('/receipts', { query }),
  createReceipt: (body: { buyerId: string; amount: string; currency: string; paymentMethod: string; notes?: string }) =>
    request<{ receipt: import('./types').Receipt }>('/receipts', { method: 'POST', body }),
  sendReceipt: (id: string) => request<{ receipt: import('./types').Receipt }>(`/receipts/${id}/send`, { method: 'POST' }),
  approveReceipt: (id: string) => request<{ receipt: import('./types').Receipt }>(`/receipts/${id}/approve`, { method: 'POST' }),
  rejectReceipt: (id: string, reason: string) =>
    request<{ receipt: import('./types').Receipt }>(`/receipts/${id}/reject`, { method: 'POST', body: { reason } }),
  myCommission: (query?: Record<string, string | number | undefined>) =>
    request<{ commission: { collected: string; commission: string; receipts: number; effectiveRate: string } }>('/receipts/commission/me', { query }),

  buyers: (query?: Record<string, string | number | undefined>) =>
    request<import('./types').Paged<import('./types').BuyerRow>>('/accounting/buyers', { query }),
  myStatement: () => request<import('./types').Statement>('/accounting/me/statement'),
  statement: (buyerId: string) => request<import('./types').Statement>(`/accounting/buyers/${buyerId}/statement`),
  matching: () => request<Record<string, unknown[]>>('/accounting/matching'),

  financialReport: () =>
    request<{ invoices: Record<string, { count: number; total: string }>; receipts: Record<string, { count: number; total: string }>; ledger: { debit: string; credit: string; balance: string }; outstanding: string; buyers: number }>(
      '/reports/financial'
    ),
  collections: (query?: Record<string, string | number | undefined>) =>
    request<{ period: { type?: string; from: string | null; to: string | null }; totals: { receipts: number; collected: string; commission: string } }>('/reports/collections', { query }),

  users: (query?: Record<string, string | number | undefined>) =>
    request<import('./types').Paged<import('./types').User>>('/users', { query }),
  createUser: (body: { name: string; username: string; email?: string; password: string; role: import('./types').Role }) =>
    request<{ user: import('./types').User }>('/v3/users', { method: 'POST', body }),
  createStaff: (body: { name: string; email: string; password: string; role: string; commissionRate?: number }) =>
    request<{ user: import('./types').User }>('/users', { method: 'POST', body }),
  settings: () => request<{ settings: Record<string, unknown> }>('/settings'),
  updateSettings: (body: Record<string, unknown>) =>
    request<{ settings: Record<string, unknown> }>('/settings', { method: 'PATCH', body }),
  notifications: () => request<import('./types').Paged<{ id: string; title: string; body: string; read: boolean; createdAt: string }>>('/notifications')
}
