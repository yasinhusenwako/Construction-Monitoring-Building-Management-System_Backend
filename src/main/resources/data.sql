
INSERT INTO divisions (id, name, description) VALUES
(1, 'Electromechanical Maintenance Division', 'Handles electrical and mechanical maintenance operations'),
(2, 'Facility Administration Division', 'Coordinates office and hall facility operations'),
(3, 'Infrastructure Development & Building Maintenance Division', 'Manages infrastructure development and building maintenance')
ON DUPLICATE KEY UPDATE
name = VALUES(name),
description = VALUES(description);

INSERT INTO users (id, name, email, password, role, divisionId) VALUES
(1, 'System Admin', 'admin@cmbms.com', '$2a$10$/O1Ts4kxwLasxJjH4I/4seCTlgZCGYV8X8d2W5nIjCbz36Wld.JUW', 'ADMIN', NULL),
(2, 'Division Supervisor', 'supervisor@cmbms.com', '$2a$10$/O1Ts4kxwLasxJjH4I/4seCTlgZCGYV8X8d2W5nIjCbz36Wld.JUW', 'SUPERVISOR', 1),
(3, 'Field Professional', 'professional@cmbms.com', '$2a$10$/O1Ts4kxwLasxJjH4I/4seCTlgZCGYV8X8d2W5nIjCbz36Wld.JUW', 'PROFESSIONAL', 1),
(4, 'Regular User', 'user@cmbms.com', '$2a$10$/O1Ts4kxwLasxJjH4I/4seCTlgZCGYV8X8d2W5nIjCbz36Wld.JUW', 'USER', NULL)
ON DUPLICATE KEY UPDATE
name = VALUES(name),
password = VALUES(password),
role = VALUES(role),
divisionId = VALUES(divisionId);

INSERT INTO projects (
    id, projectId, title, location, department, contactPerson, phone, siteCondition, description, budget,
    startDate, endDate, classification, priority, status, createdBy, createdAt, divisionId, assignedSupervisorId, boqApproved
) VALUES
(1, 'PRJ-001', 'Science Block Renovation', 'Main Campus', 'Infrastructure', 'Aline Uwimana', '0788000001', 'Operational', 'Renovation of science block classrooms', 150000.00,
 '2026-04-01', '2026-06-30', 'Renovation', 'High', 'APPROVED', 4, NOW(), 3, 2, TRUE),
(2, 'PRJ-002', 'Admin Office Expansion', 'Administration Wing', 'Facility', 'Jean Bosco', '0788000002', 'Occupied', 'Expand office space for admin staff', 90000.00,
 '2026-05-01', '2026-08-15', 'Expansion', 'Medium', 'UNDER_REVIEW', 4, NOW(), 2, 2, FALSE)
ON DUPLICATE KEY UPDATE title = VALUES(title), status = VALUES(status), assignedSupervisorId = VALUES(assignedSupervisorId), boqApproved = VALUES(boqApproved);

INSERT INTO bookings (
    id, bookingId, type, status, requester, dateTime, capacity, layout, amenities, divisionId
) VALUES
(1, 'BKG-001', 'HALL', 'SUBMITTED', 4, DATE_ADD(NOW(), INTERVAL 1 DAY), 120, 'Main Hall', 'Projector,PA System', 2),
(2, 'BKG-002', 'OFFICE', 'APPROVED', 4, DATE_ADD(NOW(), INTERVAL 2 DAY), 8, 'Meeting Room A', 'Whiteboard,WiFi', 2)
ON DUPLICATE KEY UPDATE status = VALUES(status), dateTime = VALUES(dateTime), amenities = VALUES(amenities);

INSERT INTO maintenance_requests (
    id, maintenanceId, category, priority, description, location, status, createdBy, createdAt, divisionId, assignedSupervisorId, assignedProfessionalId
) VALUES
(1, 'MNT-001', 'Electrical', 'High', 'Power outage in Lab 2', 'Science Block - Lab 2', 'ASSIGNED_TO_PROFESSIONALS', 4, NOW(), 1, 2, 3),
(2, 'MNT-002', 'Plumbing', 'Medium', 'Leaking pipe near cafeteria', 'Cafeteria', 'IN_PROGRESS', 4, NOW(), 3, 2, 3),
(3, 'MNT-003', 'Carpentry', 'Low', 'Repair broken classroom door', 'Block C - Room 12', 'COMPLETED', 4, NOW(), 3, 2, 3)
ON DUPLICATE KEY UPDATE status = VALUES(status), assignedSupervisorId = VALUES(assignedSupervisorId), assignedProfessionalId = VALUES(assignedProfessionalId);

INSERT INTO work_orders (
    id, maintenanceRequestId, assignedProfessionalId, instructions, status
) VALUES
(1, 1, 3, 'Check panel and restore feeder line safely.', 'ASSIGNED_TO_PROFESSIONALS'),
(2, 2, 3, 'Replace damaged pipe joint and test pressure.', 'IN_PROGRESS'),
(3, 3, 3, 'Replace hinge and reinforce lock fixture.', 'COMPLETED')
ON DUPLICATE KEY UPDATE status = VALUES(status), instructions = VALUES(instructions);

INSERT INTO status_history (
    id, requestId, requestType, status, changedBy, timestamp
) VALUES
(1, 1, 'MAINTENANCE', 'SUBMITTED', 4, NOW()),
(2, 1, 'MAINTENANCE', 'UNDER_REVIEW', 1, NOW()),
(3, 1, 'MAINTENANCE', 'ASSIGNED_TO_SUPERVISOR', 1, NOW()),
(4, 1, 'MAINTENANCE', 'ASSIGNED_TO_PROFESSIONALS', 2, NOW()),
(5, 1, 'MAINTENANCE', 'IN_PROGRESS', 3, NOW()),
(6, 2, 'PROJECT', 'SUBMITTED', 4, NOW()),
(7, 1, 'PROJECT', 'APPROVED', 1, NOW()),
(8, 1, 'BOOKING', 'SUBMITTED', 4, NOW()),
(9, 2, 'BOOKING', 'APPROVED', 1, NOW())
ON DUPLICATE KEY UPDATE status = VALUES(status), changedBy = VALUES(changedBy), timestamp = VALUES(timestamp);

INSERT INTO notifications (id, userId, title, message, isRead, createdAt) VALUES
(1, 2, 'New assignment', 'You have been assigned maintenance MNT-001', FALSE, NOW()),
(2, 3, 'New task', 'Maintenance MNT-001 assigned to you', FALSE, NOW()),
(3, 4, 'Project approved', 'Project PRJ-001 approved', FALSE, NOW())
ON DUPLICATE KEY UPDATE message = VALUES(message), isRead = VALUES(isRead), createdAt = VALUES(createdAt);
