# Sample Requests Guide

This guide explains the sample requests created for testing the INSA CSBMS workflows.

## Files Created

1. **`sample_requests.sql`** - SQL INSERT statements for direct database insertion
2. **`sample_requests.json`** - JSON format for API testing or reference

## Sample Data Overview

### 3 Project Requests
1. **Office Renovation - 3rd Floor** (HIGH priority)
   - Budget: 250,000 ETB
   - Duration: June 1 - July 15, 2026
   - Type: RENOVATION

2. **New Conference Room Setup** (MEDIUM priority)
   - Budget: 180,000 ETB
   - Duration: May 15 - June 30, 2026
   - Type: NEW_CONSTRUCTION

3. **Parking Lot Expansion Project** (LOW priority)
   - Budget: 450,000 ETB
   - Duration: July 1 - September 30, 2026
   - Type: INFRASTRUCTURE

### 3 Space Booking Requests

#### Building B1:
1. **Training Room A** (HIGH priority)
   - Date: May 10, 2026 (8:00 AM - 5:00 PM)
   - Type: TRAINING
   - Attendees: 25
   - Setup: CLASSROOM

2. **Main Auditorium** (MEDIUM priority)
   - Date: June 20, 2026 (8:00 AM - 6:00 PM)
   - Type: EVENT
   - Attendees: 200
   - Setup: THEATER

#### Building B2:
3. **Executive Meeting Room** (HIGH priority)
   - Date: May 15, 2026 (9:00 AM - 3:00 PM)
   - Type: MEETING
   - Attendees: 15
   - Setup: BOARDROOM

### 3 Maintenance Requests

1. **Electrical Outlet Not Working** (HIGH priority)
   - Location: Building A, Room 305
   - Category: ELECTRICAL
   - Suggested Division: Division 1 (Power Supply Division)

2. **Air Conditioning Not Cooling** (HIGH priority)
   - Location: Building B2, 2nd Floor
   - Category: HVAC
   - Suggested Division: Division 2 (Facility Administration Division)

3. **Water Leak in Restroom** (URGENT priority)
   - Location: Building B1, 1st Floor Restroom
   - Category: PLUMBING
   - Suggested Division: Division 3 (Infrastructure Development & Building Maintenance Division)

## How to Use

### Option 1: Direct SQL Insertion

1. **Open pgAdmin or your PostgreSQL client**
2. **Connect to the `cmbms` database**
3. **Run the SQL file:**
   ```sql
   -- Copy and paste contents from sample_requests.sql
   -- Or use pgAdmin's Query Tool to execute the file
   ```

4. **Verify insertion:**
   ```sql
   SELECT * FROM projects;
   SELECT * FROM bookings;
   SELECT * FROM maintenance;
   ```

### Option 2: Via Backend API (Recommended)

#### Create Project Request:
```bash
POST http://localhost:8081/api/projects
Authorization: Bearer <USER_JWT_TOKEN>
Content-Type: application/json

{
  "title": "Office Renovation - 3rd Floor",
  "description": "Complete renovation of the 3rd floor office space...",
  "projectType": "RENOVATION",
  "priority": "HIGH",
  "budget": 250000.00,
  "startDate": "2026-06-01",
  "endDate": "2026-07-15",
  "location": "Building A, 3rd Floor"
}
```

#### Create Space Booking:
```bash
POST http://localhost:8081/api/bookings
Authorization: Bearer <USER_JWT_TOKEN>
Content-Type: application/json

{
  "spaceName": "Training Room A",
  "building": "B1",
  "floor": "1",
  "roomNumber": "101",
  "bookingType": "TRAINING",
  "purpose": "Staff Training on New Software System...",
  "priority": "HIGH",
  "startDateTime": "2026-05-10T08:00:00",
  "endDateTime": "2026-05-10T17:00:00",
  "numberOfAttendees": 25,
  "setupType": "CLASSROOM",
  "specialRequirements": "Need projector, whiteboard...",
  "equipmentNeeded": "Projector, Laptop, Microphone, Whiteboard, WiFi"
}
```

#### Create Maintenance Request:
```bash
POST http://localhost:8081/api/maintenance
Authorization: Bearer <USER_JWT_TOKEN>
Content-Type: application/json

{
  "title": "Electrical Outlet Not Working - Room 305",
  "description": "Multiple electrical outlets in Room 305 are not functioning...",
  "maintenanceType": "CORRECTIVE",
  "category": "ELECTRICAL",
  "priority": "HIGH",
  "location": "Building A, Room 305",
  "building": "A",
  "floor": "3",
  "roomNumber": "305"
}
```

### Option 3: Via Frontend UI

1. **Login as user@gmail.com** (password: User@123)
2. **Navigate to the appropriate section:**
   - Projects: Dashboard → Project Management → New Project
   - Bookings: Dashboard → Space Booking → New Booking
   - Maintenance: Dashboard → Maintenance → New Request
