# Dollar API — quick contract

Base URL: `/api`

## Auth
- POST `/auth/register`
- POST `/auth/login`

## Catalog
- GET `/catalog/categories`
- GET `/catalog/categories/:slug`
- GET `/catalog/products?q=&category=&brand=&location=&minPrice=&maxPrice=&sort=`
- GET `/catalog/products/:id`

## Seller
- GET `/seller/dashboard`
- POST `/seller/products`
- PATCH `/seller/store`

## Commerce
- GET `/commerce/cart`
- POST `/commerce/cart/items`
- DELETE `/commerce/cart/items/:id`
- POST `/commerce/orders`
- GET `/commerce/orders`
- POST `/commerce/offers`
- PATCH `/commerce/offers/:id`
- GET `/commerce/rfqs`
- POST `/commerce/rfqs`
- POST `/commerce/rfqs/:id/quotes`
- POST `/commerce/disputes`
- POST `/commerce/favorites/:productId`

## Messaging
- GET `/messaging/conversations`
- POST `/messaging/conversations`
- GET `/messaging/conversations/:id/messages`
- POST `/messaging/conversations/:id/messages`

## Admin
- GET `/admin/dashboard`
- POST `/admin/categories`
- POST `/admin/attributes`
- POST `/admin/category-attributes`
- PATCH `/admin/products/:id/status`
- GET `/admin/disputes`
- PATCH `/admin/disputes/:id`

## Verification
- The repository has a GitHub Actions workflow at `.github/workflows/ci.yml`.
- JavaScript/API uses Node 22 in CI.
- Android uses JDK 17, AGP 9.4.0 and Gradle 9.6.
- The CI workflow runs Prisma generation/validation, JavaScript syntax checks, the web production build, and an Android debug APK build.
