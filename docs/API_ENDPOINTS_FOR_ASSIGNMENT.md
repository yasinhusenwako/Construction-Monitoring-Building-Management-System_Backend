# API Endpoints for Assignment

## New Endpoints Added

### 1. Get All Divisions
**Endpoint:** `GET /api/divisions`  
**Purpose:** Get list of all divisions for admin to assign maintenance requests  
**Authentication:** Required (JWT token)  
**Response:**
```json
[
  {
    "id": 0,
    "name": "Administration",
    "description": "Central administration - handles projects and space bookings"
  },
  {
    "id": 1,
    "name": "Power Supply Division",
    "description": "Handles electrical maintenance and power-related issues"
  },
  {
    "id": 2,
    "name": "Facility Administration Division",
    "description": "Handles HVAC, facility management, and general maintenance"
  },
  {
    "id": 3,
    "name": "Infrastructure Development & Building Maintenance Division",
    "description": "Handles plumbing, structural, and infrastructure maintenance"
  }
]
```

---

### 2. Get Professionals by Division
**Endpoint:** `GET /api/users/professionals?divisionId={divisionId}`  
**Purpose:** Get professionals filtered by division for assignment  
**Authentication:** Required (JWT token)  
**Parameters:**
- `divisionId` (optional): Filter by division
  - `0` = Admin professionals (for projects/bookings)
  - `1` = Division 1 professionals (for maintenance)
  - `2` = Division 2 professionals (for maintenance)
  - `3` = Division 3 professionals (for maintenance)
  - If omitted, returns all professionals

**Examples:**

**Get Admin Professionals (for Projects/Bookings):**
```
GET /api/users/professionals?divisionId=0
```
**Response:**
```json
[
  {
    "id": -1,
    "name": "Admin Professional",
    "email": "professional@gmail.com",
    "role": "PROFESSIONAL",
    "divisionId": "0",
    "department": "Administration",
    "profession": "Project Manager",
    "phone": null
  }
]
```

**Get Division 1 Professionals (for Electrical Maintenance):**
```
GET /api/users/professionals?divisionId=1
```
**Response:**
```json
[
  {
    "id": -1,
    "name": "Division 1 Professional",
    "email": "professional1@gmail.com",
    "role": "PROFESSIONAL",
    "divisionId": "1",
    "department": "Division 1",
    "profession": "Maintenance Technician",
    "phone": null
  }
]
```

**Get Division 2 Professionals (for HVAC Maintenance):**
```
GET /api/users/professionals?divisionId=2
```

**Get Division 3 Professionals (for Plumbing/Infrastructure):**
```
GET /api/users/professionals?divisionId=3
```

---

### 3. Get All Professionals
**Endpoint:** `GET /api/users/professionals/all`  
**Purpose:** Get all professionals (admin only)  
**Authentication:** Required (JWT token, ADMIN role)  
**Response:**
```json
[
  {
    "id": -1,
    "name": "Admin Professional",
    "email": "professional@gmail.com",
    "role": "PROFESSIONAL",
    "divisionId": "0",
    "department": "Administration",
    "profession": "Project Manager"
  },
  {
    "id": -1,
    "name": "Division 1 Professional",
    "email": "professional1@gmail.com",
    "role": "PROFESSIONAL",
    "divisionId": "1",
    "department": "Division 1",
    "profession": "Maintenance Technician"
  },
  {
    "id": -1,
    "name": "Division 2 Professional",
    "email": "professional2@gmail.com",
    "role": "PROFESSIONAL",
    "divisionId": "2",
    "department": "Division 2",
    "profession": "Maintenance Technician"
  },
  {
    "id": -1,
    "name": "Division 3 Professional",
    "email": "professional3@gmail.com",
    "role": "PROFESSIONAL",
    "divisionId": "3",
    "department": "Division 3",
    "profession": "Maintenance Technician"
  }
]
```

---

## Frontend Integration

### For Project/Booking Assignment (Admin)

**Step 1: Fetch Admin Professionals**
```javascript
const response = await fetch('/api/users/professionals?divisionId=0', {
  headers: {
    'Authorization': `Bearer ${token}`
  }
});
const adminProfessionals = await response.json();
```

**Step 2: Display in Dropdown**
```jsx
<select name="assignedTo">
  <option value="">Select Professional</option>
  {adminProfessionals.map(prof => (
    <option key={prof.email} value={prof.email}>
      {prof.name} - {prof.profession}
    </option>
  ))}
</select>
```