3. **Fill in the form with sample data**
4. **Submit the request**

## Testing Workflows

### Test Project Workflow

1. **User submits request:**
   - Login as `user@gmail.com`
   - Create project request (use sample data above)
   - Status: PENDING_ADMIN_REVIEW

2. **Admin reviews and assigns:**
   - Login as `admin@gmail.com`
   - Go to Projects → Pending Requests
   - Assign to `professional@gmail.com` (Admin Professional, Division 0)
   - Status: ASSIGNED

3. **Professional completes work:**
   - Login as `professional@gmail.com`
   - Go to My Tasks
   - Mark project as completed
   - Status: PENDING_ADMIN_APPROVAL

4. **Admin approves:**
   - Login as `admin@gmail.com`
   - Review completed work
   - Approve and close
   - Status: COMPLETED

5. **User views result:**
   - Login as `user@gmail.com`
   - Check project status
   - Should see COMPLETED

### Test Maintenance Workflow

1. **User submits request:**
   - Login as `user@gmail.com`
   - Create maintenance request (electrical issue)
   - Status: PENDING_ADMIN_REVIEW

2. **Admin assigns to division:**
   - Login as `admin@gmail.com`
   - Go to Maintenance → Pending Requests
   - Assign to Division 1 (Power Supply Division)
   - Status: ASSIGNED_TO_DIVISION

3. **Division supervisor assigns to professional:**
   - Login as `director1@gmail.com` (Division 1 Supervisor)
   - Go to Division Tasks
   - Assign to `professional1@gmail.com`
   - Status: ASSIGNED_TO_PROFESSIONAL

4. **Professional completes work:**
   - Login as `professional1@gmail.com`
   - Go to My Tasks
   - Complete the maintenance
   - Status: PENDING_SUPERVISOR_APPROVAL

5. **Supervisor approves:**
   - Login as `director1@gmail.com`
   - Review completed work
   - Approve
   - Status: PENDING_ADMIN_APPROVAL

6. **Admin final approval:**
   - Login as `admin@gmail.com`
   - Final review and close
   - Status: COMPLETED

7. **User views result:**
   - Login as `user@gmail.com`
   - Check maintenance status
   - Should see COMPLETED

## Important Notes

### Before Running SQL:

1. **Update User IDs:**
   - Replace `'99a6e8b9-3445-4ca9-a023-2e7c4fb548d3'` with actual user ID from Keycloak
   - Get user ID from: `SELECT id FROM users WHERE email = 'user@gmail.com'`

2. **Update Space IDs:**
   - Ensure spaces exist in your `spaces` table
   - Update `space_id` values to match your actual space records

3. **Check Date Validity:**
   - Update dates to be in the future relative to when you run the script
   - Ensure start_date < end_date

4. **Verify Enum Values:**
   - Ensure status values match your backend enum definitions
   - Check that priority, type, and category values are valid

### Status Flow:

**Projects & Bookings:**
```
PENDING_ADMIN_REVIEW → ASSIGNED → IN_PROGRESS → 
PENDING_ADMIN_APPROVAL → COMPLETED
```

**Maintenance:**
```
PENDING_ADMIN_REVIEW → ASSIGNED_TO_DIVISION → 
ASSIGNED_TO_PROFESSIONAL → IN_PROGRESS → 
PENDING_SUPERVISOR_APPROVAL → PENDING_ADMIN_APPROVAL → 
COMPLETED
```

### Priority Levels:
- **URGENT** - Immediate attention required
- **HIGH** - Important, needs quick response
- **MEDIUM** - Normal priority
- **LOW** - Can be scheduled later

### Division Assignment Guide:

| Category | Suggested Division |
|----------|-------------------|
| ELECTRICAL | Division 1 - Power Supply Division |
| HVAC | Division 2 - Facility Administration Division |
| PLUMBING | Division 3 - Infrastructure Development & Building Maintenance Division |
| STRUCTURAL | Division 3 - Infrastructure Development & Building Maintenance Division |
| GENERAL | Any division based on availability |

## Troubleshooting

### Issue: Foreign Key Constraint Error
**Solution:** Ensure the requester user exists in the users table first.

### Issue: Space Not Found
**Solution:** Create spaces in the spaces table before creating bookings.

### Issue: Invalid Status Value
**Solution:** Check your backend enum definitions and use matching values.

### Issue: Date in the Past
**Solution:** Update dates to be in the future.

## Next Steps After Insertion

1. **Verify data in database:**
   ```sql
   SELECT COUNT(*) FROM projects;
   SELECT COUNT(*) FROM bookings;
   SELECT COUNT(*) FROM maintenance;
   ```

2. **Test in frontend:**
   - Login as admin and view all requests
   - Test assignment functionality
   - Test status updates

3. **Test notifications:**
   - Verify users receive notifications
   - Check email/system notifications

4. **Test reporting:**
   - Generate reports with sample data
   - Verify analytics dashboards

## Date
May 2, 2026
