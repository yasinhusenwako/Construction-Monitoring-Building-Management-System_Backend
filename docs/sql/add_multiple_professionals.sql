-- Add support for multiple professional assignment
-- This migration adds a new column to store comma-separated professional IDs
-- The old assignedProfessionalId column is kept for backward compatibility

-- Add new column to maintenance_requests
ALTER TABLE maintenance_requests 
ADD COLUMN IF NOT EXISTS assigned_professional_ids TEXT;

-- Migrate existing data from single to multiple (copy single value to new column)
UPDATE maintenance_requests 
SET assigned_professional_ids = assigned_professional_id 
WHERE assigned_professional_id IS NOT NULL 
  AND assigned_professional_id != ''
  AND (assigned_professional_ids IS NULL OR assigned_professional_ids = '');

-- Add new column to projects
ALTER TABLE projects 
ADD COLUMN IF NOT EXISTS assigned_professional_ids TEXT;

-- Migrate existing data
UPDATE projects 
SET assigned_professional_ids = assigned_professional_id 
WHERE assigned_professional_id IS NOT NULL 
  AND assigned_professional_id != ''
  AND (assigned_professional_ids IS NULL OR assigned_professional_ids = '');

-- Add new column to bookings
ALTER TABLE bookings 
ADD COLUMN IF NOT EXISTS assigned_professional_ids TEXT;

-- Migrate existing data
UPDATE bookings 
SET assigned_professional_ids = assigned_professional_id 
WHERE assigned_professional_id IS NOT NULL 
  AND assigned_professional_id != ''
  AND (assigned_professional_ids IS NULL OR assigned_professional_ids = '');

-- Verification queries
SELECT 
    'maintenance_requests' as table_name,
    COUNT(*) as total_records,
    COUNT(assigned_professional_id) as single_assigned,
    COUNT(assigned_professional_ids) as multiple_assigned
FROM maintenance_requests
UNION ALL
SELECT 
    'projects' as table_name,
    COUNT(*) as total_records,
    COUNT(assigned_professional_id) as single_assigned,
    COUNT(assigned_professional_ids) as multiple_assigned
FROM projects
UNION ALL
SELECT 
    'bookings' as table_name,
    COUNT(*) as total_records,
    COUNT(assigned_professional_id) as single_assigned,
    COUNT(assigned_professional_ids) as multiple_assigned
FROM bookings;

-- Note: The old assigned_professional_id column is NOT dropped for backward compatibility
-- It will be automatically synced with the first professional in assigned_professional_ids
