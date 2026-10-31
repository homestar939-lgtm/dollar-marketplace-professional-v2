-- Race-free document numbering (two concurrent requests can never collide)
CREATE SEQUENCE IF NOT EXISTS "invoice_number_seq" START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS "receipt_number_seq" START WITH 1 INCREMENT BY 1;

-- Intended payment allocations live on the receipt until an accountant approves
-- it; only approved allocations may touch an invoice's outstanding balance.
ALTER TABLE "Receipt" ADD COLUMN "plannedAllocations" JSONB;
