-- Create booking_assignments table for multiple professional assignments to bookings
CREATE TABLE IF NOT EXISTS booking_assignments (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    professional_id VARCHAR(255) NOT NULL,
    instructions TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NOT NULL,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    
    -- Constraints
    CONSTRAINT fk_booking_id FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
    CONSTRAINT unique_booking_professional UNIQUE(booking_id, professional_id)
);

-- Create booking_reports table for storing daily/periodic reports for bookings
CREATE TABLE IF NOT EXISTS booking_reports (
    id BIGSERIAL PRIMARY KEY,
    assignment_id BIGINT NOT NULL,
    report_text TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NOT NULL,
    viewed BOOLEAN DEFAULT FALSE,
    
    -- Constraints
    CONSTRAINT fk_booking_assignment_id FOREIGN KEY (assignment_id) REFERENCES booking_assignments(id) ON DELETE CASCADE
);

-- Create indexes for better query performance
CREATE INDEX idx_booking_assignments_booking_id ON booking_assignments(booking_id);
CREATE INDEX idx_booking_assignments_professional_id ON booking_assignments(professional_id);
CREATE INDEX idx_booking_assignments_status ON booking_assignments(status);
CREATE INDEX idx_booking_reports_assignment_id ON booking_reports(assignment_id);
CREATE INDEX idx_booking_reports_created_at ON booking_reports(created_at);
CREATE INDEX idx_booking_reports_created_by ON booking_reports(created_by);
