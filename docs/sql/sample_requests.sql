-- Sample Requests for INSA CSBMS System
-- Run these after ensuring your database schema is set up

-- ============================================
-- 1. PROJECT REQUESTS (3 samples)
-- ============================================
-- Workflow: USER -> ADMIN -> PROFESSIONAL (Division 0) -> ADMIN -> USER

-- Project Request 1: Office Renovation
INSERT INTO projects (
    id, title, description, project_type, priority, status, 
    budget, start_date, end_date, location, 
    requester_id, requester_name, requester_email, requester_phone,
    assigned_to_id, assigned_to_name,
    created_at, updated_at
) VALUES (
    1,
    'Office Renovation - 3rd Floor',
    'Complete renovation of the 3rd floor office space including painting, flooring, electrical work, and furniture installation. The space needs to accommodate 20 workstations with modern amenities.',
    'RENOVATION',
    'HIGH',
    'PENDING_ADMIN_REVIEW',
    250000.00,
    '2026-06-01',
    '2026-07-15',
    'Building A, 3rd Floor',
    '99a6e8b9-3445-4ca9-a023-2e7c4fb548d3', -- user@gmail.com
    'Regular User',
    'user@gmail.com',
    '+251911234567',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Project Request 2: Conference Room Setup
INSERT INTO projects (
    id, title, description, project_type, priority, status, 
    budget, start_date, end_date, location, 
    requester_id, requester_name, requester_email, requester_phone,
    assigned_to_id, assigned_to_name,
    created_at, updated_at
) VALUES (
    2,
    'New Conference Room Setup',
    'Setup of a new conference room with audio-visual equipment, video conferencing system, smart board, and seating for 30 people. Includes network infrastructure and acoustic treatment.',
    'NEW_CONSTRUCTION',
    'MEDIUM',
    'PENDING_ADMIN_REVIEW',
    180000.00,
    '2026-05-15',
    '2026-06-30',
    'Building B1, 2nd Floor',
    '99a6e8b9-3445-4ca9-a023-2e7c4fb548d3', -- user@gmail.com
    'Regular User',
    'user@gmail.com',
    '+251911234567',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Project Request 3: Parking Lot Expansion
INSERT INTO projects (
    id, title, description, project_type, priority, status, 
    budget, start_date, end_date, location, 
    requester_id, requester_name, requester_email, requester_phone,
    assigned_to_id, assigned_to_name,
    created_at, updated_at
) VALUES (
    3,
    'Parking Lot Expansion Project',
    'Expand the existing parking lot to add 50 additional parking spaces. Includes asphalt paving, line marking, lighting installation, and drainage system.',
    'INFRASTRUCTURE',
    'LOW',
    'PENDING_ADMIN_REVIEW',
    450000.00,
    '2026-07-01',
    '2026-09-30',
    'Main Campus - East Side',
    '99a6e8b9-3445-4ca9-a023-2e7c4fb548d3', -- user@gmail.com
    'Regular User',
    'user@gmail.com',
    '+251911234567',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- ============================================
-- 2. SPACE BOOKING REQUESTS (3 samples - B1 & B2)
-- ============================================
-- Workflow: USER -> ADMIN -> PROFESSIONAL (Division 0) -> ADMIN -> USER

-- Space Booking 1: Training Room in B1
INSERT INTO bookings (
    id, space_id, space_name, building, floor, room_number,
    booking_type, purpose, status, priority,
    start_date_time, end_date_time,
    number_of_attendees, setup_type,
    requester_id, requester_name, requester_email, requester_phone,
    assigned_to_id, assigned_to_name,
    special_requirements, equipment_needed,
    created_at, updated_at
) VALUES (
    1,
    101, -- Assuming space_id 101 exists
    'Training Room A',
    'B1',
    '1',
    '101',
    'TRAINING',
    'Staff Training on New Software System - Full day training session for IT department staff covering the new enterprise resource planning system.',
    'PENDING_ADMIN_REVIEW',
    'HIGH',
    '2026-05-10 08:00:00',
    '2026-05-10 17:00:00',
    25,
    'CLASSROOM',
    '99a6e8b9-3445-4ca9-a023-2e7c4fb548d3', -- user@gmail.com
    'Regular User',
    'user@gmail.com',
    '+251911234567',
    NULL,
    NULL,
    'Need projector, whiteboard, and stable internet connection. Coffee break setup required.',
    'Projector, Laptop, Microphone, Whiteboard, WiFi',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Space Booking 2: Meeting Room in B2
INSERT INTO bookings (
    id, space_id, space_name, building, floor, room_number,
    booking_type, purpose, status, priority,
    start_date_time, end_date_time,
    number_of_attendees, setup_type,
    requester_id, requester_name, requester_email, requester_phone,
    assigned_to_id, assigned_to_name,
    special_requirements, equipment_needed,
    created_at, updated_at
) VALUES (
    2,
    205, -- Assuming space_id 205 exists
    'Executive Meeting Room',
    'B2',
    '2',
    '205',
    'MEETING',
    'Quarterly Board Meeting - Strategic planning and budget review meeting with senior management and board members.',
    'PENDING_ADMIN_REVIEW',
    'HIGH',
    '2026-05-15 09:00:00',
    '2026-05-15 15:00:00',
    15,
    'BOARDROOM',
    '99a6e8b9-3445-4ca9-a023-2e7c4fb548d3', -- user@gmail.com
    'Regular User',
    'user@gmail.com',
    '+251911234567',
    NULL,
    NULL,
    'VIP setup required. Need catering service for lunch. Confidential meeting - ensure privacy.',
    'Video Conference System, Projector, Conference Phone, Flipchart',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Space Booking 3: Auditorium in B1
INSERT INTO bookings (
    id, space_id, space_name, building, floor, room_number,
    booking_type, purpose, status, priority,
    start_date_time, end_date_time,
    number_of_attendees, setup_type,
    requester_id, requester_name, requester_email, requester_phone,
    assigned_to_id, assigned_to_name,
    special_requirements, equipment_needed,
    created_at, updated_at
) VALUES (
    3,
    150, -- Assuming space_id 150 exists
    'Main Auditorium',
    'B1',
    'Ground',
    'Auditorium',
    'EVENT',
    'Annual Staff Conference - Company-wide annual conference with keynote speakers, presentations, and awards ceremony.',
    'PENDING_ADMIN_REVIEW',
    'MEDIUM',
    '2026-06-20 08:00:00',
    '2026-06-20 18:00:00',
    200,
    'THEATER',
    '99a6e8b9-3445-4ca9-a023-2e7c4fb548d3', -- user@gmail.com
    'Regular User',
    'user@gmail.com',
    '+251911234567',
    NULL,
    NULL,
    'Full AV setup needed. Stage lighting and sound system. Registration desk at entrance. Catering for 200 people.',
    'Sound System, Stage Lighting, Projector, Microphones (3), Video Recording',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- ============================================
-- 3. MAINTENANCE REQUESTS (3 samples)
-- ============================================
-- Workflow: USER -> ADMIN -> DIVISION SUPERVISOR -> DIVISION PROFESSIONAL -> DIVISION SUPERVISOR -> ADMIN -> USER

-- Maintenance Request 1: Electrical Issue (Division 1 - Power Supply)
INSERT INTO maintenance (
    id, title, description, maintenance_type, category, priority, status,
    location, building, floor, room_number,
    requester_id, requester_name, requester_email, requester_phone,
    assigned_division_id, assigned_division_name,
    assigned_to_id, assigned_to_name,
    reported_date, scheduled_date,
    created_at, updated_at
) VALUES (
    1,
    'Electrical Outlet Not Working - Room 305',
    'Multiple electrical outlets in Room 305 are not functioning. Computers and equipment cannot be powered. This is affecting daily operations and needs urgent attention.',
    'CORRECTIVE',
    'ELECTRICAL',
    'HIGH',
    'PENDING_ADMIN_REVIEW',
    'Building A, Room 305',
    'A',
    '3',
    '305',
    '99a6e8b9-3445-4ca9-a023-2e7c4fb548d3', -- user@gmail.com
    'Regular User',
    'user@gmail.com',
    '+251911234567',
    NULL,
    NULL,
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Maintenance Request 2: HVAC Issue (Division 2 - Facility Administration)
INSERT INTO maintenance (
    id, title, description, maintenance_type, category, priority, status,
    location, building, floor, room_number,
    requester_id, requester_name, requester_email, requester_phone,
    assigned_division_id, assigned_division_name,
    assigned_to_id, assigned_to_name,
    reported_date, scheduled_date,
    created_at, updated_at
) VALUES (
    2,
    'Air Conditioning Not Cooling - 2nd Floor',
    'The air conditioning system on the entire 2nd floor is not cooling properly. Temperature is uncomfortably high (30°C+). Staff are complaining about the heat.',
    'CORRECTIVE',
    'HVAC',
    'HIGH',
    'PENDING_ADMIN_REVIEW',
    'Building B2, 2nd Floor',
    'B2',
    '2',
    'All Rooms',
    '99a6e8b9-3445-4ca9-a023-2e7c4fb548d3', -- user@gmail.com
    'Regular User',
    'user@gmail.com',
    '+251911234567',
    NULL,
    NULL,
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Maintenance Request 3: Plumbing Issue (Division 3 - Infrastructure Development)
INSERT INTO maintenance (
    id, title, description, maintenance_type, category, priority, status,
    location, building, floor, room_number,
    requester_id, requester_name, requester_email, requester_phone,
    assigned_division_id, assigned_division_name,
    assigned_to_id, assigned_to_name,
    reported_date, scheduled_date,
    created_at, updated_at
) VALUES (
    3,
    'Water Leak in Restroom - 1st Floor',
    'Significant water leak detected in the men''s restroom on the 1st floor. Water is pooling on the floor and may cause damage. Appears to be coming from under the sink.',
    'CORRECTIVE',
    'PLUMBING',
    'URGENT',
    'PENDING_ADMIN_REVIEW',
    'Building B1, 1st Floor Restroom',
    'B1',
    '1',
    'Restroom',
    '99a6e8b9-3445-4ca9-a023-2e7c4fb548d3', -- user@gmail.com
    'Regular User',
    'user@gmail.com',
    '+251911234567',
    NULL,
    NULL,
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- ============================================
-- NOTES:
-- ============================================
-- 1. Update the requester_id with actual user IDs from your Keycloak users
-- 2. Update space_id values to match your actual spaces table
-- 3. Adjust dates to be in the future relative to when you run this
-- 4. The assigned_to_id and assigned_division_id are NULL because these are pending admin review
-- 5. Status values should match your enum/status definitions in the backend
-- 6. Priority levels: URGENT, HIGH, MEDIUM, LOW
-- 7. Project types: RENOVATION, NEW_CONSTRUCTION, INFRASTRUCTURE, REPAIR
-- 8. Booking types: MEETING, TRAINING, EVENT, WORKSHOP
-- 9. Maintenance types: CORRECTIVE, PREVENTIVE, PREDICTIVE
-- 10. Maintenance categories: ELECTRICAL, HVAC, PLUMBING, STRUCTURAL, GENERAL

-- ============================================
-- EXPECTED WORKFLOW AFTER INSERTION:
-- ============================================
-- Projects & Bookings:
--   1. Admin reviews and assigns to professional@gmail.com (Division 0)
--   2. Professional completes the work
--   3. Admin approves and closes
--   4. User is notified

-- Maintenance:
--   1. Admin reviews and assigns to appropriate division (1, 2, or 3)
--   2. Division supervisor assigns to their professional
--   3. Professional completes the work
--   4. Division supervisor approves
--   5. Admin does final approval and closes
--   6. User is notified