**Step 3: Assign Project/Booking**
```javascript
await fetch(`/api/projects/${projectId}/assign`, {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    assignedToEmail: selectedProfessionalEmail,
    assignedToName: selectedProfessionalName
  })
});
```

---

### For Maintenance Assignment (Admin → Division)

**Step 1: Fetch All Divisions**
```javascript
const response = await fetch('/api/divisions', {
  headers: {
    'Authorization': `Bearer ${token}`
  }
});
const divisions = await response.json();
// Filter out division 0 (Administration) for maintenance
const maintenanceDivisions = divisions.filter(d => d.id !== 0);
```

**Step 2: Display in Dropdown**
```jsx
<select name="assignedDivision">
  <option value="">Select Division</option>
  {maintenanceDivisions.map(div => (
    <option key={div.id} value={div.id}>
      {div.name}
    </option>
  ))}
</select>
```

**Step 3: Assign to Division**
```javascript
await fetch(`/api/maintenance/${maintenanceId}/assign-division`, {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    divisionId: selectedDivisionId,
    divisionName: selectedDivisionName
  })
});
```

---

### For Maintenance Assignment (Supervisor → Professional)

**Step 1: Fetch Division Professionals**
```javascript
// Supervisor's divisionId comes from their JWT token
const supervisorDivisionId = currentUser.divisionId;

const response = await fetch(`/api/users/professionals?divisionId=${supervisorDivisionId}`, {
  headers: {
    'Authorization': `Bearer ${token}`
  }
});
const divisionProfessionals = await response.json();
```

**Step 2: Display in Dropdown**
```jsx
<select name="assignedTo">
  <option value="">Select Professional</option>
  {divisionProfessionals.map(prof => (
    <option key={prof.email} value={prof.email}>
      {prof.name} - {prof.profession}
    </option>
  ))}
</select>
```

**Step 3: Assign to Professional**
```javascript
await fetch(`/api/maintenance/${maintenanceId}/assign-professional`, {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    assignedToEmail: selectedProfessionalEmail,
    assignedToName: selectedProfessionalName
  })
});
```

---

## Database Setup

### Step 1: Insert Divisions
Run this SQL in pgAdmin:
```sql
-- File: Backend/insert_divisions.sql
INSERT INTO divisions (id, name, description) VALUES
(0, 'Administration', 'Central administration - handles projects and space bookings'),
(1, 'Power Supply Division', 'Handles electrical maintenance and power-related issues'),
(2, 'Facility Administration Division', 'Handles HVAC, facility management, and general maintenance'),
(3, 'Infrastructure Development & Building Maintenance Division', 'Handles plumbing, structural, and infrastructure maintenance')
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description;
```

### Step 2: Verify Divisions
```sql
SELECT * FROM divisions ORDER BY id;
```

---

## Testing with cURL

### Test Get Divisions
```bash
curl -X GET http://localhost:8081/api/divisions \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Test Get Admin Professionals
```bash
curl -X GET "http://localhost:8081/api/users/professionals?divisionId=0" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Test Get Division 1 Professionals
```bash
curl -X GET "http://localhost:8081/api/users/professionals?divisionId=1" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Test Get All Professionals
```bash
curl -X GET http://localhost:8081/api/users/professionals/all \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## Important Notes

### User ID Format
- Keycloak users have `id = -1` (negative ID)
- Use `email` as the identifier for assignment
- The backend will look up the user by email when assigning

### Division IDs
- `0` = Administration (for projects/bookings)
- `1` = Power Supply Division (electrical)
- `2` = Facility Administration Division (HVAC, general)
- `3` = Infrastructure Division (plumbing, structural)

### Assignment Flow

**Projects & Bookings:**
```
1. Admin fetches professionals with divisionId=0
2. Admin assigns to selected professional (by email)
3. Professional receives task
```

**Maintenance:**
```
1. Admin fetches divisions (exclude division 0)
2. Admin assigns to selected division
3. Division supervisor fetches professionals with their divisionId
4. Supervisor assigns to selected professional (by email)
5. Professional receives task
```

---

## Troubleshooting

### Issue: Empty professionals list
**Solution:** 
1. Verify users exist in Keycloak with PROFESSIONAL role
2. Check divisionId attribute is set correctly
3. Verify backend can connect to Keycloak

### Issue: Divisions not found
**Solution:**
1. Run `insert_divisions.sql` in pgAdmin
2. Verify divisions table exists
3. Check database connection

### Issue: 401 Unauthorized
**Solution:**
1. Verify JWT token is valid
2. Check token is included in Authorization header
3. Ensure user has required role (ADMIN for most operations)

---

## Date
May 2, 2026
