-- ════════════════════════════════════════════════════════════════════
-- v7.9.62: Add cover_url column to profiles table (Profile Banner System)
-- ════════════════════════════════════════════════════════════════════
-- Run this SQL in Supabase Dashboard → SQL Editor → New query → Run
--
-- WHAT THIS DOES:
--   1. Adds cover_url column (text) to profiles table
--   2. RLS policy: owner can UPDATE cover_url (covered by existing own update policy)
--   3. Public can SELECT cover_url (covered by existing public read policy)
--
-- AFTER RUNNING:
--   - Launcher v228+ can upload cover/banner images via Profile screen
--   - Cover image stored in community-images bucket (same as avatar)
--   - URL stored in profiles.cover_url column
--
-- ROLLBACK (kalau perlu):
--   ALTER TABLE public.profiles DROP COLUMN IF EXISTS cover_url;
-- ════════════════════════════════════════════════════════════════════

-- Step 1: Add cover_url column (nullable, text)
ALTER TABLE public.profiles
    ADD COLUMN IF NOT EXISTS cover_url text;

-- Step 2: Add comment for documentation
COMMENT ON COLUMN public.profiles.cover_url IS
    'v7.9.62: Profile cover/banner image URL. Stored in community-images bucket. Nullable — null means user has not uploaded a cover yet.';

-- Step 3: Verify column added
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name = 'profiles'
  AND column_name = 'cover_url';

-- Expected output:
--   column_name | data_type | is_nullable
--   ------------+-----------+-------------
--   cover_url   | text      | YES

-- Note: RLS policies already cover cover_url:
--   - "profiles public read" → SELECT * (includes cover_url) for everyone
--   - "profiles own update"  → UPDATE (including cover_url) for owner only
-- No new policies needed.

-- ════════════════════════════════════════════════════════════════════
-- OPTIONAL: Set default cover for developer account (for testing)
-- ════════════════════════════════════════════════════════════════════
-- UPDATE public.profiles
-- SET cover_url = 'https://example.com/cover.jpg'
-- WHERE username = 'dlavieid';
