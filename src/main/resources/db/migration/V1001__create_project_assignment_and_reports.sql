-- Create project_assignments table for multiple professional assignments
CREATE TABLE IF NOT EXISTS project_assignments (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    professional_id VARCHAR(255) NOT NULL,
    instructions TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NOT NULL,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    
    -- Constraints
    CONSTRAINT fk_project_id FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT unique_project_professional UNIQUE(project_id, professional_id)
);

-- Create professional_reports table for storing daily/periodic reports
CREATE TABLE IF NOT EXISTS professional_reports (
    id BIGSERIAL PRIMARY KEY,
    assignment_id BIGINT NOT NULL,
    report_text TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NOT NULL,
    
    -- Constraints
    CONSTRAINT fk_assignment_id FOREIGN KEY (assignment_id) REFERENCES project_assignments(id) ON DELETE CASCADE
);

-- Create indexes for better query performance
CREATE INDEX idx_project_assignments_project_id ON project_assignments(project_id);
CREATE INDEX idx_project_assignments_professional_id ON project_assignments(professional_id);
CREATE INDEX idx_project_assignments_status ON project_assignments(status);
CREATE INDEX idx_professional_reports_assignment_id ON professional_reports(assignment_id);
CREATE INDEX idx_professional_reports_created_at ON professional_reports(created_at);
