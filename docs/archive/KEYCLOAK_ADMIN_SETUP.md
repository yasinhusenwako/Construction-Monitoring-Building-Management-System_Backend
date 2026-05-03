# Keycloak Admin API Setup for Backend

## Issue
Backend is getting 401 Unauthorized when trying to fetch users from Keycloak Admin API:
```
Failed to fetch users from Keycloak: jakarta.ws.rs.NotAuthorizedException: HTTP 401 Unauthorized
```

## Root Cause
The backend needs proper credentials and permissions to access Keycloak Admin API for user management operations.

## Solution

### Step 1: Get Backend Client Secret

1. **Go to Keycloak Admin Console:**
   - URL: http://localhost:8090
   - Login: admin / admin

2. **Switch to "insa" realm** (dropdown at top-left)

3. **Go to Clients:**
   - Click **"Clients"** in left menu
   - Find and click **"insa-backend"**

4. **Go to Credentials tab:**
   - Copy the **Client Secret** value
   - Save it for Step 3

### Step 2: Assign Admin Roles to Service Account

The backend client needs admin permissions to manage users.

1. **Still in the "insa-backend" client:**
   - Go to **"Service account roles"** tab
   - Click **"Assign role"**

2. **Filter by clients:**
   - Click the **"Filter by clients"** dropdown
   - Select **"realm-management"**

3. **Assign these roles:**
   - ✅ `view-users`
   - ✅ `query-users`
   - ✅ `manage-users`
   - ✅ `view-realm`

4. **Click "Assign"**

### Step 3: Configure Backend Environment Variables

You have two options:

#### Option A: Using Environment Variables (Recommended)

Create a file `Backend/.env` with:
```env
KEYCLOAK_CLIENT_SECRET=<paste-client-secret-from-step-1>
KEYCLOAK_ADMIN_CLIENT_SECRET=<paste-client-secret-from-step-1>
KEYCLOAK_ADMIN_USERNAME=admin
KEYCLOAK_ADMIN_PASSWORD=admin
```

#### Option B: Update application.yml Directly

Edit `Backend/src/main/resources/application.yml`:

```yaml
keycloak:
  credentials:
    secret: <paste-client-secret-here>
  
  admin:
    client-secret: <paste-client-secret-here>
    username: admin
    password: admin
```

**Note**: Option A is more secure as it keeps secrets out of version control.

### Step 4: Restart Backend

```bash
# Stop current backend (Ctrl+C)
cd Backend
mvn spring-boot:run
```

Or if using the background process:
```bash
# Stop
docker stop <backend-container>
# Start
docker start <backend-container>
```

### Step 5: Test

1. **Go to your application**
2. **Navigate to a page that fetches users** (e.g., admin dashboard, team page)
3. **Check if users load without 401 error**

## Verification

### Check Backend Logs

Look for successful Keycloak connection:
```
INFO  [org.keycloak.adapters] - Keycloak is using a per-deployment configuration.
INFO  [org.keycloak.admin] - Successfully connected to Keycloak Admin API
```

### Test API Endpoint

Using curl or browser:
```bash
curl -H "Authorization: Bearer <your-token>" http://localhost:8081/api/keycloak/users
```

Should return user list instead of 401 error.

## Alternative: Use Master Realm Admin

If you prefer not to use service account, you can use master realm admin credentials:

In `application.yml`:
```yaml
keycloak:
  admin:
    server-url: http://localhost:8090
    realm: master  # Use master realm
    username: admin
    password: admin
    # Don't use client-id and client-secret for username/password auth
```

**Note**: This is less secure but simpler for development.

## Troubleshooting

### Still Getting 401 After Configuration

1. **Verify client secret is correct:**
   - Go to Keycloak → Clients → insa-backend → Credentials
   - Regenerate secret if needed
   - Update backend configuration

2. **Verify service account has roles:**
   - Go to Keycloak → Clients → insa-backend → Service account roles
   - Should see realm-management roles assigned

3. **Check backend logs for detailed error:**
   ```bash
   # Look for Keycloak-related errors
   tail -f Backend/logs/spring.log
   ```

### Client Secret Not Working

If the client secret from the realm template doesn't work:

1. **Regenerate the secret:**
   - Keycloak → Clients → insa-backend → Credentials
   - Click **"Regenerate"**
   - Copy new secret
   - Update backend configuration

2. **Restart backend** with new secret

## Security Notes

### For Production:

1. **Use environment variables** - Never commit secrets to git
2. **Use service account** - More secure than username/password
3. **Limit permissions** - Only assign necessary roles
4. **Rotate secrets regularly** - Change client secrets periodically
5. **Use HTTPS** - Enable SSL for Keycloak in production

### For Development:

Current setup is fine for local development, but remember:
- Don't commit `.env` file
- Don't commit secrets in `application.yml`
- Use `.gitignore` to exclude sensitive files

## Files to Update

1. `Backend/.env` (create this file) - **Recommended**
   OR
2. `Backend/src/main/resources/application.yml` - **Not recommended for secrets**

## Summary

The backend needs:
1. ✅ Client secret for `insa-backend` client
2. ✅ Service account with admin roles
3. ✅ Environment variables configured
4. ✅ Backend restarted

After completing these steps, the 401 error should be resolved.

---

**Date**: May 2, 2026
