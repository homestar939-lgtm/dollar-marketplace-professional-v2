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

For a physical Android device, set `BuildConfig.API_BASE_URL` to the LAN address of the API server.

## Backend development

```bash
npm install
npm run db:generate
npm run db:migrate
npm run db:seed
npm run dev
```

API health: `GET /health`

## Production architecture target

Android App / Web → API → PostgreSQL → Redis → Search Index → Object Storage → Queue/Workers → Payment/Shipping adapters.

The included development database remains SQLite for frictionless local setup. The schema is intentionally relational and is ready to be migrated to PostgreSQL before production deployment.

## Demo accounts

- buyer@dollar.local / Buyer123!
- seller@dollar.local / Seller123!
- admin@dollar.local / Admin123!

Change all credentials and JWT secrets before deployment.

## Build without a computer
The repository includes `.github/workflows/ci.yml`. GitHub Actions builds the web/API and produces an Android debug APK in the cloud. The Android job uses JDK 17 and Gradle 9.6 with AGP 9.4.0. The debug API URL defaults to the Android emulator address; a different URL can be supplied with `-PAPI_BASE_URL=...`.

## Dependency policy
All direct dependencies are pinned to exact versions. Compose uses the official BOM so Compose modules resolve as one compatible set. No `latest`, `+`, or floating plugin versions are used.
