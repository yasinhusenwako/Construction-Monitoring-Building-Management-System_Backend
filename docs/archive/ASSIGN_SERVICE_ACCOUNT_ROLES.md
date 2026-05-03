# Assign Service Account Roles to insa-backend Client

## Current Status
✅ Backend is using service account authentication (client credentials)
✅ Client secret is configured
❌ Service account needs admin roles to access Keycloak Admin API

## Steps to Assign Roles

### Step 1: Access Keycloak Admin Console

1. Go to: **http://localhost:8090**
2. Click **"Administration Console"**
3. Login: **admin** / **admin**

### Step 2: Switch to INSA Realm

- Click the dropdown at **top-left** (shows "master")
- Select **"insa"**

### Step 3: Configure Service Account Roles

1. **Go to Clients:**
   - Click **"Clients"** in the left menu
   - Find and click **"insa-backend"**

2. **Go to Service Account Roles tab:**
   - Click the **"Service account roles"** tab
   - You should see "Service account enabled" message

3. **Assign Realm Management Roles:**
   - Click **"Assign role"** button
   - Click the **"Filter by clients"** dropdown
   - Select **"realm-management"**
   
4. **Select these roles** (check the boxes):
   - ✅ `view-users`
   - ✅ `query-users`
   - ✅ `manage-users`
   - ✅ `view-realm`
   - ✅ `manage-realm` (optional, for full admin access)

5. **Click "Assign"**

### Step 4: Verify Roles Assigned

After assigning, you should see the roles listed in the "Assigned roles" section:
- realm-management: view-users
- realm-management: query-users
- realm-management: manage-users
- realm-management: view-realm

### Step 5: Test the API

1. **Refresh your application** in the browser
2. **Try accessing user-related features:**
   - Admin dashboard
   - Team page
   - User management

3. **The 401 error should be gone!**

## What These Roles Do

| Role | Permission |
|------|------------|
| `view-users` | View user list and details |
| `query-users` | Search and query users |
| `manage-users` | Create, update, delete users |
| `view-realm` | View realm configuration |
| `manage-realm` | Full realm management (optional) |

## Verification

### Check Backend Logs

After assigning roles, the backend should be able to fetch users without errors.

### Test API Endpoint

Try accessing: http://localhost:8081/api/keycloak/users

Should return user list instead of 401 error.

## Troubleshooting

### Still Getting 401 After Assigning Roles

1. **Restart the backend:**
   ```bash
   # Kill current process
   taskkill /PID <backend-pid> /F
   
   # Start again
   cd Backend
   mvn spring-boot:run
   ```

2. **Verify roles are assigned:**
   - Go back to Keycloak
   - Check "Service account roles" tab
   - Roles should be listed under "Assigned roles"

3. **Check client secret is correct:**
   - Go to "Credentials" tab
   - Verify the secret matches what's in `Backend/.env`

### Service Account Not Enabled

If you don't see the "Service account roles" tab:

1. Go to **"Settings"** tab
2. Scroll down to **"Capability config"**
3. Enable **"Service accounts roles"**
4. Click **"Save"**
5. The "Service account roles" tab should now appear

## Why Service Account is Better

### Before (Username/Password):
- ❌ Uses master realm admin credentials
- ❌ Less secure
- ❌ Password can expire
- ❌ Requires user account

### After (Service Account):
- ✅ Uses client credentials (OAuth2)
- ✅ More secure
- ✅ No password expiration
- ✅ No user account needed
- ✅ Fine-grained permissions

## Configuration Summary

### Backend Configuration:
```yaml
keycloak:
  admin:
    server-url: http://localhost:8090
    realm: insa
    client-id: insa-backend
    client-secret: 6krziITC6UadIt5iTsKVuNZ5I976OwkM
```

### Authentication Method:
```java
KeycloakBuilder.builder()
    .serverUrl(serverUrl)
    .realm(realm)
    .clientId(clientId)
    .clientSecret(clientSecret)
    .grantType("client_credentials")  // Service account
    .build();
```

## Next Steps

1. ✅ Assign service account roles (follow steps above)
2. ✅ Test the application
3. ✅ Verify 401 errors are gone

---

**Date**: May 2, 2026
