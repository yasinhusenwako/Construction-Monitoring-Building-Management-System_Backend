# Keycloak Admin API 401 Error - FIXED

## Problem
Backend was getting `HTTP 401 Unauthorized` when trying to fetch users from Keycloak using the Admin API.

Error message:
```
Failed to fetch users from Keycloak: jakarta.ws.rs.NotAuthorizedException: HTTP 401 Unauthorized
```

## Root Cause
Spring Boot **does not automatically load `.env` files**. The `KEYCLOAK_ADMIN_CLIENT_SECRET` environment variable was empty, causing authentication to fail.

## Solution
Added `spring-dotenv` library to automatically load environment variables from `.env` file.

### Changes Made

1. **Added spring-dotenv dependency to `pom.xml`:**
```xml
<dependency>
    <groupId>me.paulschwarz</groupId>
    <artifactId>spring-dotenv</artifactId>
    <version>4.0.0</version>
</dependency>
```

2. **Verified `.env` file contains correct credentials:**
```env
KEYCLOAK_CLIENT_SECRET=6krziITC6UadIt5iTsKVuNZ5I976OwkM
KEYCLOAK_ADMIN_CLIENT_SECRET=6krziITC6UadIt5iTsKVuNZ5I976OwkM
```

3. **Configuration in `application.yml` references environment variables:**
```yaml
keycloak:
  admin:
    server-url: http://localhost:8090
    realm: insa
    client-id: insa-backend
    client-secret: ${KEYCLOAK_ADMIN_CLIENT_SECRET:}
```

4. **KeycloakConfig.java uses service account authentication:**
```java
Keycloak keycloak = KeycloakBuilder.builder()
    .serverUrl(serverUrl)
    .realm(realm)
    .clientId(clientId)
    .clientSecret(clientSecret)
    .grantType("client_credentials")
    .build();
```

## Verification
Tested with test endpoint `/api/test/keycloak/users-count`:
- ✅ Successfully connected to Keycloak
- ✅ Successfully fetched 4 users
- ✅ Service account authentication working

## Service Account Configuration
The `insa-backend` client in Keycloak has:
- ✅ Service Accounts Enabled: true
- ✅ Client Secret: `6krziITC6UadIt5iTsKVuNZ5I976OwkM`
- ✅ Service Account Roles assigned:
  - `view-realm`
  - `view-users`
  - `query-users`
  - `query-groups`
  - `manage-users`

## Status
✅ **RESOLVED** - Keycloak Admin API is now working correctly.

## Date
May 2, 2026
