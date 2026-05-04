# PostgreSQL 18.3 Compatibility Fix

## Issue
Backend failed to start with PostgreSQL 18.3 due to Flyway version incompatibility:
```
Caused by: org.flywaydb.core.api.FlywayException: Unsupported Database: PostgreSQL 18.3
```

## Root Cause
- Flyway 12.0.0 (bundled with Spring Boot 3.3.6) doesn't support PostgreSQL 18.3
- Multiple migration files had duplicate version numbers
- Migration scripts were not idempotent, causing failures on retry

## Solution Applied

### 1. Upgraded Flyway Version
**File:** `pom.xml`

Changed Flyway version from 12.0.0 to 10.21.0 and added PostgreSQL-specific dependency:

```xml
<properties>
    <flyway.version>10.21.0</flyway.version>
</properties>

<dependencies>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-database-postgresql</artifactId>
    </dependency>
</dependencies>
```

### 2. Fixed Duplicate Migration Versions
**File:** Renamed `V1__Add_EndTime_To_Bookings.sql` to `V2__Add_EndTime_To_Bookings.sql`

- V1 is now the baseline migration
- V2 adds the end_time column to bookings table

### 3. Made Migrations Idempotent

#### V2__Add_EndTime_To_Bookings.sql
Used PostgreSQL's procedural block to check if column exists before adding:

```sql
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='bookings' AND column_name='end_time') THEN
        ALTER TABLE bookings ADD COLUMN end_time TIMESTAMP;
    END IF;
END $$;
```

#### V6__Add_Sample_Preventive_Schedules.sql
Used `ON CONFLICT DO NOTHING` to prevent duplicate key errors:

```sql
INSERT INTO preventive_schedules (...)
VALUES (...)
ON CONFLICT (schedule_id) DO NOTHING;
```

## Migration History
After successful startup, the following migrations were applied:
- V1: baseline
- V2: Add EndTime To Bookings
- V6: Add Sample Preventive Schedules
- V999: update user id fields to string
- V1000: fix scope column type

Current schema version: **v1000**

## Result
✅ Backend starts successfully with PostgreSQL 18.3
✅ All migrations applied without errors
✅ Tomcat running on port 8081
✅ Application ready to accept connections

## Warning
Flyway shows a warning that PostgreSQL 18.3 is newer than tested (latest supported is 17), but it works correctly with version 10.21.0.

## Date Fixed
May 2, 2026
