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

Default emulator API endpoint:
`http://10.0.2.2:4000/api`

The authenticated application is the default (`DEMO_MODE=false`). For a local demo build only, opt in with `-PDEMO_MODE=true`.

Release artifacts are intentionally blocked until you provide an HTTPS `API_BASE_URL`, `GOOGLE_WEB_CLIENT_ID`, `DEMO_MODE=false`, and a private signing key through Gradle properties: `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, and `RELEASE_KEY_PASSWORD`. Never commit signing credentials. For a physical Android device in development, use the computer's reachable LAN/API host rather than the emulator-only `10.0.2.2` address.

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

The B2B API requires PostgreSQL. The production API refuses to start with demo login enabled, token exposure enabled, a weak JWT secret, wildcard CORS, or non-HTTPS CORS origins. The web production build always uses real authentication. Android release builds require a production HTTPS API endpoint, configured Google OAuth client ID, real authentication, and signing credentials.

Do not use the development seed accounts or demo credentials in a live environment. Development seed data is never created when `NODE_ENV=production`. To create the first production administrator once, provide `SEED_ADMIN_NAME`, `SEED_ADMIN_EMAIL`, and a unique password of at least 24 characters through the deployment secret manager, then run `NODE_ENV=production npm run prisma:seed -w apps/api`. The production seed creates only that administrator and refuses to run if an administrator already exists.

Before a public release, configure and verify a real email sender for account verification/password reset, the Google OAuth client, the HTTPS API/domain, a production PostgreSQL deployment, backups/monitoring, and a protected Android signing key. See [`docs/RELEASE_READINESS.md`](docs/RELEASE_READINESS.md) for current product gaps and required business decisions.

## Demo accounts

- buyer@dollar.local / Buyer123!
- seller@dollar.local / Seller123!
- admin@dollar.local / Admin123!

These seeded accounts are for local development only. Production seeding follows the one-time administrator flow described above.

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

Seeded accounts (password shown for local demos only):

| role | email | password |
|---|---|---|
| ADMIN | admin@dollar.local | Admin@12345 |
| WAREHOUSE | warehouse@dollar.local | Warehouse@12345 |
| COLLECTOR | collector@dollar.local | Collector@12345 |
| ACCOUNTANT | accountant@dollar.local | Accountant@12345 |
| BUYER | buyer@dollar.local | Buyer@12345 |

### Tests

```bash
cd apps/api
DATABASE_URL=... JWT_SECRET=... EXPOSE_TOKENS=true BCRYPT_ROUNDS=4 \
  node --test tests/flow.test.js
```

21 scenario tests drive the real HTTP API against a real PostgreSQL database:
registration/verification, Google Sign-In (stubbed verifier), RBAC denial,
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
