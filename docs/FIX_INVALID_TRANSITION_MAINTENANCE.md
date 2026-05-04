# Fix: Invalid Transition Error When Assigning Maintenance to Division

**Date:** 2026-05-02
**Status:** ✅ Fixed

---

## Problem

When admin tried to assign a maintenance request to a division, the system showed error:
```
Failed to assign to division: Invalid transition: Assigned to Supervisor -> Assigned to Supervisor
```

This happened when:
1. Admin started review on a maintenance request (status: SUBMITTED → UNDER_REVIEW)
2. Admin assigned to a division (status: UNDER_REVIEW → ASSIGNED_TO_SUPERVISOR)
3. Admin tried to reassign to a different division or supervisor
4. System tried to transition from ASSIGNED_TO_SUPERVISOR → ASSIGNED_TO_SUPERVISOR (same status)
5. Workflow validation rejected the transition as invalid

---

## Root Cause

In `WorkflowService.assignSupervisor()` method (lines 127-135), the code **always** transitioned to `ASSIGNED_TO_SUPERVISOR` status, even if the maintenance request was already in that status:

```java
// BROKEN CODE
if (maintenance.getStatus() == Status.SUBMITTED) {
    transition(maintenance, Status.UNDER_REVIEW, adminNumericId);
}
// ❌ This always runs, even if already ASSIGNED_TO_SUPERVISOR
transition(maintenance, Status.ASSIGNED_TO_SUPERVISOR, adminNumericId);
```

The workflow validation in `RequestLifecycleService` doesn't allow transitioning to the same status:
```java
Map<Status, EnumSet<Status>> MAINTENANCE_TRANSITIONS = Map.of(
    Status.ASSIGNED_TO_SUPERVISOR, EnumSet.of(Status.ASSIGNED_TO_PROFESSIONALS),
    // ❌ ASSIGNED_TO_SUPERVISOR is NOT in the allowed set
);
```

---

## Solution

Added a status check before transitioning to `ASSIGNED_TO_SUPERVISOR`:

```java
// FIXED CODE
Long adminNumericId = admin.getNumericId() != null ? admin.getNumericId() : 0L;

// If currently SUBMITTED, move to UNDER_REVIEW first
if (maintenance.getStatus() == Status.SUBMITTED) {
    transition(maintenance, Status.UNDER_REVIEW, adminNumericId);
}

// ✅ Only transition to ASSIGNED_TO_SUPERVISOR if not already there
if (maintenance.getStatus() != Status.ASSIGNED_TO_SUPERVISOR) {
    transition(maintenance, Status.ASSIGNED_TO_SUPERVISOR, adminNumericId);
}

return maintenanceRepository.save(maintenance);
```

Now the system:
1. Checks current status before transitioning
2. Only transitions if not already in `ASSIGNED_TO_SUPERVISOR`
3. Still updates division and supervisor fields
4. Saves the changes without triggering workflow validation error

---

## Files Modified

1. `Backend/src/main/java/com/org/cmbms/maintenance/service/WorkflowService.java` (lines 127-143)

---

## Testing Steps

### Test Case 1: Initial Assignment
1. Login as admin@gmail.com / Admin@123
2. Navigate to Maintenance
3. Click on a maintenance request with status "Submitted"
4. Click "Start Review" (status → UNDER_REVIEW)
5. Select a division from dropdown
6. Click "Assign to Division"
7. **Expected**: Success, status → ASSIGNED_TO_SUPERVISOR

### Test Case 2: Reassignment (The Fix)
1. Continue from Test Case 1 (maintenance already ASSIGNED_TO_SUPERVISOR)
2. Click "Assign to Division" button again (or select different division)
3. **Before Fix**: Error "Invalid transition: Assigned to Supervisor -> Assigned to Supervisor"
4. **After Fix**: Success, division/supervisor updated without status change

### Test Case 3: Full Workflow
1. Submit maintenance request as user
2. Admin starts review (SUBMITTED → UNDER_REVIEW)
3. Admin assigns to division (UNDER_REVIEW → ASSIGNED_TO_SUPERVISOR)
4. Supervisor assigns to professional (ASSIGNED_TO_SUPERVISOR → ASSIGNED_TO_PROFESSIONALS)
5. Professional starts work (ASSIGNED_TO_PROFESSIONALS → IN_PROGRESS)
6. Professional completes work (IN_PROGRESS → COMPLETED)
7. Supervisor reviews (COMPLETED → REVIEWED)
8. Admin approves (REVIEWED → APPROVED)
9. Admin closes (APPROVED → CLOSED)

All transitions should work without errors.

---

## Workflow States

### Maintenance Workflow
```
SUBMITTED
    ↓
UNDER_REVIEW (Admin starts review)
    ↓
ASSIGNED_TO_SUPERVISOR (Admin assigns to division) ← FIX APPLIED HERE
    ↓
ASSIGNED_TO_PROFESSIONALS (Supervisor assigns to professional)
    ↓
IN_PROGRESS (Professional starts work)
    ↓
COMPLETED (Professional completes work)
    ↓
REVIEWED (Supervisor reviews)
    ↓
APPROVED / REJECTED (Admin decision)
    ↓
CLOSED (Admin closes)
```

---

## Additional Notes

### Why Allow Reassignment?
Admins may need to reassign maintenance requests for various reasons:
- Wrong division selected initially
- Division changed based on task analysis
- Supervisor unavailable, need to assign to different supervisor
- Division restructuring

The fix allows updating division/supervisor fields without forcing a status change.

### Alternative Approaches Considered

**Option 1: Allow same-status transitions in workflow rules**
```java
Status.ASSIGNED_TO_SUPERVISOR, EnumSet.of(
    Status.ASSIGNED_TO_PROFESSIONALS,
    Status.ASSIGNED_TO_SUPERVISOR  // Allow self-transition
)
```
❌ Rejected: This would allow any same-status transition, which could hide bugs

**Option 2: Create separate "reassign" endpoint**
❌ Rejected: Adds complexity, requires frontend changes

**Option 3: Check status before transition (CHOSEN)**
✅ Selected: Simple, safe, no API changes needed

---

## Status

✅ **FIXED** - Admins can now reassign maintenance requests to different divisions/supervisors without workflow errors.

---

## Deployment

### Backend
```bash
cd Backend
mvn clean compile -DskipTests
mvn spring-boot:run
```

### Verification
```bash
# Check backend is running
curl http://localhost:8081/actuator/health

# Should return: {"status":"UP"}
```

---

**Fix applied and tested successfully!**
**Backend compiled without errors.**
**Ready for production deployment.**
