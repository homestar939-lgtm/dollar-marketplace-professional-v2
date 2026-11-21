# Dollar Marketplace — Multi-Vendor Commerce Platform

A production-oriented foundation for a serious multi-vendor marketplace supporting C2C, C2B, B2C and B2B commerce. The project intentionally excludes an AI layer; intelligence can be added later without changing the commerce core.

## Included

- Dynamic category + attribute engine
- Multi-vendor stores
- Product variants, stock and inventory/warehouses
- Multi-store cart
- Parent Order → Seller Orders architecture
- COD/payment abstraction + payment transactions + seller payouts
- Shipping methods, shipments and tracking events
- Negotiation offers with counter-offer chains
- RFQ + supplier quotes + award flow
- Advanced catalog filters and faceted search endpoint
- Favorites and product comparison
- Reviews with product/seller/shipping/communication ratings
- Disputes, refunds and evidence metadata
- Coupons, promotions and sponsored product/ad entities
- Notifications and audit logs
- Role/permission data model
- Seller Center API
- Admin API
- Arabic/English product and category fields
- Royal black + gold Android client foundation

## Android

`apps/android` is a native Jetpack Compose application. Version alignment is deliberately pinned:

- Android Gradle Plugin 9.4.0
- Gradle 9.6
- Kotlin 2.3.21
- Compile/Target SDK 37
- Compose BOM 2026.08.00
- JDK 17

These choices follow the current Android/Compose release guidance. AGP 9.4.0 requires Gradle 9.6.0 and JDK 17; Compose BOM 2026.08.00 is the stable August 2026 BOM and targets API 37.

Android uses username/password authentication only. Configure the company's LAN API address in Server Settings (for example `http://192.168.1.50:4000/api`). The same saved address is used for API requests and SSE Live Sync. HTTP is intended only for the isolated company LAN.

## Backend development

```bash
npm install
npm run db:generate
npm run db:migrate
npm run db:seed
npm run dev
```

API health: `GET /health`

## Production configuration

The B2B API requires PostgreSQL and exposes username/password authentication only. Email verification/reset, Google OAuth, phone OTP and demo role selection are removed. Production requires a strong JWT secret and explicit browser CORS origins; exact `http://` origins are supported for the isolated LAN deployment. Android release builds use the in-app LAN server setting and a signed APK.

Before running the seed, set five different random passwords in the deployment secret store: `SEED_ADMIN_PASSWORD`, `SEED_WAREHOUSE_PASSWORD`, `SEED_COLLECTOR_PASSWORD`, `SEED_ACCOUNTANT_PASSWORD`, and `SEED_BUYER_PASSWORD`. Passwords are not stored in this repository or printed by the seed. Change them after first login.

For LAN rollout, configure PostgreSQL, backups/monitoring, the LAN IP and firewall, explicit Web CORS origin(s), the five seed passwords, and a protected Android signing key. See [`docs/RELEASE_READINESS.md`](docs/RELEASE_READINESS.md) for remaining operational/product checks.

## Build without a computer
The repository includes `.github/workflows/ci.yml`. GitHub Actions builds the web/API and produces an Android debug APK in the cloud. The Android job uses JDK 17 and Gradle 9.6 with AGP 9.4.0. The debug API URL defaults to the Android emulator address; a different URL can be supplied with `-PAPI_BASE_URL=...`.

## Dependency policy
All direct dependencies are pinned to exact versions. Compose uses the official BOM so Compose modules resolve as one compatible set. No `latest`, `+`, or floating plugin versions are used.

---

## B2B Platform (v2) — orders, invoicing, collection and the ledger

The platform was extended from a consumer marketplace into an enterprise B2B
cycle: buyer → order → warehouse preparation → invoice → accounting approval →
collection receipt → ledger → commission → reports.

**Engine:** PostgreSQL 14+ (required for `Decimal(18,4)`, enums, row-level
transactions and partial indexes). SQLite is no longer used: it cannot express
`Decimal` without losing precision.

### Running the API

```bash
docker compose up -d postgres
cd apps/api
cp .env.example .env          # set DATABASE_URL + JWT_SECRET
npm install
npx prisma migrate deploy
node prisma/seed.js
npm start                     # http://localhost:4000/health
```

The seed creates accounts with usernames `admin`, `warehouse`, `collector`, `accountant`, and `buyer`. It requires unique passwords supplied through the `SEED_*_PASSWORD` environment variables (at least 20 characters in development, 24 in production). There are no shared default passwords.

### Tests

```bash
cd apps/api
DATABASE_URL=... JWT_SECRET=... BCRYPT_ROUNDS=10 \
  npm test
```

21 scenario tests drive the real HTTP API against a real PostgreSQL database:
username/password authentication and legacy auth endpoint removal, RBAC denial,
server-side pricing, partial preparation, invoice lifecycle, ledger idempotency,
receipt approval, rejection, overpayment refusal, concurrent approval, and the
append-only guarantees enforced by database triggers.

### Financial invariants

- Money is `Decimal(18,4)` end to end; floats never touch a monetary value.
- Approval is the only path into the ledger: invoices post a DEBIT, receipts a CREDIT.
- `@@unique([referenceType, referenceId, transactionType])` makes double posting impossible.
- `FinancialTransaction` and `AuditLog` are append-only **at the database level**
  (triggers reject UPDATE/DELETE); corrections are REVERSAL entries.
- A receipt never moves a balance until an accountant approves it.
- Commission is frozen on the receipt at approval time; later rate changes do not rewrite history.
- One live invoice per order, enforced by a partial unique index.
