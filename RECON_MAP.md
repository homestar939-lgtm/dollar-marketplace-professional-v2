# RECON_MAP — مرحلة الاستخبارات السحابية

> **المشروع:** دولار (Dollar Marketplace) — منصة تجارة متعددة البائعين
> **المرجع الاستخباراتي:** [mercurjs/mercur](https://github.com/mercurjs/mercur) @ `main`
> **المنهجية:** الاعتماد السحابي المطلق — قراءة وكتابة مباشرة عبر GitHub REST API (`curl`)، بلا `git clone`، بلا لمس ملفات الجهاز أو أي طرفية محلية.
> **الحالة:** مرحلة 1 (Recon) — مكتملة.

---

## 0. ملخص تنفيذي (لمن يقرأ 30 ثانية)

Mercur ليس منصة "مكتوبة من الصفر"؛ هو **طبقة سوق (marketplace layer) فوق Medusa.js**. جوهره أن Medusa يوفّر التجارة الأساسية (منتجات، مخزون، طلبات، دفع، شحن، ضرائب)، وMercur يضيف فوقها: البائع، العرض (offer)، العمولة، المدفوعات للبائع، وتقسيم الطلب إلى طلبات فرعية. هذا يعني أن **90% من قيمة Mercur هي في العقود المعمارية للنمط (modules + links + workflows)، لا في كود أعمال فريد**.

**الاستنتاج الاستراتيجي الأهم لدولار:** لا يوجد سبب لاعتماد Medusa كمحرك. دولار يملك بالفعل نطاقًا أعماليًا أوسع (RFQ، تفاوض، نزاعات، حوكمة) على Prisma. القيمة الحقيقية التي نلتقطها من Mercur هي **نمط الفصل بين الوحدات + نمط التركيب (links/workflows) + نمط توليد عميل API مُنمّط + RBAC محمول**.

---

## 1. كيف تمت القراءة (شفافية الأدلة)

كل رقم أو جدول في هذا التقرير مستخرَج من استجابات API فعلية، لا من الذاكرة:

| # | الطلب | النتيجة |
|---|-------|---------|
| 1 | `GET /repos/mercurjs/mercur/contents/` | 35 عنصرًا في الجذر |
| 2 | `GET raw/.../package.json` | 1567 بايت — monorepo، Bun، Turbo |
| 3 | `GET /git/trees/main?recursive=1` | 10,430 عقدة، 7,200 blob، `truncated: false` |
| 4 | `GET raw/.../README.md` | 9261 بايت |
| 5 | `GET raw/.../docs/ARCHITECTURE.md` | 203 سطرًا — الوثيقة المرجعية للطبقات |
| 6 | `GET raw/.../{apps,packages}/*/package.json` (×11) | جدول التبعيات الكامل |
| 7 | `GET raw/.../registry.json` | مخطط سجل الـ blocks |
| 8 | `GET /repos/homestar939-lgtm/dollar-marketplace-professional-v2` | مستودع الهدف: عام، `main`، 103 KB |
| 9 | `GET contents/` لمستودع دولار | 10 عناصر جذر + `apps/{android,api,web}` |

> **قيود يجب تسجيلها:** القراءة تمت بهوية مجهولة (بلا توكن) على API العام — سقف 60 طلبًا/ساعة لكل IP. الكتابة (PUT) تتطلب توكنًا مصرَّحًا به، لذلك لم يُكتب هذا الملف عبر الـ API إلا بعد توفير التوكن.

---

## 2. هيكل مستودع Mercur — الأرقام

```
mercur/                              Turborepo + Bun 1.3.8 · Node >=18 (README يطلب >=20)
├─ apps/                    953 ملفًا
│   ├─ api/                 41   ← Medusa server + withMercur()  (رفيع جدًا: المنطق في core)
│   ├─ storefront/          367  ← Next 16 + React 19 (323 ملف ts/tsx)
│   ├─ vendor/              28   ← لوحة البائع: React 18 + Vite 8
│   ├─ admin-test/          18   ← لوحة المشغّل: React 18 + Vite 8
│   └─ docs/                499  ← موقع توثيق
├─ packages/                5746 ملفًا
│   ├─ core/                1158 ← @mercurjs/core : قلب السوق (Medusa plugin)
│   ├─ admin/               1952 ← صفحات لوحة المشغّل
│   ├─ vendor/              1489 ← صفحات لوحة البائع
│   ├─ dashboard-shared/    345  ← مكوّنات/هوكس مشتركة بين اللوحتين
│   ├─ registry/            292  ← سجل الـ blocks (نمط shadcn)
│   ├─ dashboard-sdk/       35   ← Vite plugin: توجيه قائم على الملفات + blocks
│   ├─ types/               70   ← أنواع مشتركة (يعتمد عليه الجميع)
│   ├─ cli/                 68   ← mercurjs: init/add/search/view/diff/codegen
│   ├─ client/              12   ← Proxy يمنحك API مُنمّطًا
│   ├─ create-mercur-app/   21   ← bootstrap المشاريع
│   ├─ docs/                5
│   └─ plugins/
│       ├─ stripe-connector/   223 + 76 ← Stripe Connect (payouts)
│       └─ magento2-connector/  76     ← هجرة/مزامنة كتالوج
├─ templates/               101  ← basic / plugin / registry (تُستخدم بـ CLI)
├─ integration-tests/       146
├─ e2e-tests/                46
├─ docs/                      5  ← ARCHITECTURE, PERFORMANCE, PRODUCT, UI-ARCHITECTURE, UI-ICONS
└─ .github/                  13  ← lint, integration-tests, release, pages, dependabot, triage
```

**ملاحظة توازن:** 63% من حجم المستودع (admin+vendor+dashboard-shared) عبارة عن **واجهات**، والفلسفة الحقيقية في 1158 ملفًا فقط من `core`. هذا درس مباشر لدولار.

---

## 3. الطبقات — كما توثّقها الوثيقة المرجعية

```
Storefront / Mobile / أي عميل
        │  @mercurjs/client  (.query / .mutate / .delete — Proxy مُنمّط)
        ▼
apps/api   (Medusa server + withMercur())
   Routes: /admin/*   /vendor/*   /store/*
        │  HTTP → workflows → modules
        ▼
@mercurjs/core  (Medusa plugin)
   modules   · links   · workflows   · subscribers   · api hooks   · RBAC
        ▼
PostgreSQL + Redis          + Stripe Connect (payouts)
```

**القواعد الحاكمة التي يستحق نقلها حرفيًا:**

1. **الوحدات لا تستدعي بعضها مباشرة أبدًا.** التواصل فقط عبر `links` (علاقات مُعرَّفة خارجيًا) و`workflows` (تنسيق + تعويض تلقائي عند الفشل). هذا هو الفرق بين monolith فوضوي وبنية قابلة للتوسّع.
2. **Workflows تعويضية (compensating).** كل step يعيد `StepResponse`؛ فشل خطوة يُرجع النظام. في سوق متعدد البائعين — حيث عملية واحدة تلمس بائعًا ودفعة وعمولة ومخزونًا — هذا ليس ترفًا.
3. **API رفيع، منطق في الـ domain.** `apps/api` = 41 ملفًا فقط. لا منطق أعمال فيه، فقط تركيب.
4. **عميل واحد مُولَّد.** لا `fetch` عشوائي في الواجهات؛ `codegen` يبني خريطة الطرق، والـ Proxy يشتق الـ types منها. هذا يقتل فئة كاملة من أخطاء التكامل.
5. **RBAC مُسجَّل تلقائيًا** عند `withMercur()` — عزل البائع يعمل out-of-the-box.
6. **Blocks registry** (شبيه بـ shadcn): المستخدم يضيف ميزات بالنسخ إلى مستوده لا بالاعتماد على نسخة مثبّتة — يجعل التخصيص ممكنًا بلا fork.

**الأوزان داخل `core`:**

| المكوّن | الملفات | الدلالة |
|---|---|---|
| `modules/` | 168 | 14 وحدة أعمال |
| `workflows/` | — | 22+ مجموعة (بائع، عمولة، مخرجات، سلة، تقسيم طلب، عرض، تغيير منتج) |
| `links/` | 49 | أكثر من 40 علاقة (product-seller، offer-product، offer-variant، order-group-order …) |
| `subscribers/` | 27 | تفاعل مع أحداث الدفع/التقسيم/الويب هوك |
| `api/` | — | `admin/*`, `vendor/*`, `store/*`, hooks, middlewares, validators, query-config |

**الوحدات الـ14 في `packages/core/src/modules/`:**
`seller` · `commission` · `offer` · `payout` · `product-attribute` · `product-edit` · `custom-fields` · `media` · `promotion-cost` · `review` · `admin-ui` · `vendor-ui` · `codegen` · `telemetry`

**الطبقات الفرعية:** `permissions/definitions` (RBAC) · `patches/` (ترقيع سلوك Medusa بلا fork — نمط مهم) · `feature-flags/` · `providers/` · `migration-scripts/`.

---

## 4. أهم 10 حزم مستخدمة

مرتّبة بحسب **الأثر المعماري** على دولار، لا بحسب عدد التكرار.

| # | الحزمة | الإصدار المثبّت | الدور | لماذا تهمّنا |
|---|--------|------------------|-------|--------------|
| 1 | `@medusajs/framework` | `2.22.0` | نواة Medusa (DI، وحدات، workflows، HTTP) | النموذج المرجعي لبنية قابلة للتركيب. مثبّت في `overrides` بالكامل |
| 2 | `@medusajs/medusa` | `2.22.0` | حزمة التجارة الجاهزة | يحدّد حجم ما "لا نكتبه" — وهذا هو مقياس الجدوى |
| 3 | `@medusajs/core-flows` | `2.22.0` | workflows جاهزة قابلة للتمديد/التجاوز | الإلهام المباشر لطبقة `workflows` في دولار |
| 4 | `zod` | `4.6.5` | تحقق/مخططات | **الأكثر تكرارًا: 8 حزم**. مثبّت في `overrides` — عقد واحد للتحقق في كل الطبقات |
| 5 | `@mercurjs/client` | `2.3.9-canary.7` | Proxy API مُنمّط | الحل لفوضى `fetch` في `apps/web` |
| 6 | `@tanstack/react-query` | `5.64.2` | حالة الخادم | 6 حزم. الطبقة الوحيدة المسموحة لجلب البيانات |
| 7 | `@medusajs/ui` + `@medusajs/icons` | `4.2.3` / `2.22.0` | نظام تصميم اللوحات | 6 حزم لكل منهما — يمنع تشعّب المكوّنات |
| 8 | `react` + `react-dom` | `18.3.1` (اللوحات) / `19.2.1` (storefront) | الواجهة | **تحذير: إصداران في مستودع واحد** — ثمنه تعقيد حقيقي |
| 9 | `next` | `16.4.2` | storefront (SSR/ISR) | متوافق مع `apps/web` في دولار |
| 10 | `@stripe/react-stripe-js` + `stripe-connector` plugin | `3.7.0` / plugin 223 ملفًا | Stripe Connect (payouts) | النموذج المرجعي لدعم البائع — يحلّ محلّ COD/التحصيل اليدوي تدريجيًا |

**الحزم الأكثر تكرارًا فعليًا** (من تجميع 11 `package.json`): `zod` (8) · `@medusajs/icons` (6) · `@medusajs/ui` (6) · `@mercurjs/client` (6) · `react-i18next` (6) · `@tanstack/react-query` (6) · `react-hook-form` (6) · `@hookform/resolvers` (6) · `@mercurjs/dashboard-shared` (5) · `i18next` (5).

**أدوات البناء:** `bun@1.3.8` (packageManager) · `turbo@2.7.4` · `typescript@5.9.3` (مثبّت بدقة، لا `^`) · `oxlint@1.64.0` + `eslint@10` · `prettier@3.7.4`.

---

## 5. تحليل الفجوة: Mercur ↔ دولار

| البُعد | Mercur | دولار (الحالي) | القرار |
|--------|--------|-----------------|--------|
| محرك التجارة | Medusa 2.22 (JS، modules، workflows) | Node + Prisma (مكتوب داخليًا) | **نبقي Prisma** — الاستبدال يعني إعادة كتابة كل النطاق |
| تعدد البائعين | seller + offer (عرض على منتج رئيسي) | stores + variants + parent/seller orders | دولار **أعمق** في تقسيم الطلب |
| العمولات والمدفوعات | commission + payout + Stripe Connect | payment abstraction + COD + payouts | **نستعير نموذج payout state machine** |
| التفاوض | غير موجود | negotiation + counter-offers | ميزة تفاضلية لدولار — لا تُهدَر |
| RFQ | غير موجود | RFQ + supplier quotes + award | ميزة تفاضلية — لا تُهدَر |
| النزاعات/الاسترجاع | غير موجود | disputes + evidence | ميزة تفاضلية — لا تُهدَر |
| عزل الوحدات | modules + links + workflows | غير مفروض | ✅ **أعلى قيمة مستخرجة** |
| عقد الـ API | codegen + Proxy مُنمّط | غير موجود | ✅ **ثاني أعلى قيمة** |
| RBAC | permissions/definitions | role/permission data model | نستعير الهيكل |
| العميل | Next (web) + Kotlin | Next (web) + Jetpack Compose | متكافئ — لا نقل |
| الأصول البصرية | Medusa UI | Royal black + gold | نبقي هوية دولار |

**خلاصة:** Mercur منافس مباشر لـ Mirakl لا لدولار. تبنّي محركه = تخلٍّ عن RFQ والتفاوض والنزاعات. لذا: **نستعير الأنماط، لا المنصة**.

**ما نرفضه صراحةً:** `canary` versions في الإنتاج · إصدارَي React في مستودع واحد · الاعتماد على Bun كـ runtime إلزامي · 1952 ملفًا من صفحات لوحة إدارية جاهزة قبل التحقق من صحة الـ domain.

---

## 6. الخطوة التالية المقترحة (أولوية مرتّبة)

### ⭐ الخطوة 1 — `docs/ADR-001-core-engine.md` : تثبيت القرار قبل كتابة أي كود

وثيقة قرار معماري (ADR) من صفحة واحدة تُنهي النقاش:

- **المُقرَّر:** يبقى محرك دولار على **Prisma + Node**، ولا يُعتمد Medusa/Mercur كمحرك.
- **المُلتقَط من Mercur:** (أ) عزل الوحدات عبر `links` صريحة، (ب) `workflows` تعويضية، (ج) `codegen` + عميل مُنمّط، (د) `permissions/definitions` لـ RBAC، (هـ) registry للـ blocks.
- **المرفوض:** استبدال طبقة البيانات، Bun كإلزام، canary.
- **المعيار الذي يُعيد فتح القرار:** إن تجاوز عدد كتابات عبر الوحدات حدًّا معينًا، أو إن احتجنا Stripe Connect خلال 60 يومًا.

### ⭐ الخطوة 2 — `docs/MERCUR_PATTERNS.md`: 6 أنماط قابلة للنقل، كل نمط بـ "قبل/بعد" على كود دولار

لكل نمط: المشكلة في دولار اليوم → الشكل عند Mercur (بمسار الملف الفعلي) → الشكل المطلوب في دولار → تعريف "منتهٍ" قابل للاختبار.

### ⭐ الخطوة 3 — تصليب `apps/api` قبل أي ميزة جديدة

1. `apps/api/src/modules/` بوحدات صريحة: `vendor` · `offer` · `commission` · `payout` · `order-group` · `negotiation` · `rfq` · `dispute`.
2. `apps/api/src/links/` — علاقات صريحة (offer↔product، offer↔variant، seller-order↔payout…).
3. قاعدة مفروضة آليًا (سكربت `verify` موجود في المستودع بالفعل — يُوسَّع): **لا استيراد مباشر بين وحدة ووحدة**.
4. `apps/api/src/workflows/` بخطوات تعويضية لثلاث عمليات حرجة فقط: إنشاء طلب متعدد البائعين · توزيع العمولة · تحويل payout.

### الخطوة 4 — بوابة CI حقيقية

`.github/workflows/ci.yml`: `lint` + `typecheck` + `prisma migrate deploy` على Postgres خدمي + `seed` + اختبارات عقد على مسارات `/health` والمسارات الحرجة. (Mercur يملك `lint.yml` + `integration-tests.yml` — نفس الفكرة، مقياس أصغر.)

### الخطوة 5 — (لاحقًا، بعد استقرار الـ domain) عميل مُنمّط + registry

`codegen` من مسارات `apps/api` → `packages/client` → استهلاكه في `apps/web`. ثم registry للـ blocks. **لا تبدأ بهذه الخطوة**؛ بلا domain مستقر يولّد الـ codegen دَينًا تقنيًا بدل أن يمنعه.

---

## 7. المخاطر والتحذيرات

| الخطر | الوصف | التخفيف |
|-------|-------|---------|
| إغراء الترحيل | الانتقال إلى Medusa يبدو "أسرع" لأن 1158 ملفًا جاهزة | ADR-001 يُغلق الباب صراحةً |
| `canary` dependencies | `@mercurjs/*` كلها `2.3.9-canary.7` | لا نستهلك أي `canary` |
| تعارض الإصدارات | React 18 و19 في مستودع واحد + `overrides` طويلة | إصدار React واحد في دولار |
| ادّعاءات README | README يقول `apps/android` موجود ✅ (تحقّقنا: موجود) لكن يفترض API على `10.0.2.2:4000` | لا نأخذ الادّعاءات كمسلّمات؛ كل شيء يُتحقق عبر API |
| فجوة التوثيق | `docs/` في Mercur 5 ملفات فقط مقابل 5746 ملف كود | نكتب الوثائق أولًا (الخطوة 1 و2) |
| سقف الـ API | 60 طلبًا/ساعة بلا توكن | كل قراءة تُجمَّع في استدعاءات شجرة واحدة |

---

## 8. قائمة المصادر (قابلة لإعادة التحقق)

- `https://api.github.com/repos/mercurjs/mercur/contents/`
- `https://api.github.com/repos/mercurjs/mercur/git/trees/main?recursive=1`
- `https://raw.githubusercontent.com/mercurjs/mercur/main/package.json`
- `https://raw.githubusercontent.com/mercurjs/mercur/main/docs/ARCHITECTURE.md`
- `https://raw.githubusercontent.com/mercurjs/mercur/main/README.md`
- `https://github.com/mercurjs/mercur` · الترخيص: **MIT**

---

_أُعِدَّت هذه الخريطة بقراءة سحابية فقط. لا نسخة محلية من Mercur، ولا تعديل على أي ملف خارج هذا المستودع._
