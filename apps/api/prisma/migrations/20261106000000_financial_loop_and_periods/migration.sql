-- Track the buyer's outstanding debit as a cache of ledger debits less credits.
ALTER TABLE "User"
  ADD COLUMN IF NOT EXISTS "debitBalance" DECIMAL(18,4) NOT NULL DEFAULT 0;

-- Backfill existing users from the append-only ledger.
UPDATE "User" AS buyer
SET "debitBalance" = COALESCE((
  SELECT SUM(CASE WHEN tx."transactionType" = 'DEBIT' THEN tx.amount ELSE -tx.amount END)
  FROM "FinancialTransaction" AS tx
  WHERE tx."buyerId" = buyer.id
), 0);

ALTER TYPE "InvoiceStatus" ADD VALUE IF NOT EXISTS 'PARTIALLY_PAID';
ALTER TYPE "InvoiceStatus" ADD VALUE IF NOT EXISTS 'PAID';
