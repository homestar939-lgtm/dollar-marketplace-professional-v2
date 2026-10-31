# Dollar Marketplace — API reference (v2)

Base URL: `http://localhost:4000` · All payloads are JSON · Money is a string
with exactly four decimals (`"340.0000"`), never a float.

Authentication: `Authorization: Bearer <accessToken>`.

## Roles

`ADMIN`, `WAREHOUSE`, `COLLECTOR`, `ACCOUNTANT`, `BUYER`, `SELLER`, `SUPPORT`.
Permissions are enforced server-side (`src/middleware/authorization.js`); hiding
a screen in the client is never the security boundary.

## Auth

| Method | Path | Notes |
|---|---|---|
| POST | `/api/auth/register` | creates a `BUYER` in `PENDING_VERIFICATION` |
| POST | `/api/auth/verify-email` | `{ token }` → `ACTIVE` |
| POST | `/api/auth/login` | blocked until the email is verified |
| POST | `/api/auth/google` | `{ idToken }`, verified against Google + audience |
| POST | `/api/auth/refresh` | rotates the refresh token |
| POST | `/api/auth/logout` | revokes the current session |
| GET | `/api/auth/me` | current profile |
| GET | `/api/auth/sessions` | active sessions |
| POST | `/api/auth/forgot-password` | always answers `{ delivered: true }` |
| POST | `/api/auth/reset-password` | revokes every session |
| POST | `/api/auth/change-password` | requires the current password |

## Catalogue

| Method | Path | Permission |
|---|---|---|
| GET | `/api/agencies?q=&status=` | `agency.read` |
| POST | `/api/agencies` | `agency.write` (ADMIN) |
| PATCH | `/api/agencies/:id` | `agency.write` |
| GET | `/api/products?q=&agencyId=&minPrice=&maxPrice=` | `product.read` |
| GET | `/api/products/:id` | `product.read` |
| POST | `/api/products` | `product.write` (WAREHOUSE/ADMIN) |
| PATCH | `/api/products/:id` | `product.write` |

Buyers only ever see `ACTIVE` products, and optional fields (expiry, country,
description, images, specs) are stripped server-side when the operator disabled them.

## Orders

| Method | Path | Permission |
|---|---|---|
| POST | `/api/orders` | `order.create` (BUYER) |
| GET | `/api/orders` | `order.read` — buyers are scoped to their own rows |
| GET | `/api/orders/:id` | owner or staff |
| POST | `/api/orders/:id/prepare` | `order.prepare` (WAREHOUSE) |
| POST | `/api/orders/:id/ship` | `order.ship` (WAREHOUSE) |
| POST | `/api/orders/:id/cancel` | buyer owner or staff, PENDING only |

`POST /api/orders` body: `{ items: [{ productId, quantity }], notes? }`.
Prices, totals and line sums are computed from the database; any price sent by the
client is ignored. `POST /:id/prepare` accepts
`{ items: [{ orderItemId, preparedQuantity }], note? }` and supports partial
quantities. `POST /:id/ship` needs `{ courierPhone, courierName?, courierChannel? }`
and stamps `shippedAt` from the server clock.

## Invoices

| Method | Path | Permission |
|---|---|---|
| POST | `/api/invoices` | `invoice.create` (WAREHOUSE) |
| PATCH | `/api/invoices/:id` | `invoice.create` — DRAFT only |
| POST | `/api/invoices/:id/send` | `invoice.send` |
| POST | `/api/invoices/:id/approve` | `invoice.approve` (ACCOUNTANT) |
| POST | `/api/invoices/:id/reject` | `invoice.reject` + `{ reason }` |
| POST | `/api/invoices/:id/void` | `invoice.void` |
| GET | `/api/invoices` / `/:id` | `invoice.read` |

Statuses: `DRAFT → SENT → APPROVED | REJECTED → VOIDED`.
Approval posts exactly one DEBIT; repeating it returns `409`.

## Receipts

| Method | Path | Permission |
|---|---|---|
| POST | `/api/receipts` | `receipt.create` (COLLECTOR) |
| POST | `/api/receipts/:id/send` | `receipt.send` |
| POST | `/api/receipts/:id/approve` | `receipt.approve` (ACCOUNTANT) |
| POST | `/api/receipts/:id/reject` | `receipt.reject` + `{ reason }` |
| POST | `/api/receipts/:id/void` | `receipt.void` |
| GET | `/api/receipts` / `/:id` | collectors see their own, buyers their own |
| GET | `/api/receipts/commission/me` | `commission.read` + `?from=&to=` |

Body: `{ buyerId, amount, currency, paymentMethod, invoiceIds?[], collectionDate?, notes?, overpaymentReason? }`.
Amounts above the outstanding balance are rejected (`422`) unless an ADMIN
supplies `overpaymentReason`.

## Accounting & reports

| Method | Path | Permission |
|---|---|---|
| GET | `/api/accounting/buyers` | `buyer.read` |
| GET | `/api/accounting/buyers/:buyerId/statement` | `ledger.read` |
| GET | `/api/accounting/me/statement` | `ledger.read.own` |
| GET | `/api/accounting/matching` | `reports.financial` |
| GET | `/api/reports/collections` | `reports.collection` |
| GET | `/api/reports/commissions` | `commission.read` |
| GET | `/api/reports/financial` | `reports.financial` |
| GET | `/api/reports/outstanding` | `reports.financial` |

## Settings & notifications

| Method | Path | Permission |
|---|---|---|
| GET | `/api/settings` | authenticated |
| PATCH | `/api/settings` | `settings.write` (ADMIN) |
| GET | `/api/notifications` | own notifications |
| POST | `/api/notifications/:id/read` | own |
| POST | `/api/notifications/read-all` | own |

## Errors

```json
{ "error": { "code": "CONFLICT", "message": "...", "details": [] }, "requestId": "..." }
```

`401 UNAUTHENTICATED` · `403 FORBIDDEN` · `404 NOT_FOUND` · `409 CONFLICT` ·
`422 UNPROCESSABLE` (validation) · `429` · `500 INTERNAL_ERROR`.
