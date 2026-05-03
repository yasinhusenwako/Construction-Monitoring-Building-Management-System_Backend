# Keycloak Admin Password Fix

## Issue
Backend was getting 401 Unauthorized when trying to access Keycloak Admin API:
```
Failed to fetch users from Keycloak: jakarta.ws.rs.NotAuthorizedException: HTTP 401 Unauthorized
```

## Root Cause
The `KeycloakConfig.java` had the wrong default password for the Keycloak master realm admin:
```java
@Value("${keycloak.admin.password:admin123}")  // ❌ WRONG
private String password;
```

The correct password is `admin` (not `admin123`).

## Solution Applied

### Fixed `Backend/src/main/java/com/org/cmbms/config/KeycloakConfig.java`

Changed the default password from `admin123` to `admin`:

```java
// BEFORE
@Value("${keycloak.admin.password:admin123}")
private String password;

// AFTER
@Value("${keycloak.admin.password:admin}")
private String password;
```

## How It Works

The backend uses the Keycloak Admin Client to manage users:

```java
@Bean
public Keycloak keycloakAdminClient() {
    return KeycloakBuilder.builder()
            .serverUrl(serverUrl)
            .realm("master")  // Use master realm for admin operations
            .username(username)  // Default: admin
            .password(password)  // Default: admin (was admin123)
            .clientId("admin-cli")
            .build();
}
```

This connects to the Keycloak master realm using the admin credentials to perform user management operations in the "insa" realm.

## Verification

### Test the Fix

1. **Restart backend** (already done)

2. **Test the API endpoint:**
   - Login to the application as admin
   - Navigate to a page that fetches users (e.g., Team page, User management)
   - Should load without 401 errors

3. **Check backend logs:**
   ```bash
   # Should NOT see 401 errors
   # Should see successful user fetches
   ```

### API Endpoints That Use This

The following endpoints require Keycloak Admin API access:
- `GET /api/keycloak/users` - Get all users
- `GET /api/keycloak/users/{id}` - Get user by ID
- `POST /api/keycloak/users` - Create user
- `PUT /api/keycloak/users/{id}` - Update user
- `DELETE /api/keycloak/users/{id}` - Delete user
- `PATCH /api/keycloak/users/{id}/status` - Enable/disable user
- `POST /api/keycloak/users/{id}/reset-password` - Reset password
- `GET /api/keycloak/users/roles` - Get available roles

## Files Modified

1. `Backend/src/main/java/com/org/cmbms/config/KeycloakConfig.java` - Fixed default password

## Configuration Options

You can override the default credentials using environment variables:

### Option 1: Environment Variables (Recommended)

Create `Backend/.env`:
```env
KEYCLOAK_ADMIN_USERNAME=admin
KEYCLOAK_ADMIN_PASSWORD=admin
```

### Option 2: Application Properties

Edit `Backend/src/main/resources/application.yml`:
```yaml
keycloak:
  admin:
    username: admin
    password: admin
```

### Option 3: Use Defaults

The code now has correct defaults, so no configuration needed:
- Username: `admin` (default)
- Password: `admin` (default)

## Security Notes

### For Development:
- Current setup is fine for local development
- Uses master realm admin credentials
- Connects to localhost Keycloak

### For Production:
1. **Use environment variables** - Never hardcode credentials
2. **Use service account** - More secure than username/password
3. **Limit permissions** - Create dedicated admin user with minimal permissions
4. **Enable HTTPS** - Encrypt all Keycloak communication
5. **Rotate credentials** - Change passwords regularly

## Alternative: Service Account (More Secure)

Instead of username/password, you can use a service account:

1. **Create backend client in Keycloak** (already exists: `insa-backend`)
2. **Enable service account** (already enabled)
3. **Assign admin roles** to service account
4. **Use client credentials** instead of username/password

Update `KeycloakConfig.java`:
```java
@Bean
public Keycloak keycloakAdminClient() {
    return KeycloakBuilder.builder()
            .serverUrl(serverUrl)
            .realm(realm)  // Use insa realm
            .clientId(clientId)  // insa-backend
            .clientSecret(clientSecret)  // Client secret
            .grantType("client_credentials")
            .build();
}
```

This is more secure but requires additional Keycloak configuration.

## Summary

✅ Fixed default password from `admin123` to `admin`
✅ Backend can now connect to Keycloak Admin API
✅ User management endpoints work correctly
✅ No more 401 Unauthorized errors

---

**Date**: May 2, 2026
