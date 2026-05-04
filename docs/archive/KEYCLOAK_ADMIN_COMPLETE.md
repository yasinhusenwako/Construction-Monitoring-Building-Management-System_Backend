# Keycloak Admin API Integration - Complete Solution

## Issue Summary
The backend was unable to fetch users from Keycloak, returning `HTTP 401 Unauthorized` errors when calling the Admin API.

## Root Cause
Spring Boot does not automatically load `.env` files. The `KEYCLOAK_ADMIN_CLIENT_SECRET` environment variable was empty, causing service account authentication to fail.

## Complete Solution

### 1. Added spring-dotenv Dependency
**File:** `Backend/pom.xml`

```xml
<dependency>
    <groupId>me.paulschwarz</groupId>
    <artifactId>spring-dotenv</artifactId>
    <version>4.0.0</version>
</dependency>
```

This library automatically loads environment variables from `.env` file when Spring Boot starts.

### 2. Environment Variables Configuration
**File:** `Backend/.env`

```env
KEYCLOAK_CLIENT_SECRET=6krziITC6UadIt5iTsKVuNZ5I976OwkM
KEYCLOAK_ADMIN_CLIENT_SECRET=6krziITC6UadIt5iTsKVuNZ5I976OwkM
KEYCLOAK_ADMIN_USERNAME=admin
KEYCLOAK_ADMIN_PASSWORD=admin
```

### 3. Application Configuration
**File:** `Backend/src/main/resources/application.yml`

```yaml
keycloak:
  admin:
    server-url: http://localhost:8090
    realm: insa
    client-id: insa-backend
    client-secret: ${KEYCLOAK_ADMIN_CLIENT_SECRET:}
    username: ${KEYCLOAK_ADMIN_USERNAME:}
    password: ${KEYCLOAK_ADMIN_PASSWORD:}
```

### 4. Keycloak Admin Client Configuration
**File:** `Backend/src/main/java/com/org/cmbms/config/KeycloakConfig.java`

Uses service account (client credentials) authentication:

```java
@Bean
public Keycloak keycloakAdminClient() {
    return KeycloakBuilder.builder()
        .serverUrl(serverUrl)
        .realm(realm)
        .clientId(clientId)
        .clientSecret(clientSecret)
        .grantType("client_credentials")
        .build();
}
```

## Keycloak Configuration Requirements

### Client Configuration (insa-backend)
In Keycloak Admin Console → Clients → insa-backend:

**Settings Tab:**
- Client authentication: ON
- Service accounts enabled: ON
- Authorization enabled: ON
- Standard flow enabled: ON
- Direct access grants enabled: ON

**Credentials Tab:**
- Client Secret: `6krziITC6UadIt5iTsKVuNZ5I976OwkM`

**Service Account Roles Tab:**
Assign these roles from `realm-management`:
- ✅ view-realm
- ✅ view-users
- ✅ query-users
- ✅ query-groups
- ✅ manage-users

## Verification Steps

### 1. Test Service Account Authentication
```powershell
$body = @{
    grant_type='client_credentials'
    client_id='insa-backend'
    client_secret='6krziITC6UadIt5iTsKVuNZ5I976OwkM'
}
Invoke-RestMethod -Uri 'http://localhost:8090/realms/insa/protocol/openid-connect/token' `
    -Method Post -Body $body -ContentType 'application/x-www-form-urlencoded'
```

Expected: Returns access token with 300 second expiry.

### 2. Check Backend Logs
Look for these log messages on startup:
```
INFO com.org.cmbms.config.KeycloakConfig : Initializing Keycloak Admin Client
INFO com.org.cmbms.config.KeycloakConfig : Server URL: http://localhost:8090
INFO com.org.cmbms.config.KeycloakConfig : Realm: insa
INFO com.org.cmbms.config.KeycloakConfig : Client ID: insa-backend
INFO com.org.cmbms.config.KeycloakConfig : Using service account authentication
INFO com.org.cmbms.config.KeycloakConfig : Keycloak Admin Client initialized successfully
```

### 3. Test from Frontend
Login as admin user and navigate to user management page. The page should load users from Keycloak without errors.

## API Endpoints

### Get All Users
```
GET /api/keycloak/users
Authorization: Bearer <JWT_TOKEN>
Role Required: ADMIN
```

### Get User by ID
```
GET /api/keycloak/users/{userId}
Authorization: Bearer <JWT_TOKEN>
Role Required: ADMIN
```

### Create User
```
POST /api/keycloak/users
Authorization: Bearer <JWT_TOKEN>
Role Required: ADMIN
Content-Type: application/json

{
  "username": "user@example.com",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "password": "Password@123",
  "roles": ["USER"],
  "phone": "+251911234567",
  "department": "IT",
  "divisionId": "1",
  "profession": "Developer"
}
```

### Update User
```
PUT /api/keycloak/users/{userId}
Authorization: Bearer <JWT_TOKEN>
Role Required: ADMIN
Content-Type: application/json

{
  "email": "newemail@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "enabled": true,
  "roles": ["USER", "SUPERVISOR"]
}
```

### Delete User
```
DELETE /api/keycloak/users/{userId}
Authorization: Bearer <JWT_TOKEN>
Role Required: ADMIN
```

### Reset Password
```
POST /api/keycloak/users/{userId}/reset-password
Authorization: Bearer <JWT_TOKEN>
Role Required: ADMIN
Content-Type: application/json

{
  "password": "NewPassword@123",
  "temporary": false
}
```

### Toggle User Status
```
PATCH /api/keycloak/users/{userId}/status?enabled=true
Authorization: Bearer <JWT_TOKEN>
Role Required: ADMIN
```

### Get Available Roles
```
GET /api/keycloak/users/roles
Authorization: Bearer <JWT_TOKEN>
Role Required: ADMIN
```

## Troubleshooting

### Issue: Still getting 401 errors
**Solution:**
1. Verify `.env` file exists in `Backend/` directory
2. Check that `spring-dotenv` dependency is in `pom.xml`
3. Rebuild: `mvn clean install -DskipTests`
4. Restart backend: `mvn spring-boot:run`

### Issue: Service account has no permissions
**Solution:**
1. Go to Keycloak Admin Console
2. Navigate to: Clients → insa-backend → Service Account Roles
3. Assign roles from `realm-management` client
4. Required roles: view-realm, view-users, query-users, manage-users

### Issue: Client secret mismatch
**Solution:**
1. Go to Keycloak Admin Console
2. Navigate to: Clients → insa-backend → Credentials
3. Copy the Client Secret
4. Update `Backend/.env` with the correct secret
5. Restart backend

## Status
✅ **RESOLVED** - Keycloak Admin API is fully functional

## Date
May 2, 2026

## Related Files
- `Backend/pom.xml` - Added spring-dotenv dependency
- `Backend/.env` - Environment variables
- `Backend/src/main/resources/application.yml` - Application configuration
- `Backend/src/main/java/com/org/cmbms/config/KeycloakConfig.java` - Keycloak client configuration
- `Backend/src/main/java/com/org/cmbms/user/service/KeycloakAdminService.java` - Admin service
- `Backend/src/main/java/com/org/cmbms/user/controller/KeycloakUserController.java` - REST endpoints
