export type Role = 'ADMIN' | 'WAREHOUSE' | 'COLLECTOR' | 'ACCOUNTANT' | 'BUYER' | 'SELLER' | 'SUPPORT'

export interface User {
  id: string
  name: string
  email: string
  role: Role
  status: 'PENDING_VERIFICATION' | 'ACTIVE' | 'SUSPENDED' | 'DISABLED'
  commissionRate?: string | null
}

export interface Agency {
  id: string
  name: string
  nameEn?: string | null
  slug: string
  description?: string | null
  logoUrl?: string | null
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED'
  _count?: { products: number }
}

export interface Product {
  id: string
  name: string
  nameEn?: string | null
  sku: string
  price: string
  currency: string
  unit: string
  minimumOrderQuantity: number
  availability?: 'IN_STOCK' | 'OUT_OF_STOCK' | 'UNKNOWN'
  status?: string
  description?: string
  expiryDate?: string | null
  countryOfOrigin?: string | null
  imageUrl?: string | null
  images?: string[]
  agency?: { id: string; name: string; slug?: string }
}

export interface OrderItem {
  id: string
  productName: string
  productSku: string
  unit: string
  unitPrice: string
  quantity: number
  preparedQuantity: number
  lineTotal: string
  currency: string
}

export interface Order {
  id: string
  orderNumber: string
  status: string
  currency: string
  totalAmount: string
  createdAt: string
  shippedAt?: string | null
  courierPhone?: string | null
  courierName?: string | null
  buyer?: { id: string; name: string; email: string }
  items: OrderItem[]
  invoices?: { id: string; invoiceNumber: string; status: string }[]
}

export interface Invoice {
  id: string
  invoiceNumber: string
  status: 'DRAFT' | 'SENT' | 'APPROVED' | 'REJECTED' | 'VOIDED'
  totalAmount: string
  subtotal: string
  currency: string
  createdAt: string
  rejectionReason?: string | null
  buyerId: string
  items?: { id: string; description: string; quantity: number; unitPrice: string; lineTotal: string }[]
}

export interface Receipt {
  id: string
  receiptNumber: string
  status: 'DRAFT' | 'SENT' | 'APPROVED' | 'REJECTED' | 'VOIDED'
  amount: string
  currency: string
  commissionRate?: string | null
  commissionAmount?: string | null
  paymentMethod: string
  createdAt: string
  buyerId: string
  collectorId: string
  buyer?: { id: string; name: string }
  collector?: { id: string; name: string }
}

export interface StatementLine {
  id: string
  type: 'DEBIT' | 'CREDIT'
  amount: string
  currency: string
  referenceType: string
  description?: string | null
  createdAt: string
  runningBalance: string
}

export interface Statement {
  buyer: { id: string; name: string; email: string }
  totals: { debit: string; credit: string; balance: string }
  outstanding: { invoiceId: string; invoiceNumber: string; total: string; allocated: string; remaining: string }[]
  statement: StatementLine[]
  closingBalance: string
  receipts: { id: string; receiptNumber: string; amount: string; status: string; createdAt: string }[]
}

export interface Paged<T> {
  data: T[]
  meta: { page: number; limit: number; total: number; pages: number; unread?: number }
}

export interface BuyerRow {
  id: string
  name: string
  email: string
  phone?: string | null
  debit: string
  credit: string
  balance: string
  outstanding: string
  openInvoices: number
}
