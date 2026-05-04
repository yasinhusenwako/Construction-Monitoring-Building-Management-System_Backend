-- Insert Divisions for INSA CSBMS System
-- Run this in pgAdmin before inserting sample requests

-- Clear existing divisions (optional - comment out if you want to keep existing data)
-- TRUNCATE TABLE divisions RESTART IDENTITY CASCADE;

-- Insert divisions
INSERT INTO divisions (id, name, description) VALUES
(0, 'Administration', 'Central administration - handles projects and space bookings'),
(1, 'Power Supply Division', 'Handles electrical maintenance and power-related issues'),
(2, 'Facility Administration Division', 'Handles HVAC, facility management, and general maintenance'),
(3, 'Infrastructure Development & Building Maintenance Division', 'Handles plumbing, structural, and infrastructure maintenance')
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description;

-- Verify insertion
SELECT * FROM divisions ORDER BY id;

-- Expected result:
-- id | name                                                      | description
-- ---+-----------------------------------------------------------+----------------------------------------------------------
--  0 | Administration                                            | Central administration - handles projects and space bookings
--  1 | Power Supply Division                                     | Handles electrical maintenance and power-related issues
--  2 | Facility Administration Division                          | Handles HVAC, facility management, and general maintenance
--  3 | Infrastructure Development & Building Maintenance Division| Handles plumbing, structural, and infrastructure maintenance
