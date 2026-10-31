# تقرير التسليم — دولار ماركت بليس (B2B)

**المستودع:** `homestar939-lgtm/dollar-marketplace-professional-v2` · **الفرع:** `main`
**آخر التزامات هذه المرحلة:** `4edf0c3` (الخلفية) · `289d30c` (الويب) · `9acd964` (أندرويد + مصنع البناء) · `81db6f0` (آخر إصلاح نجح عليه البناء)

> منهج هذا التقرير: كل رقم أدناه ناتج عن **تشغيل فعلي** (اختبارات، هجرات، بناء، فحص حزمة APK).
> وما لم يُشغَّل مذكور صراحةً في القسم 9 دون تجميل.

---

## 1. الخلاصة في جدول

| المحور | الحالة | الدليل |
|---|---|---|
| مخطط Prisma | ✅ صالح | `prisma validate` → *The schema ... is valid 🚀* |
| هجرات قاعدة البيانات | ✅ 3 هجرات مطبَّقة | `prisma migrate deploy` على PostgreSQL |
| اختبارات الخلفية | ✅ **22/22** | `node --test tests/flow.test.js` — 21 سيناريو |
| بناء الخلفية وتشغيلها | ✅ | `/health` 200 · `login` 200 · تقارير 200 · مسار محمي بلا توكن 401 |
| CI الخلفية على GitHub | ✅ **8/8 نجاح** | كل push، مع خدمة Postgres حقيقية |
| بناء الويب | ✅ | `tsc --noEmit` نظيف + `vite build` (222 kB) |
| تطابق الويب مع الـ API | ✅ **20/20** نقطة نهاية موجودة | فحص بنداءات حقيقية، 0 مفقودة |
| تطبيق أندرويد | ✅ **بُني APK على GitHub Actions** | `BUILD SUCCESSFUL` · أرتيفاكت 21.6 MB |
| فحص حزمة APK | ✅ | 7 dex · توقيع v2/v3 · كل أصناف B2B داخل الحزمة |
| تشغيل APK على جهاز | ❌ **لم يُنفَّذ** | لا قناة تثبيت متاحة — انظر القسم 9 |

---

## 2. الملفات المعدَّلة

| الملف | التغيير |
|---|---|
| `apps/api/prisma/schema.prisma` | إعادة كتابة كاملة: PostgreSQL، `Decimal(18,4)`، Enums، كيانات B2B + الحفاظ على نماذج المستهلك القديمة |
| `apps/api/package.json` | سكربتات validate/migrate/test، الإصدار 2.0.0 (لا تغيير في التبعيات) |
| `apps/api/.env.example` | عقد البيئة الجديد |
| `apps/api/prisma/seed.js` | بيانات تجريبية على المخطط الجديد (مدير/مخزن/محصل/محاسب/مشترٍ + وكالة + 3 منتجات) |
| `docker-compose.yml` | خدمة `postgres:16` بصحة + `migrate deploy` قبل الإقلاع |
| `.github/workflows/ci.yml` | CI خلفية: Postgres خدمي → validate → migrate → seed → test |
| `apps/web/package.json` | `build` = `tsc --noEmit && vite build` |
| `apps/web/src/{main,App,api,types,styles}.ts(x)`, `vite-env.d.ts` | استبدال واجهة السوق القديمة بواجهة B2B كاملة |
| `apps/android/app/src/main/java/com/dollar/marketplace/MainActivity.kt` | نقطة الإقلاع تحوّلت إلى `B2bRoot()` |
| `apps/android/app/build.gradle.kts` | `GOOGLE_WEB_CLIENT_ID` + `play-services-auth:21.4.0` |
| `apps/android/build.gradle.kts` | إزالة `kotlin.android` (AGP 9 يدمج Kotlin) + إضافة `plugin.serialization` |
| `apps/android/gradle/wrapper/gradle-wrapper.properties` | **إصلاح خلل**: `gradle-9.6-bin.zip` غير موجود → `gradle-9.6.0-bin.zip` |
| `docs/API.md`, `README.md`, `package.json` (الجذر) | توثيق B2B وربط سكربتات قاعدة البيانات |

## 3. الملفات المنشأة

| المجموعة | العدد | المحتوى |
|---|---|---|
| `apps/api/src/modules/**` | 44 | 11 وحدة (auth · users · agencies · products · orders · invoices · receipts · accounting · reports · settings · notifications) |
| `apps/api/src/services/**` | 5 | ledger · invoice · receipt · commission · numbering |
| `apps/api/src/{lib,middleware,utils,config}/**` | 16 | money · jwt · tokens · tx · prisma · rbac · validation · error-handler · audit … |
| `apps/api/prisma/migrations/**` | 4 | 3 هجرات + lock |
| `apps/api/tests/flow.test.js` | 1 | 21 سيناريو مالي شامل |
| `apps/api/src/legacy/**` | 9 | المسارات القديمة منقولة (لا محذوفة) + README يشرح عدم صلاحيتها |
| `apps/web/src/{components,screens}/**` | 7 | ui + 6 شاشات أدوار |
| `apps/android/.../b2b/**` | 12 | نماذج · عميل API · حالة · مكوّنات · شاشات ترحيب · دخول · 5 لوحات |
| `.github/workflows/android-build.yml` | 1 | مصنع بناء APK على GitHub |

