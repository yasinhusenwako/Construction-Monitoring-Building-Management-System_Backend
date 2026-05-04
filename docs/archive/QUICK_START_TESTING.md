# Quick Start Testing Guide

## Your Users (Ready to Use)

| Email | Password | Role | Division | Purpose |
|-------|----------|------|----------|---------|
| admin@gmail.com | Admin@123 | ADMIN | Administration | Manages everything |
| professional@gmail.com | Professional@123 | PROFESSIONAL | Administration | Handles projects & bookings |
| director1@gmail.com | Supervisor@123 | SUPERVISOR | Division 1 | Power Supply Division |
| professional1@gmail.com | Professional@123 | PROFESSIONAL | Division 1 | Electrical maintenance |
| director2@gmail.com | Supervisor@123 | SUPERVISOR | Division 2 | Facility Administration |
| professional2@gmail.com | Professional@123 | PROFESSIONAL | Division 2 | HVAC maintenance |
| director3@gmail.com | Supervisor@123 | SUPERVISOR | Division 3 | Infrastructure |
| professional3@gmail.com | Professional@123 | PROFESSIONAL | Division 3 | Plumbing maintenance |
| user@gmail.com | User@123 | USER | General | Submits requests |

## Step-by-Step: Insert Sample Data

### 1. Open pgAdmin
- Connect to your PostgreSQL server
- Select database: `cmbms`

### 2. Open Query Tool
- Right-click on `cmbms` database
- Select "Query Tool"

### 3. Copy and Paste SQL
- Open file: `Backend/sample_requests_with_actual_ids.sql`
- Copy ALL the INSERT statements
- Paste into Query Tool
- Click "Execute" (F5)

### 4. Verify Data Inserted
Run these queries:
```sql
-- Should show 3 projects
SELECT COUNT(*) FROM projects;

-- Should show 3 bookings
SELECT COUNT(*) FROM bookings;

-- Should show 3 maintenance requests
SELECT COUNT(*) FROM maintenance;
```

## Testing Workflows

### Test 1: Project Workflow (Simple)

**Step 1: View as Admin**
1. Login: `admin@gmail.com` / `Admin@123`
2. Go to: Dashboard → Project Management
3. You should see 3 pending projects

**Step 2: Assign to Professional**
1. Click on "Office Renovation - 3rd Floor"
2. Click "Assign"
3. Select: `professional@gmail.com` (Admin Professional)
4. Click "Assign"
5. Status changes to: ASSIGNED

**Step 3: View as Professional**
1. Logout and login as: `professional@gmail.com` / `Professional@123`
2. Go to: Dashboard → My Tasks
3. You should see the assigned project
4. Click "Mark as Complete"
5. Status changes to: PENDING_ADMIN_APPROVAL

**Step 4: Approve as Admin**
1. Logout and login as: `admin@gmail.com` / `Admin@123`
2. Go to: Dashboard → Project Management
3. Click on the completed project
4. Click "Approve & Close"
5. Status changes to: COMPLETED

**Step 5: Verify as User**
1. Logout and login as: `user@gmail.com` / `User@123`
2. Go to: Dashboard → My Requests
3. You should see the completed project

---

### Test 2: Maintenance Workflow (Complex)

**Step 1: View as Admin**
1. Login: `admin@gmail.com` / `Admin@123`
2. Go to: Dashboard → Maintenance
3. You should see 3 pending maintenance requests

**Step 2: Assign to Division**
1. Click on "Electrical Outlet Not Working"
2. Click "Assign to Division"
3. Select: Division 1 (Power Supply Division)
4. Click "Assign"
5. Status changes to: ASSIGNED_TO_DIVISION

**Step 3: View as Division Supervisor**
1. Logout and login as: `director1@gmail.com` / `Supervisor@123`
2. Go to: Dashboard → Division Tasks
3. You should see the electrical maintenance request
4. Click "Assign to Professional"
5. Select: `professional1@gmail.com` (Division 1 Professional)
6. Click "Assign"
7. Status changes to: ASSIGNED_TO_PROFESSIONAL

**Step 4: Complete as Professional**
1. Logout and login as: `professional1@gmail.com` / `Professional@123`
2. Go to: Dashboard → My Tasks
3. You should see the electrical maintenance
4. Click "Mark as Complete"
5. Add completion notes
6. Status changes to: PENDING_SUPERVISOR_APPROVAL

**Step 5: Approve as Supervisor**
1. Logout and login as: `director1@gmail.com` / `Supervisor@123`
2. Go to: Dashboard → Division Tasks
3. Click on the completed maintenance
4. Review the work
5. Click "Approve"
6. Status changes to: PENDING_ADMIN_APPROVAL

**Step 6: Final Approval as Admin**
1. Logout and login as: `admin@gmail.com` / `Admin@123`
2. Go to: Dashboard → Maintenance
3. Click on the maintenance request
4. Review all details
5. Click "Approve & Close"
6. Status changes to: COMPLETED

