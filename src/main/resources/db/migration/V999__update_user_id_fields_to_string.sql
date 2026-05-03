-- Migration to support both numeric user IDs and Keycloak email identifiers
-- This allows the system to work with both traditional database users and Keycloak users

-- Update projects table
ALTER TABLE projects ALTER COLUMN created_by TYPE VARCHAR(255);

-- Update maintenance_requests table
ALTER TABLE maintenance_requests ALTER COLUMN created_by TYPE VARCHAR(255);

-- Update bookings table
ALTER TABLE bookings ALTER COLUMN requester TYPE VARCHAR(255);

-- Note: Existing numeric IDs will be automatically converted to strings
-- New Keycloak users will use email addresses as identifiers