---

## 4. قاعدة البيانات

- **Prisma 6.19.3** (دون تغيير) · المزوّد PostgreSQL.
- 3 هجرات: المخطط الأولي (47 KB) + تسلسلات الفواتير/السندات + تسلسل الطلبات.
- ما لا يستطيع Prisma التعبير عنه، وطُبِّق في SQL:
  - فهرس فريد جزئي: **فاتورة حيّة واحدة لكل طلب**.
  - قيود `CHECK`: لا ترحيل صفري/سالب · لا تجهيز يتجاوز المطلوب · لا عمولة سالبة.
  - **مشغّلات ترفض `UPDATE`/`DELETE`** على `FinancialTransaction` و`AuditLog` — الدفتر غير قابل للتعديل على مستوى القاعدة، لا بالاتفاق.
- المال `Decimal(18,4)` في كل مكان؛ لا `Float` في أي مسار مالي.

## 5. الاختبارات — 22/22

```
$ node --test tests/flow.test.js
✔ Dollar B2B platform — end-to-end financial flow
ℹ tests 22   ℹ pass 22   ℹ fail 0
```
وذلك من **نسخة نُزّلت من GitHub** لا من مجلد العمل. تغطية: تسجيل → تحقق بريد → Google (بمدقّق محقون) → أدوار → وكالة/منتج → بحث → طلب → تجهيز جزئي → فاتورة → اعتماد → شحن → سند → اعتماد → رصيد → عمولة → تقارير → تدقيق.
اختبارات سلوكية موجّهة: رفض UPDATE على الدفتر · اعتمادان متزامنان → قيد واحد · منع الدفع الزائد · منع فرض السعر من العميل · عزل بيانات المشترين والمحصلين.

## 6. الخلفية — تشغيل حقيقي

```
GET /health                    -> 200 {"status":"ok","database":"up","version":"2.0.0"}
POST /api/auth/login           -> 200 role=ADMIN status=ACTIVE
GET /api/orders (بلا توكن)     -> 401
GET /api/reports/financial     -> 200 {"invoices":{"APPROVED":{"count":2,"total":"552.5000"}},
                                       "ledger":{"debit":"552.5000","credit":"350.0000"}}
```

## 7. الويب — بناء مُتحقَّق

- `tsc --noEmit`: نظيف بلا أخطاء أنواع (strict).
- `vite build`: `✓ built in 5.54s` → JS 222.28 kB (gzip 67.99) + CSS 5.03 kB.
- **فحص التعاقد:** استُخرجت كل نقاط النهاية التي يناديها العميل (20) وقوبلت بالخادم الحقيقي → **0 مفقودة**.
- الشاشات: دخول/تسجيل · مشتري (كتالوج، سلة، طلبات، كشف حساب) · مخزن (تجهيز، فاتورة، شحن) · محصل (أرصدة، سندات، عمولة) · محاسب (اعتماد/رفض، مطابقة، ملخص) · مدير (مستخدمون، وكالات).

## 8. أندرويد — بُني فعلاً على GitHub Actions

### 8.1 المصنع

`.github/workflows/android-build.yml`: يعمل على كل push يمسّ `apps/android/**` وعبر `workflow_dispatch`. JDK 21 + Gradle 9.6.0 + Android SDK من صورة الـ runner، ثم `:app:assembleDebug` ورفع الـ APK.

### 8.2 رحلة البناء (شفافية كاملة — 5 محاولات)

| # | النتيجة | السبب الحقيقي |
|---|---|---|
| 1 | ❌ | `android-actions/setup-android@v3` يفشل: حزمة `tools` أُزيلت من مستودع SDK |
| 2 | ❌ | `kotlin.android` لم يعد مطلوباً منذ AGP 9.0 (Kotlin مدمج) |
| 3 | ❌ | خطآن في Kotlin: `pending.intentSender` |
| 4 | ❌ | خطآن: `it.receiptNumber` (المقصود `it.receipt.receiptNumber`) + `signInIntentSender` غير موجود في `play-services-auth:21.4.0` |
| 5 | ✅ | **`BUILD SUCCESSFUL in 1m 2s`**، والـ APK مرفوع |

**دلالة هذه الجولة:** المترجم هو من نقض الكود، لا أنا. ~2,500 سطر Kotlin كُتبت بلا مترجم محلي، ورفض المترجم 4 عيوب حقيقية، ثم أقرّ البناء.

### 8.3 فحص حزمة APK (تنزيل حقيقي + تفكيك)

