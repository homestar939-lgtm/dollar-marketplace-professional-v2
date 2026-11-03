-- ═══════════════════════════════════════════════════════════════════════════
-- هجرة إصدار V3: ميزات الهاتف (OTP) والتسليم والفواتير والوكلاء
-- كل العبارات idempotent (IF NOT EXISTS) ليعاد تشغيلها بأمان على قاعدة قائمة.
-- ═══════════════════════════════════════════════════════════════════════════

-- ─────────────────────────── المستخدم والتحقق بالهاتف ───────────────────────
-- ملاحظة: عمود phone موجود مسبقاً غير فريد؛ نبقيه كما هو ثم نضيف فهرساً فريداً.
ALTER TABLE "User" ADD COLUMN IF NOT EXISTS "phone" TEXT;
ALTER TABLE "User" ADD COLUMN IF NOT EXISTS "phoneVerifiedAt" TIMESTAMP(3);
-- دورة التحصيل الافتراضية للمستخدم (شهرية).
ALTER TABLE "User" ADD COLUMN IF NOT EXISTS "collectionPeriod" TEXT DEFAULT 'MONTHLY';
CREATE UNIQUE INDEX IF NOT EXISTS "User_phone_key" ON "User"("phone");

-- ───────────────────────────── رموز التحقق OTP ──────────────────────────────
CREATE TABLE IF NOT EXISTS "OtpCode" (
    "id" TEXT NOT NULL,
    "userId" TEXT NOT NULL,
    "code" TEXT NOT NULL,
    "purpose" TEXT NOT NULL,
    "expiresAt" TIMESTAMP(3) NOT NULL,
    "usedAt" TIMESTAMP(3),
    "attempts" INTEGER NOT NULL DEFAULT 0,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "OtpCode_pkey" PRIMARY KEY ("id")
);

-- فهارس البحث عن الرموز: حسب المستخدم والغرض، وحسب انتهاء الصلاحية للتنظيف.
CREATE INDEX IF NOT EXISTS "OtpCode_userId_purpose_idx" ON "OtpCode"("userId", "purpose");
CREATE INDEX IF NOT EXISTS "OtpCode_expiresAt_idx" ON "OtpCode"("expiresAt");

-- ربط الرموز بالمستخدم مع الحذف التسلسلي عند حذف المستخدم.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'OtpCode_userId_fkey') THEN
        ALTER TABLE "OtpCode"
            ADD CONSTRAINT "OtpCode_userId_fkey"
            FOREIGN KEY ("userId") REFERENCES "User"("id")
            ON DELETE CASCADE ON UPDATE CASCADE;
    END IF;
END
$$;

-- ───────────────────── قيمة DELIVERED في نوع حالة الطلب ─────────────────────
-- في هذا المستودع النوع OrderStatus موجود والقيمة DELIVERED ضمنه مسبقاً،
-- لذا العبارة داخل ELSE تستخدم IF NOT EXISTS لتكون عديمة الأثر تماماً.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'OrderStatus') THEN
        -- النوع غير موجود: لا ننشئه هنا، فهو مسؤولية هجرة init.
        NULL;
    ELSE
        ALTER TYPE "OrderStatus" ADD VALUE IF NOT EXISTS 'DELIVERED';
    END IF;
END
$$;

-- ─────────────────────────────── بيانات التسليم ─────────────────────────────
ALTER TABLE "Order" ADD COLUMN IF NOT EXISTS "deliveredAt" TIMESTAMP(3);
ALTER TABLE "Order" ADD COLUMN IF NOT EXISTS "deliveredBy" TEXT;

-- ───────────────────────────── بيانات الوكالة ──────────────────────────────
-- ملاحظة: عمود address موجود مسبقاً بنوع Json؛ العبارة التالية لا تُغيّر نوعه.
ALTER TABLE "Agency" ADD COLUMN IF NOT EXISTS "phone" TEXT;
ALTER TABLE "Agency" ADD COLUMN IF NOT EXISTS "address" TEXT;
ALTER TABLE "Agency" ADD COLUMN IF NOT EXISTS "notes" TEXT;
ALTER TABLE "Agency" ADD COLUMN IF NOT EXISTS "signature" TEXT;
ALTER TABLE "Agency" ADD COLUMN IF NOT EXISTS "invoiceFooter" TEXT;
ALTER TABLE "Agency" ADD COLUMN IF NOT EXISTS "receiptFooter" TEXT;

-- ───────────────────────────── بيانات المنتج ──────────────────────────────
-- ملاحظة: showCountry/showDescription/showImages موجودة مسبقاً؛ العبارات لا تُغيّر شيئاً.
ALTER TABLE "Product" ADD COLUMN IF NOT EXISTS "showExpiryDate" BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE "Product" ADD COLUMN IF NOT EXISTS "showCountry" BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE "Product" ADD COLUMN IF NOT EXISTS "showDescription" BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE "Product" ADD COLUMN IF NOT EXISTS "showImages" BOOLEAN NOT NULL DEFAULT true;

-- ───────────────────────────── تذييل سند القبض ─────────────────────────────
ALTER TABLE "Receipt" ADD COLUMN IF NOT EXISTS "footerText" TEXT;
ALTER TABLE "Receipt" ADD COLUMN IF NOT EXISTS "agencySignature" TEXT;

-- ───────────────────────────── تذييل الفاتورة ──────────────────────────────
ALTER TABLE "Invoice" ADD COLUMN IF NOT EXISTS "footerText" TEXT;
ALTER TABLE "Invoice" ADD COLUMN IF NOT EXISTS "agencySignature" TEXT;
