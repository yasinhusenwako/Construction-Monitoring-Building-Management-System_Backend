# INSA BuildMS Backend (Spring Boot)

## Frontend integration (Next.js)

Set frontend environment:

```bash
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

Set backend environment:

```bash
DB_HOST=localhost
DB_PORT=3306
DB_NAME=cmbms
DB_USERNAME=root
DB_PASSWORD=root
JWT_SECRET=ThisIsA32ByteMinimumSecretKeyForCMBMSJwtToken123456
JWT_EXPIRATION_MS=86400000
UPLOAD_DIR=uploads
SERVER_PORT=8080
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://127.0.0.1:3000
```

## Run

```bash
mvn -q -DskipTests compile
mvn -q test
mvn spring-boot:run
```

## Contract notes

- Auth endpoints:
  - `POST /api/auth/register`
  - `POST /api/auth/login`
  - `POST /api/auth/forgot-password`
- Admin/Supervisor/Professional workflow endpoints preserved under `/api/admin`, `/api/supervisor`, `/api/professional`.
- Status values serialized in frontend-friendly labels (`Submitted`, `Under Review`, ...).
- Backward compatibility aliases supported for legacy frontend payload keys like `fullName` and `maintenanceRequestId`.
