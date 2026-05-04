# Keycloak Protocol Mapper Setup - divisionId

## Problem
```
Division not set for supervisor
✅ Division ID extracted from token: undefined
```

The `divisionId` custom attribute is stored in Keycloak user attributes, but it's NOT included in the JWT token by default. We need to add a **Protocol Mapper** to include it.

---

## Solution: Add Protocol Mapper for divisionId

### Step 1: Login to Keycloak Admin Console
1. Open browser: http://localhost:8090
2. Click "Administration Console"
3. Login: admin / admin
4. Select realm: **insa** (top-left dropdown)

### Step 2: Add Protocol Mapper to Client
1. Click "Clients" in left sidebar
2. Click on **"insa-backend"** client
3. Click "Client scopes" tab
4. Click on **"insa-backend-dedicated"** (or create if doesn't exist)
5. Click "Mappers" tab
6. Click "Add mapper" → "By configuration"
7. Select **"User Attribute"**

### Step 3: Configure divisionId Mapper
Fill in the form:

| Field | Value |
|-------|-------|
| **Name** | divisionId |
| **User Attribute** | divisionId |
| **Token Claim Name** | divisionId |
| **Claim JSON Type** | String |
| **Add to ID token** | ON |
| **Add to access token** | ON |
| **Add to userinfo** | ON |
| **Multivalued** | OFF |

Click **"Save"**

### Step 4: Repeat for Frontend Client (Optional but Recommended)
1. Go back to "Clients"
2. Click on **"insa-frontend"** client
3. Follow steps 3-6 above to add the same mapper

### Step 5: Verify Token Contains divisionId
1. Logout from frontend (if logged in)
2. Login again
3. Check backend logs - should see:
   ```
   ✅ Division ID extracted from token: DIV-001
   ```

---

## Alternative: Add Protocol Mapper via Realm Export/Import

If you prefer to add the mapper via realm configuration file, update `keycloak-insa-realm-template.json`:

### Add to insa-backend client:
```json
{
  "clientId": "insa-backend",
  "protocolMappers": [
    {
      "name": "divisionId",
      "protocol": "openid-connect",
      "protocolMapper": "oidc-usermodel-attribute-mapper",
      "consentRequired": false,
      "config": {
        "userinfo.token.claim": "true",
        "user.attribute": "divisionId",
        "id.token.claim": "true",
        "access.token.claim": "true",
        "claim.name": "divisionId",
        "jsonType.label": "String"
      }
    }
  ]
}
```

Then reimport the realm:
```bash
cd Frontend
docker-compose -f docker-compose.keycloak.yml down
rm -rf keycloak-data
docker-compose -f docker-compose.keycloak.yml up -d
```

---

## Verification Steps

### 1. Check User Attributes in Keycloak
1. Go to Keycloak Admin Console
2. Select realm: **insa**
3. Click "Users" → "View all users"
4. Click on a supervisor (e.g., director1@gmail.com)
5. Click "Attributes" tab
6. Verify `divisionId` = `DIV-001`

### 2. Check JWT Token
1. Login to frontend as supervisor
2. Open browser DevTools → Network tab
3. Find any API request to backend
4. Copy the Authorization header (Bearer token)
5. Go to https://jwt.io
6. Paste the token
7. Check the payload - should contain:
   ```json
   {
     "email": "director1@gmail.com",
     "name": "Division 1 Director",
     "divisionId": "DIV-001",
     ...
   }
   ```

### 3. Test Supervisor Access
1. Login as director1@gmail.com / Supervisor@123
2. Navigate to Maintenance page
3. Should see maintenance requests for Division 1
4. No "Division not set for supervisor" error

---

## Troubleshooting

### Token still doesn't contain divisionId
1. **Clear browser cache** or use incognito window
2. **Logout and login again** to get new token
3. **Restart backend** to reload security configuration
4. **Check mapper is enabled** in Keycloak

### Mapper not showing in token
1. Verify mapper is added to correct client scope
2. Check "Add to access token" is ON
3. Verify user attribute name matches exactly: `divisionId` (case-sensitive)

### Still getting "Division not set" error
1. Check backend logs for divisionId extraction
2. Verify user has divisionId attribute in Keycloak
3. Ensure attribute value is not empty
4. Check attribute format matches: `DIV-001`, `DIV-002`, `DIV-003`

---

## Quick Fix (Manual Testing)

If you need to test immediately without setting up protocol mapper, you can temporarily modify the code to use a default division for testing:

```java
// TEMPORARY FIX - FOR TESTING ONLY
if (divisionId == null && role == Role.SUPERVISOR) {
    divisionId = "DIV-001"; // Default for testing
    System.out.println("⚠️ Using default divisionId for testing: " + divisionId);
}
```

**DO NOT use this in production!** This is only for testing the workflow.

---

## Status
⚠️ **ACTION REQUIRED**: Add protocol mapper in Keycloak to include divisionId in JWT token

Once protocol mapper is added:
✅ divisionId will be included in JWT token
✅ Supervisors can access their division's maintenance requests
✅ No more "Division not set for supervisor" errors
