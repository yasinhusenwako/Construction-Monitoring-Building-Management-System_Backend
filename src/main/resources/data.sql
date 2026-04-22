
INSERT INTO divisions (id, name, description) VALUES
(1, 'Electromechanical Maintenance Division', 'Handles electrical and mechanical maintenance operations'),
(2, 'Facility Administration Division', 'Coordinates office and hall facility operations'),
(3, 'Infrastructure Development & Building Maintenance Division', 'Manages infrastructure development and building maintenance')
ON CONFLICT (id) DO UPDATE SET
name = EXCLUDED.name,
description = EXCLUDED.description;
INSERT INTO projects (
    id, project_id, title, location, department, contact_person, phone, site_condition, description, budget,
    start_date, end_date, classification, priority, status, created_by, created_at, division_id, assigned_supervisor_id, boq_approved
) VALUES
(1, 'PRJ-001', 'Science Block Renovation', 'Main Campus', 'Infrastructure', 'Aline Uwimana', '0788000001', 'Operational', 'Renovation of science block classrooms', 150000.00,
 '2026-04-01', '2026-06-30', 'Renovation', 'High', 'APPROVED', 4, NOW(), 3, 2, TRUE),
(2, 'PRJ-002', 'Admin Office Expansion', 'Administration Wing', 'Facility', 'Jean Bosco', '0788000002', 'Occupied', 'Expand office space for admin staff', 90000.00,
 '2026-05-01', '2026-08-15', 'Expansion', 'Medium', 'UNDER_REVIEW', 4, NOW(), 2, 2, FALSE)
ON CONFLICT (id) DO UPDATE SET
title = EXCLUDED.title,
status = EXCLUDED.status,
assigned_supervisor_id = EXCLUDED.assigned_supervisor_id,
boq_approved = EXCLUDED.boq_approved;

INSERT INTO bookings (
    id, booking_id, type, status, requester, date_time, capacity, layout, amenities, division_id
) VALUES
(1, 'BKG-001', 'HALL', 'SUBMITTED', 4, NOW() + INTERVAL '1 day', 120, 'Main Hall', 'Projector,PA System', 2),
(2, 'BKG-002', 'OFFICE', 'APPROVED', 4, NOW() + INTERVAL '2 day', 8, 'Meeting Room A', 'Whiteboard,WiFi', 2)
ON CONFLICT (id) DO UPDATE SET
status = EXCLUDED.status,
date_time = EXCLUDED.date_time,
amenities = EXCLUDED.amenities;

INSERT INTO maintenance_requests (
    id, maintenance_id, category, priority, description, location, status, created_by, created_at, division_id, assigned_supervisor_id, assigned_professional_id
) VALUES
(1, 'MNT-001', 'Electrical', 'High', 'Power outage in Lab 2', 'Science Block - Lab 2', 'SUBMITTED', 4, NOW(), NULL, NULL, NULL)
ON CONFLICT (id) DO UPDATE SET
status = EXCLUDED.status,
assigned_supervisor_id = EXCLUDED.assigned_supervisor_id,
assigned_professional_id = EXCLUDED.assigned_professional_id;

INSERT INTO work_orders (
    id, maintenance_request_id, assigned_professional_id, instructions, status
) VALUES
(1, 1, NULL, '', 'SUBMITTED')
ON CONFLICT (id) DO UPDATE SET
status = EXCLUDED.status,
instructions = EXCLUDED.instructions;

INSERT INTO status_history (
    id, request_id, request_type, status, changed_by, timestamp
) VALUES
(1, 1, 'MAINTENANCE', 'SUBMITTED', 4, NOW()),
(2, 1, 'PROJECT', 'SUBMITTED', 4, NOW()),
(3, 1, 'PROJECT', 'APPROVED', 1, NOW()),
(4, 1, 'BOOKING', 'SUBMITTED', 4, NOW()),
(5, 2, 'BOOKING', 'APPROVED', 1, NOW())
ON CONFLICT (id) DO UPDATE SET
status = EXCLUDED.status,
changed_by = EXCLUDED.changed_by,
timestamp = EXCLUDED.timestamp;

INSERT INTO notifications (id, user_id, title, message, is_read, created_at) VALUES
(1, 1, 'System Ready', 'Welcome to INSA BuildMS', FALSE, NOW())
ON CONFLICT (id) DO UPDATE SET
message = EXCLUDED.message,
is_read = EXCLUDED.is_read,
created_at = EXCLUDED.created_at;

-- Reset sequences to avoid duplicate key errors when inserting new records
SELECT setval('divisions_id_seq', (SELECT MAX(id) FROM divisions));
SELECT setval('users_id_seq', COALESCE((SELECT MAX(id) FROM users), 1));
SELECT setval('projects_id_seq', (SELECT MAX(id) FROM projects));
SELECT setval('bookings_id_seq', (SELECT MAX(id) FROM bookings));
SELECT setval('maintenance_requests_id_seq', (SELECT MAX(id) FROM maintenance_requests));
SELECT setval('work_orders_id_seq', (SELECT MAX(id) FROM work_orders));
SELECT setval('status_history_id_seq', (SELECT MAX(id) FROM status_history));
SELECT setval('notifications_id_seq', (SELECT MAX(id) FROM notifications));
