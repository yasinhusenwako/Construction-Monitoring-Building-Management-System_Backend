# Fix: Division Not Set for Supervisor

**Date:** 2026-05-02
**Status:** ⚠️ Requires Keycloak Configuration

---

## Problem
```
[API ERROR] 400 /api/maintenance
{"error":"Bad Request","message":"Division not set for supervisor"}

✅ Division ID extracted from token: undefined
```

Supervisors cannot access maintenance requests because `divisionId` is not being extracted from the Keycloak JWT token.

---

## Root Cause

Keycloak stores custom user attributes (like `divisionId`) in the user profile, but these attributes are **NOT automatically included in the JWT token**. 

The JWT token only contains standard claims like:
- `sub` (subject/user ID)
- `email`
- `name`
- `preferred_username`
- `realm_access` (roles)

Custom attributes like `divisionId` need a **Protocol Mapper** to be included in the token.

---

## Solution (2 Parts)

### Part 1: Improved Token Extraction (✅ Done)

Updated `SecurityUtils.extractUserPrincipalFromJwt()` to try multiple ways to extract `divisionId`:
1. Direct claim: `jwt.getClaimAsString("divisionId")`
2. Alternative names: `division_id`, `divisionid`
3. Nested in attributes object
4. Handle array format (Keycloak stores attributes as arrays)

**File Modified:**
- `Backend/src/main/java/com/org/cmbms/common/security/SecurityUtils.java`

**Status:** ✅ Compiled successfully

### Part 2: Add Keycloak Protocol Mapper (⚠️ User Action Required)

Add a protocol mapper in Keycloak to include `divisionId` in the JWT token.

**Quick Steps:**
1. Login to Keycloak Admin: http://localhost:8090
2. Select realm: **insa**
3. Go to: Clients → **insa-backend** → Client scopes → **insa-backend-dedicated** → Mappers
4. Click: "Add mapper" → "By configuration" → "User Attribute"
5. Configure:
   - Name: `divisionId`
   - User Attribute: `divisionId`
   - Token Claim Name: `divisionId`
   - Claim JSON Type: `String`
   - Add to ID token: **ON**
   - Add to access token: **ON**
   - Add to userinfo: **ON**
6. Click "Save"
7. Logout and login again to get new token

**Detailed Instructions:** See `Backend/KEYCLOAK_PROTOCOL_MAPPER_SETUP.md`

---

## Testing

### Before Fix
```bash
# Login as supervisor
# Navigate to Maintenance page
# Error: "Division not set for supervisor"
# Backend logs: "✅ Division ID extracted from token: undefined"
```

### After Fix
```bash
# Add protocol mapper in Keycloak
# Logout and login again
# Navigate to Maintenance page
# Success: See maintenance requests for your division
# Backend logs: "✅ Division ID extracted from token: DIV-001"
```

---

## Verification

### 1. Check User Attributes
```
Keycloak Admin → Users → director1@gmail.com → Attributes
Should see: divisionId = DIV-001
```

### 2. Check JWT Token
```
1. Login to frontend
2. Open DevTools → Network → Any API request
3. Copy Authorization header (Bearer token)
4. Go to https://jwt.io and paste token
5. Check payload contains: "divisionId": "DIV-001"
```

### 3. Check Backend Logs
```
✅ Division ID extracted from token: DIV-001
```

---

## Files Modified

1. `Backend/src/main/java/com/org/cmbms/common/security/SecurityUtils.java` - Improved divisionId extraction

## Files Created

1. `Backend/KEYCLOAK_PROTOCOL_MAPPER_SETUP.md` - Detailed setup guide
2. `Backend/FIX_DIVISION_NOT_SET.md` - This file

---

## Status

✅ **Backend Code**: Fixed and compiled
⚠️ **Keycloak Config**: Protocol mapper needs to be added manually
📝 **Documentation**: Complete setup guide provided

---

## Next Steps

1. **Restart Backend** (to apply code changes)
   ```bash
   cd Backend
   mvn spring-boot:run
   ```

2. **Add Protocol Mapper** in Keycloak (see KEYCLOAK_PROTOCOL_MAPPER_SETUP.md)

3. **Test Supervisor Access**
   - Login as director1@gmail.com / Supervisor@123
   - Navigate to Maintenance page
   - Should see division's maintenance requests

---

**Fix applied to backend code!**
**Keycloak configuration required to complete the fix.**
