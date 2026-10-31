# تقرير التسليم — دولار ماركت بليس (المرحلة 1: الخلفية المالية B2B)

**الالتزام (Commit):** [`4edf0c3`](https://github.com/homestar939-lgtm/dollar-marketplace-professional-v2/commit/4edf0c3c14095455e2278444ac927209c2476a9f) · **الفرع:** `main`
**النطاق المنفَّذ:** مخطط قاعدة البيانات + أمن المصادقة والصلاحيات + دورة الفاتورة/سند القبض/الدفتر المالي + الاختبارات.
**غير المنفَّذ في هذه المرحلة (معلن صراحةً):** واجهات Android وWeb لواجهات B2B الجديدة — انظر القسم 7.

---

## 1. الملفات المعدَّلة (9)

| الملف | التغيير |
|---|---|
| `apps/api/prisma/schema.prisma` | **إعادة كتابة كاملة**. المخطط القديم لم يكن يمرّ من `prisma validate` إطلاقًا (كل النماذج في 73 سطرًا مضغوطة، والمحلّل يرفضها). الآن: PostgreSQL، `Decimal(18,4)`، Enums، وكيانات B2B الجديدة مع **الحفاظ على كل نماذج المستهلك القديمة** (Store, Cart, Offer, Rfq, Dispute…) وتحويل أموالها إلى Decimal |
| `apps/api/package.json` | سكربتات `prisma:validate` و`prisma:migrate` (deploy) و`test`، والإصدار 2.0.0. **لم تتغيّر أي تبعية** |
| `apps/api/.env.example` | عقد البيئة الجديد (DATABASE_URL لـ Postgres، JWT، TTLS، Google Client ID، EXPOSE_TOKENS) |
| `apps/api/prisma/seed.js` | بيانات تجريبية على المخطط الجديد: مدير، مخزن، محصل بنسبة 2.5%، محاسب، مشتري، وكالة، 3 منتجات، إعدادات مؤسسة |
| `docker-compose.yml` | إضافة خدمة `postgres:16-alpine` بصحة (healthcheck)، وربط الـ API بها مع `migrate deploy` قبل الإقلاع |
| `.github/workflows/ci.yml` | CI حقيقي: Postgres خدمي → `npm ci` → `validate` → `migrate deploy` → `seed` → الاختبارات |
| `docs/API.md` | مرجع كامل للمسارات والصلاحيات والرموز (كان 1.4KB وصفيًا) |
| `README.md` | قسم B2B (v2): التشغيل، الحسابات المزروعة، الاختبارات، الثوابت المالية |
| `package.json` (الجذر) | ربط `db:generate/db:migrate/db:seed` بحزمة `apps/api` |

## 2. الملفات المنشأة (84 ملفًا · 273 KB)

| المجموعة | العدد | المحتوى |
|---|---|---|
| `apps/api/src/modules/**` | 44 | 11 وحدة، كل واحدة `routes / controller / service / validation`: auth · users · agencies · products · orders · invoices · receipts · accounting · reports · settings · notifications |
| `apps/api/src/services/**` | 5 | `financial-ledger` · `invoice-service` · `receipt-service` · `commission-service` · `numbering` |
| `apps/api/src/lib/**` | 7 | `money` (Decimal حصريًا) · `jwt` · `tokens` · `errors` · `tx` (SERIALIZABLE + إعادة محاولة) · `prisma` · `async-handler` |
| `apps/api/src/middleware/**` | 5 | `authentication` · `authorization` (مصفوفة صلاحيات) · `validation` (zod) · `error-handler` · `request-context` |
| `apps/api/src/utils/**` + `config/` | 4 | `audit` · `pagination` · `currency` · `config/env` |
| `apps/api/prisma/migrations/**` | 4 | 3 هجرات + `migration_lock.toml` |
| `apps/api/tests/flow.test.js` | 1 | 21 سيناريو شامل |
| `apps/api/src/app.js`, `server.js` | 2 | تجميع التطبيق، إقلاع، إغلاق آمن |
| `apps/api/src/legacy/**` | 9 | المسارات القديمة **منقولة لا محذوفة** + README يشرح سبب عدم صلاحيتها |
| ملفات الجذر | 3 | `docs/API.md` · `.github/workflows/ci.yml` · تحديثات أعلاه |

**المحذوف (8 ملفات):** المسارات القديمة من مواقعها (`src/routes/*.js`، `src/lib/auth.js`، `src/middleware/auth.js`) — نُقلت إلى `src/legacy/` بنفس المحتوى. المبرر المثبت: المخطط القديم لم يكن يمرّ من `validate`، فلم تكن هذه الملفات قابلة للتشغيل أصلًا، وهي تشير إلى حقول أزالها المخطط الجديد (`Product.titleAr`…).

## 3. إصدار Prisma

`prisma 6.19.3` و`@prisma/client 6.19.3` — **دون تغيير** (نفس ما كان في المشروع).

## 4. نتيجة Prisma Validate

```
$ npx prisma validate
Prisma schema loaded from prisma/schema.prisma
The schema at prisma/schema.prisma is valid 🚀
```
ونُفِّذت الهجرات فعليًا:
```
$ npx prisma migrate deploy
3 migrations found in prisma/migrations
All migrations have been successfully applied.
```

## 5. نتيجة الاختبارات

```
$ node --test tests/flow.test.js
✔ Dollar B2B platform — end-to-end financial flow
ℹ tests 22   ℹ pass 22   ℹ fail 0   ℹ duration_ms ~16000
```
21 سيناريو، كلها عبر HTTP حقيقي وقاعدة بيانات حقيقية — **ونُفِّذت أيضًا من نسخة نظيفة تم تنزيلها من GitHub** (لا من مجلد العمل) فنجحت 22/22.

التغطية: التسجيل/التحقق من البريد · الدخول المحجوب قبل التحقق · Google Sign-In (بمدقّق محقون) · نسيت/تغيير كلمة المرور · رفض RBAC · إنشاء وكالة/منتج · بحث المشتري بالاسم وSKU والوكالة · **منع العميل من فرض السعر** · تجهيز جزئي ثم كامل · منع الشحن قبل إرسال الفاتورة · إنشاء/تعديل/إرسال الفاتورة · الاعتماد مع منع التكرار · ثبات الفاتورة المعتمدة · سند قبض لا يمسّ الرصيد قبل الاعتماد · الرفض بلا أثر · منع الدفع الزائد · تصفية المتبقي · عزل بيانات المشترين والمحصلين · **الدفتر غير قابل للتعديل على مستوى القاعدة** · **اعتمادان متزامنان → قيد واحد فقط** · التقارير وسجل التدقيق والمطابقة.

## 6. نتيجة بناء/تشغيل الـ Backend

```
[health]  GET /health            -> 200 {"status":"ok","database":"up","version":"2.0.0"}
[login]   POST /api/auth/login   -> 200 role=ADMIN status=ACTIVE
[guard]   GET /api/orders (بلا توكن) -> 401
[report]  GET /api/reports/financial -> 200
          {"invoices":{"APPROVED":{"count":2,"total":"552.5000"}},
           "ledger":{"debit":"552.5000","credit":"350.0000"}}
```
التشغيل تمّ من نسخة GitHub النظيفة، على **PostgreSQL 18.6 حقيقي** عبر بروتوكول Postgres السلكي. البيئة لا تسمح بتشغيل خدمة Postgres الأصلية (نواة الحاوية تمنع SysV shared memory)، فاستُخدم محرّك PostgreSQL نفسه في بناء WASM (`PGlite`) مع خادم سلكي — نفس المحرّك، ونفس الهجرات، ونفس القيود والمشغّلات. لم يُستخدم أي بديل وهمي.

## 7. Android و Web — **لم يُبنَ، ولم يُعدَّل**

| البند | الحالة | السبب |
|---|---|---|
| بناء Android | **لم يُنفَّذ** | لا يوجد JDK ولا Android SDK ولا Gradle في هذه البيئة (`java: command not found`). لا يمكنني الادّعاء بنجاح بناء لم أشغّله |
| بناء Web | **لم يُنفَّذ** | `apps/web` لم يُعدَّل في هذه المرحلة، فلم يكن هناك ما يُبنى. `npm run build -w apps/web` يحتاج تبعيات الواجهة غير المثبتة هنا |
| تعديل كود Android/Web | **لم يحدث** | هذه مرحلة خلفية + قاعدة بيانات. الملفات القائمة (Compose: `Screens.kt` 49KB، `Detail.kt` 45KB…) بقيت كما هي بلا مساس |

واجهات B2B (شاشات الترحيب الثلاث، لوحات المخزن/المحصل/المحاسب، RTL) هي **المرحلة 2**، ولم أُنشئ لها ملفات فارغة ولا شاشات شكلية.

## 8. أخطاء/فجوات متبقية (معلنة بصراحة)

1. **إرسال البريد غير موصول**: التحقق وإعادة التعيين يولّدان رموزًا صحيحة، لكن لا يوجد مزوّد SMTP. الرموز تُعاد في الاستجابة فقط عند `EXPOSE_TOKENS=true` (تطوير/اختبار). في الإنتاج يلزم ربط مزوّد أو خدمة بريد.
2. **Google Sign-In**: التحقق الحقيقي مبني على نقطة `oauth2.googleapis.com/tokeninfo` + فحص `aud` مقابل `GOOGLE_CLIENT_ID`. الاختبار استخدم مدقّقًا محقونًا (`app.locals.googleVerifier`)، ولم يُنفَّذ تبادل حقيقي مع Google لعدم وجود `idToken` فعلي.
3. **لا حدّ لمحاولات الدخول** (rate limiting / قفل حساب) — فجوة أمنية معروفة، لم تُنفَّذ.
4. **الإشعارات داخلية فقط** (جدول `Notification`)؛ لا Push ولا WebSocket.
5. **حالة `DELIVERED`** لا يوجد لها مسار تأكيد استلام بعد الشحن.
6. **إعدادات إظهار الحقول الاختيارية** تُضبط لكل منتج (وأيضًا كقيم افتراضية في `SystemSettings.productDisplaySettings`)؛ الأولوية لحقول المنتج.
7. **نماذج المستهلك القديمة** (Offer, Rfq, Dispute, Cart…) محفوظة في المخطط لكن بلا مسارات في نسخة B2B.
8. **`scripts/verify-source.mjs`** القديم لم يُشغَّل ولم يُحدَّث.

## 9. خطوات يدوية ضرورية

1. `docker compose up -d postgres` ثم `cd apps/api && cp .env.example .env` وضبط `DATABASE_URL` و`JWT_SECRET`.
2. `npx prisma migrate deploy && node prisma/seed.js && npm start`.
3. ضبط `GOOGLE_CLIENT_ID` في البيئة، وفي تطبيق Android استخدام **Credential Manager / Google Identity** لإصدار `idToken` ثم إرساله إلى `/api/auth/google`.
4. ربط مزوّد بريد (Resend/SES/SMTP) لإرسال رموز التحقق فعلًا.
5. إبطال توكن GitHub المستخدم في التنفيذ (انتهى غرضه).

---

## الثوابت المالية المطبَّقة (قابلة للتحقق)

- كل مبلغ `Decimal(18,4)`؛ لا Float في أي مسار مالي.
- الاعتماد هو المدخل الوحيد للدفتر: الفاتورة DEBIT، السند CREDIT.
- `@@unique([referenceType, referenceId, transactionType])` يمنع الترحيل المزدوج (أُثبت باختبار تزامن).
- `FinancialTransaction` و`AuditLog` **غير قابلين للتعديل على مستوى القاعدة** (مشغّلات ترفض UPDATE/DELETE) — أُثبت باختبار.
- سند القبض لا يحرّك الرصيد قبل اعتماد المحاسب.
- العمولة تُجمَّد وقت الاعتماد؛ تغيير النسبة لاحقًا لا يعيد كتابة التاريخ.
- فاتورة حيّة واحدة لكل طلب عبر فهرس فريد جزئي.
