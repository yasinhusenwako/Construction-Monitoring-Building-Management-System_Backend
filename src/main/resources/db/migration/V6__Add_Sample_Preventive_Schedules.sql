-- Add sample preventive maintenance schedules for PostgreSQL

-- First, let's ensure we have some professional users
-- (Assuming user IDs 5 and 6 are professionals based on existing data)

INSERT INTO preventive_schedules (schedule_id, system, frequency, last_done, next_due, status, assignee, assigned_professional_id, notes, created_at, updated_at)
VALUES
-- Overdue schedules
('PM-001', 'HVAC – Floor 1 & 2', 'Every 3 months', '2025-10-15', '2026-01-15', 'Overdue', 'Tekle Haile', 5, 'Check filters, coils, and refrigerant levels', '2025-10-15', CURRENT_DATE),
('PM-002', 'Fire Suppression System', 'Every 6 months', '2025-09-20', '2026-03-20', 'Overdue', 'Dawit Tadesse', 6, 'Test sprinklers and alarm systems', '2025-09-20', CURRENT_DATE),

-- Due Today
('PM-003', 'Generator – HQ Block A', 'Every month', CURRENT_DATE - INTERVAL '1 month', CURRENT_DATE, 'Due Today', 'Tekle Haile', 5, 'Check fuel levels, oil, and battery', CURRENT_DATE - INTERVAL '1 month', CURRENT_DATE),

-- Due Soon (within 7 days)
('PM-004', 'Elevator A1 – Tower A', 'Every 6 months', CURRENT_DATE - INTERVAL '6 months', CURRENT_DATE + INTERVAL '5 days', 'Due Soon', 'Dawit Tadesse', 6, 'Inspect cables, brakes, and safety systems', CURRENT_DATE - INTERVAL '6 months', CURRENT_DATE),
('PM-005', 'HVAC – Floor 3 & 4', 'Every 3 months', CURRENT_DATE - INTERVAL '3 months', CURRENT_DATE + INTERVAL '3 days', 'Due Soon', 'Tekle Haile', 5, 'Replace filters and clean ducts', CURRENT_DATE - INTERVAL '3 months', CURRENT_DATE),

-- Scheduled (more than 7 days away)
('PM-006', 'UPS & Power Systems', 'Every 3 months', CURRENT_DATE - INTERVAL '2 months', CURRENT_DATE + INTERVAL '1 month', 'Scheduled', 'Dawit Tadesse', 6, 'Test battery backup and voltage regulation', CURRENT_DATE - INTERVAL '2 months', CURRENT_DATE),
('PM-007', 'Water Pumps – Main Building', 'Every 6 months', CURRENT_DATE - INTERVAL '4 months', CURRENT_DATE + INTERVAL '2 months', 'Scheduled', 'Tekle Haile', 5, 'Check motor, seals, and pressure', CURRENT_DATE - INTERVAL '4 months', CURRENT_DATE),
('PM-008', 'Emergency Lighting System', 'Every year', CURRENT_DATE - INTERVAL '3 months', CURRENT_DATE + INTERVAL '9 months', 'Scheduled', 'Dawit Tadesse', 6, 'Test all emergency lights and exit signs', CURRENT_DATE - INTERVAL '3 months', CURRENT_DATE),
('PM-009', 'Plumbing System Inspection', 'Every 6 months', CURRENT_DATE - INTERVAL '1 month', CURRENT_DATE + INTERVAL '5 months', 'Scheduled', 'Tekle Haile', 5, 'Check for leaks, pressure, and drainage', CURRENT_DATE - INTERVAL '1 month', CURRENT_DATE),
('PM-010', 'Security System – CCTV', 'Every 3 months', CURRENT_DATE - INTERVAL '1 month', CURRENT_DATE + INTERVAL '2 months', 'Scheduled', 'Dawit Tadesse', 6, 'Clean cameras, check recording, test motion sensors', CURRENT_DATE - INTERVAL '1 month', CURRENT_DATE);
