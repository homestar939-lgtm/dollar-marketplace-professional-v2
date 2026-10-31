# Legacy route layer (kept, not wired)

These files came from the original consumer-marketplace draft. They are **not**
mounted by `src/app.js`, and they were never runnable: the previous
`prisma/schema.prisma` failed `prisma validate` (it was a single-line schema that
the Prisma parser rejected), so the API could not even generate a client.

They reference `Product.titleAr/titleEn`, `Store.ownerId` and other fields that
the B2B schema replaced. They are preserved here for reference only. The B2B
modules under `src/modules/*` supersede them:

| legacy file | superseded by |
|---|---|
| `auth.js` | `modules/auth` (email verification, Google, sessions, RBAC) |
| `catalog.js` | `modules/agencies` + `modules/products` |
| `commerce.js` | `modules/orders` + `modules/invoices` + `modules/receipts` |
| `seller.js` | `modules/products` (agency-scoped) |
| `admin.js` | `modules/users` + `modules/settings` + `modules/reports` |
| `messaging.js` | unchanged domain, not yet ported to B2B |
| `legacy-auth-lib.js`, `legacy-auth-middleware.js` | `lib/jwt.js`, `middleware/authentication.js` |