**Step 7: Verify as User**
1. Logout and login as: `user@gmail.com` / `User@123`
2. Go to: Dashboard → My Requests
3. You should see the completed maintenance request

---

### Test 3: Space Booking Workflow

**Step 1: View as Admin**
1. Login: `admin@gmail.com` / `Admin@123`
2. Go to: Dashboard → Space Booking
3. You should see 3 pending bookings

**Step 2: Assign to Professional**
1. Click on "Training Room A" booking
2. Click "Assign"
3. Select: `professional@gmail.com` (Admin Professional)
4. Click "Assign"
5. Status changes to: ASSIGNED

**Step 3: Setup as Professional**
1. Logout and login as: `professional@gmail.com` / `Professional@123`
2. Go to: Dashboard → My Tasks
3. You should see the booking
4. Click "Mark as Ready"
5. Status changes to: PENDING_ADMIN_APPROVAL

**Step 4: Approve as Admin**
1. Logout and login as: `admin@gmail.com` / `Admin@123`
2. Go to: Dashboard → Space Booking
3. Click on the booking
4. Click "Approve & Confirm"
5. Status changes to: CONFIRMED

**Step 5: Verify as User**
1. Logout and login as: `user@gmail.com` / `User@123`
2. Go to: Dashboard → My Bookings
3. You should see the confirmed booking

---

## Quick SQL Commands for Testing

### Assign Project to Professional (Skip UI)
```sql
UPDATE projects 
SET status = 'ASSIGNED',
    assigned_to_id = 'da9d5702-d0a5-4659-9c49-048f515c8053',
    assigned_to_name = 'Admin Professional',
    updated_at = CURRENT_TIMESTAMP
WHERE title = 'Office Renovation - 3rd Floor';
```

### Assign Maintenance to Division 1
```sql
UPDATE maintenance 
SET status = 'ASSIGNED_TO_DIVISION',
    assigned_division_id = 1,
    assigned_division_name = 'Power Supply Division',
    updated_at = CURRENT_TIMESTAMP
WHERE title = 'Electrical Outlet Not Working - Room 305';
```

### Assign Maintenance to Professional
```sql
UPDATE maintenance 
SET status = 'ASSIGNED_TO_PROFESSIONAL',
    assigned_to_id = '00650836-02e2-4c86-ae51-b7c8a40c3063',
    assigned_to_name = 'Division 1 Professional',
    updated_at = CURRENT_TIMESTAMP
WHERE title = 'Electrical Outlet Not Working - Room 305';
```

### Mark as Completed
```sql
UPDATE maintenance 
SET status = 'COMPLETED',
    completed_date = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE title = 'Electrical Outlet Not Working - Room 305';
```

---

## Expected Results

After inserting sample data, you should have:

### Projects Table
- 3 projects with status: PENDING_ADMIN_REVIEW
- All requested by: user@gmail.com
- Priorities: HIGH, MEDIUM, LOW
- Total budget: 880,000 ETB

### Bookings Table
- 3 bookings with status: PENDING_ADMIN_REVIEW
- Buildings: B1 (2 bookings), B2 (1 booking)
- Types: TRAINING, MEETING, EVENT
- Total attendees: 240 people

### Maintenance Table
- 3 maintenance requests with status: PENDING_ADMIN_REVIEW
- Categories: ELECTRICAL, HVAC, PLUMBING
- Priorities: HIGH, HIGH, URGENT
- Suggested divisions: 1, 2, 3

---

## Troubleshooting

### Error: Column does not exist
**Solution:** Your table schema might be different. Check your actual column names:
```sql
SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'projects';
```

### Error: Foreign key violation
**Solution:** Make sure the user exists:
```sql
SELECT id, email FROM users WHERE email = 'user@gmail.com';
```

### Error: Invalid enum value
**Solution:** Check your enum definitions:
```sql
SELECT enum_range(NULL::project_status);
SELECT enum_range(NULL::priority_level);
```

### No data showing in frontend
**Solution:** 
1. Check if data exists: `SELECT * FROM projects;`
2. Verify backend is running: http://localhost:8081/actuator/health
3. Check browser console for errors
4. Verify API endpoints are working

---

## Next Steps After Testing

1. **Test Notifications**
   - Verify users receive notifications when status changes
   - Check email notifications (if configured)

2. **Test Filtering**
   - Filter by status, priority, division
   - Search functionality
   - Date range filters

3. **Test Reports**
   - Generate reports with sample data
   - Export to PDF/Excel
   - View analytics dashboards

4. **Test Permissions**
   - Verify supervisors only see their division
   - Verify professionals only see assigned tasks
   - Verify users only see their own requests

5. **Add More Sample Data**
   - Create more diverse scenarios
   - Test edge cases
   - Test concurrent assignments

---

## Date
May 2, 2026
