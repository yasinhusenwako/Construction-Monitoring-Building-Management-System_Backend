-- Check the status of maintenance requests in DIV-001
-- Run this in pgAdmin to see what status the maintenance requests have

SELECT 
    maintenance_id,
    category,
    status,
    division_id,
    assigned_supervisor_id,
    created_by,
    created_at
FROM maintenance_requests
WHERE division_id = 'DIV-001'
ORDER BY created_at DESC;

-- Also check if there are ANY maintenance requests at all
SELECT 
    COUNT(*) as total_maintenance,
    COUNT(CASE WHEN division_id = 'DIV-001' THEN 1 END) as div_001_count,
    COUNT(CASE WHEN status = 'Submitted' THEN 1 END) as submitted_count,
    COUNT(CASE WHEN status = 'Assigned to Supervisor' THEN 1 END) as assigned_to_supervisor_count
FROM maintenance_requests;

-- Check what statuses exist in the database
SELECT DISTINCT status, COUNT(*) as count
FROM maintenance_requests
GROUP BY status
ORDER BY count DESC;