| الفحص | النتيجة |
|---|---|
| الحجم | 22,833,810 بايت (21.6 MB) |
| المحتوى | 210 مدخلاً · 7 ملفات dex (~71 MB غير مضغوطة) · `resources.arsc` |
| التوقيع | كتلة توقيع APK v2/v3 عند 22,813,558 (قبل الفهرس المركزي) |
| المانيفست | `com.dollar.marketplace` · `MainActivity` · `INTERNET` · `usesCleartextTraffic` |
| أصناف B2B | `B2bOnboardingKt` `B2bLoginKt` `B2bState` `B2bApi` `B2bBuyerKt` `B2bWarehouseKt` `B2bCollectorKt` `B2bAccountantKt` `B2bAdminKt` — **كلها موجودة** |
| التبعيات | `GoogleSignIn` · `io.ktor.client.HttpClient` · `kotlinx.serialization` |
| نصوص عربية | «دولار: تجارة الجملة بثقة» و«تخطٍّ إلى الدخول» و`onboarding_seen_v1` داخل الحزمة |

### 8.4 ما يفعله التطبيق

1. **شاشات ترحيب ثلاث** (عنوان تسويقي + وصف + مزايا + مؤشر تقدّم + متابعة/رجوع + تخطٍّ إلى الدخول)، تُحفظ علامة إكمالها فلا تعود تظهر.
2. **هوية بصرية**: أسود ملكي + ذهبي، و**RTL مثبّت على مستوى الجذر**.
3. **دخول**: بريد/كلمة مرور + **Google Sign-In** (الرمز يُتحقق منه في الخادم، لا نثق ببريد العميل) + تسجيل ذاتي للمشتري فقط.
4. **خمس لوحات معزولة**: المشتري · المخزن (تجهيز جزئي/فاتورة/شحن) · المحصل (سندات/عمولة) · المحاسب (اعتماد/رفض بسبب) · المدير (منح الأدوار/الوكالات).

## 9. ما لم يُنفَّذ — بلا تجميل

1. **لم يُشغَّل الـ APK على جهاز**. البيئة تملك جسر جهاز (لقطات/لمس/قراءة شاشة) لكنه **لا يملك قناة تثبيت**: `adb-shell` يرد `[DEVICE_CHANNEL_UNAVAILABLE]` (لا root/Shizuku/ADB)، وقائمة `/app/*` لا تحتوي نقطة تثبيت. فالتطبيق مبنيّ ومفحوص كحزمة، لا مُجرّب تشغيلياً.
2. **Google Sign-In غير مُفعّل حياً**: `GOOGLE_WEB_CLIENT_ID` غير مضبوط في المصنع، ولم يُجرَّب تبادل حقيقي مع Google. الزر يعرض رسالة صريحة بدل أن ينكسر.
3. **إرسال البريد غير موصول** (لا SMTP): رموز التحقق/التصفير تُعاد فقط عند `EXPOSE_TOKENS=true`.
4. **لا حدّ لمحاولات الدخول** (rate limiting / قفل حساب) — فجوة أمنية معروفة.
5. **الإشعارات داخلية فقط** (جدول `Notification`، لا Push/WebSocket).
6. **لا مسار تأكيد استلام** (حالة `DELIVERED`) بعد الشحن.
7. **نماذج المستهلك القديمة** (Offer, Rfq, Dispute, Cart…) محفوظة في المخطط بلا مسارات في نسخة B2B.

## 10. خطوات يدوية مطلوبة

1. `docker compose up -d postgres` ثم `cd apps/api && cp .env.example .env` وضبط `DATABASE_URL` + `JWT_SECRET`.
2. `npx prisma migrate deploy && node prisma/seed.js && npm start`.
3. **لتفعيل Google على أندرويد**: أضف Repository Variable باسم `GOOGLE_WEB_CLIENT_ID`، وسجّل SHA-1 لبصمة الـ debug keystore في Google Cloud Console.
4. **لتوجيه التطبيق لخادمك**: Repository Variable باسم `DOLLAR_API_BASE_URL` (الافتراضي `http://10.0.2.2:4000/api` للمحاكي).
5. ربط مزوّد بريد (Resend/SES/SMTP) لإرسال رموز التحقق فعلاً.
6. إبطال توكن GitHub المستخدم في التنفيذ بعد الانتهاء.

---

## الثوابت المالية المطبَّقة (قابلة للتحقق)

- كل مبلغ `Decimal(18,4)`؛ لا Float في أي مسار مالي.
- الاعتماد هو المدخل الوحيد للدفتر: الفاتورة DEBIT، السند CREDIT.
- `@@unique([referenceType, referenceId, transactionType])` يمنع الترحيل المزدوج (مُثبت باختبار تزامن).
- `FinancialTransaction` و`AuditLog` غير قابلين للتعديل على مستوى القاعدة (مُثبت باختبار).
- سند القبض لا يحرّك الرصيد قبل اعتماد المحاسب.
- العمولة تُجمَّد وقت الاعتماد؛ تغيير النسبة لاحقاً لا يعيد كتابة التاريخ.
- فاتورة حيّة واحدة لكل طلب (فهرس فريد جزئي).
