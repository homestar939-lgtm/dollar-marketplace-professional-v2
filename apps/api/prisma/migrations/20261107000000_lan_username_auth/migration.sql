-- Preserve existing accounts while moving authentication to username/password only.
ALTER TABLE "User" ADD COLUMN "username" TEXT;

UPDATE "User"
SET "username" = COALESCE(
  NULLIF(regexp_replace(lower(split_part("email", '@', 1)), '[^a-z0-9_.-]', '', 'g'), ''),
  'user'
) || '_' || substr(md5("id"), 1, 6);

ALTER TABLE "User" ALTER COLUMN "username" SET NOT NULL;
UPDATE "User" SET "status" = 'ACTIVE' WHERE "status" = 'PENDING_VERIFICATION';
ALTER TABLE "User" ALTER COLUMN "status" SET DEFAULT 'ACTIVE';
CREATE UNIQUE INDEX "User_username_key" ON "User"("username");

DROP TABLE IF EXISTS "EmailVerificationToken";
DROP TABLE IF EXISTS "PasswordResetToken";
DROP TABLE IF EXISTS "OtpCode";
DROP INDEX IF EXISTS "User_email_key";
DROP INDEX IF EXISTS "User_googleSubject_key";
ALTER TABLE "User" DROP COLUMN "email";
ALTER TABLE "User" DROP COLUMN "emailVerifiedAt";
ALTER TABLE "User" DROP COLUMN "googleSubject";
ALTER TABLE "User" DROP COLUMN "phoneVerifiedAt";
