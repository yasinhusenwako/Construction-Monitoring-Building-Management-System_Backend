-- Sample Requests for INSA CSBMS System
-- WITH ACTUAL USER IDs FROM YOUR KEYCLOAK
-- Ready to run in pgAdmin

-- ============================================
-- USER IDs FROM YOUR SYSTEM:
-- ============================================
-- admin@gmail.com: 86f2dcb7-6830-4a06-b374-57d5ded865d9
-- director1@gmail.com: bf891695-d335-45be-b18c-5ecda49092b9
-- director2@gmail.com: 269e09c7-841a-4ad1-ba89-507e4dbfba20
-- director3@gmail.com: 4cfbcd46-e188-4438-acd0-08a588d49921
-- professional@gmail.com: da9d5702-d0a5-4659-9c49-048f515c8053
-- professional1@gmail.com: 00650836-02e2-4c86-ae51-b7c8a40c3063
-- professional2@gmail.com: 345b3f65-b3d6-4d19-b80f-98ecbee9314b
-- professional3@gmail.com: 404b70da-8724-44c5-afbd-b458862b43ee
-- user@gmail.com: 99a6e8b9-3445-4ca9-a023-2e7c4fb548d3

-- ============================================
-- 1. PROJECT REQUESTS (3 samples)
-- ============================================
-- Workflow: USER -> ADMIN -> PROFESSIONAL (Division 0) -> ADMIN -> USER

-- Project Request 1: Office Renovation
INSERT INTO projects (
    title, description, project_type, priority, status, 
    budget, start_date, end_date, location, 
    requester_id, requester_name, requester_email, requester_phone,
    created_at, updated_at
) VALUES (
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
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Project Request 2: Conference Room Setup
INSERT INTO projects (
    title, description, project_type, priority, status, 
    budget, start_date, end_date, location, 
    requester_id, requester_name, requester_email, requester_phone,
    created_at, updated_at
) VALUES (
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
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Project Request 3: Parking Lot Expansion
INSERT INTO projects (
    title, description, project_type, priority, status, 
    budget, start_date, end_date, location, 
    requester_id, requester_name, requester_email, requester_phone,
    created_at, updated_at
) VALUES (
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
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- ============================================
-- 2. SPACE BOOKING REQUESTS (3 samples - B1 & B2)
-- ============================================
-- Workflow: USER -> ADMIN -> PROFESSIONAL (Division 0) -> ADMIN -> USER

-- Space Booking 1: Training Room in B1
INSERT INTO bookings (
    space_name, building, floor, room_number,
    booking_type, purpose, status, priority,
    start_date_time, end_date_time,
    number_of_attendees, setup_type,
    requester_id, requester_name, requester_email, requester_phone,
    special_requirements, equipment_needed,
    created_at, updated_at
) VALUES (
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
    'Need projector, whiteboard, and stable internet connection. Coffee break setup required.',
    'Projector, Laptop, Microphone, Whiteboard, WiFi',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Space Booking 2: Meeting Room in B2
INSERT INTO bookings (
    space_name, building, floor, room_number,
    booking_type, purpose, status, priority,
    start_date_time, end_date_time,
    number_of_attendees, setup_type,
    requester_id, requester_name, requester_email, requester_phone,
    special_requirements, equipment_needed,
    created_at, updated_at
) VALUES (
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
    'VIP setup required. Need catering service for lunch. Confidential meeting - ensure privacy.',
    'Video Conference System, Projector, Conference Phone, Flipchart',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Space Booking 3: Auditorium in B1
INSERT INTO bookings (
    space_name, building, floor, room_number,
    booking_type, purpose, status, priority,
    start_date_time, end_date_time,
    number_of_attendees, setup_type,
    requester_id, requester_name, requester_email, requester_phone,
    special_requirements, equipment_needed,
    created_at, updated_at
) VALUES (
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
    title, description, maintenance_type, category, priority, status,
    location, building, floor, room_number,
    requester_id, requester_name, requester_email, requester_phone,
    reported_date,
    created_at, updated_at
) VALUES (
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
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Maintenance Request 2: HVAC Issue (Division 2 - Facility Administration)
INSERT INTO maintenance (
    title, description, maintenance_type, category, priority, status,
    location, building, floor, room_number,
    requester_id, requester_name, requester_email, requester_phone,
    reported_date,
    created_at, updated_at
) VALUES (
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
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Maintenance Request 3: Plumbing Issue (Division 3 - Infrastructure Development)
INSERT INTO maintenance (
    title, description, maintenance_type, category, priority, status,
    location, building, floor, room_number,
    requester_id, requester_name, requester_email, requester_phone,
    reported_date,
    created_at, updated_at
) VALUES (
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
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- ============================================
-- VERIFICATION QUERIES
-- ============================================
-- Run these after insertion to verify:

-- Check projects
SELECT id, title, status, priority, requester_name, budget 
FROM projects 
ORDER BY created_at DESC;

-- Check bookings
SELECT id, space_name, building, booking_type, status, 
       start_date_time, number_of_attendees, requester_name
FROM bookings 
ORDER BY created_at DESC;

-- Check maintenance
SELECT id, title, category, priority, status, 
       location, requester_name
FROM maintenance 
ORDER BY created_at DESC;

-- ============================================
-- NEXT STEPS FOR TESTING WORKFLOWS:
-- ============================================

-- 1. ADMIN ASSIGNS PROJECT TO PROFESSIONAL:
-- UPDATE projects 
-- SET status = 'ASSIGNED',
--     assigned_to_id = 'da9d5702-d0a5-4659-9c49-048f515c8053', -- professional@gmail.com
--     assigned_to_name = 'Admin Professional',
--     updated_at = CURRENT_TIMESTAMP
-- WHERE id = 1;

-- 2. ADMIN ASSIGNS MAINTENANCE TO DIVISION 1:
-- UPDATE maintenance 
-- SET status = 'ASSIGNED_TO_DIVISION',
--     assigned_division_id = 1,
--     assigned_division_name = 'Power Supply Division',
--     updated_at = CURRENT_TIMESTAMP
-- WHERE id = 1;

-- 3. DIVISION 1 SUPERVISOR ASSIGNS TO PROFESSIONAL:
-- UPDATE maintenance 
-- SET status = 'ASSIGNED_TO_PROFESSIONAL',
--     assigned_to_id = '00650836-02e2-4c86-ae51-b7c8a40c3063', -- professional1@gmail.com
--     assigned_to_name = 'Division 1 Professional',
--     updated_at = CURRENT_TIMESTAMP
-- WHERE id = 1;
