-- Cleanup failed Flyway migration
-- Run this in pgAdmin if the migration fails

-- Check current migration status
SELECT * FROM flyway_schema_history ORDER BY installed_rank;

-- Delete the failed migration record (if it exists)
DELETE FROM flyway_schema_history WHERE version = '2' AND success = false;

-- Verify cleanup
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
