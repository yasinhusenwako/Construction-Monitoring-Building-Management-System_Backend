# CSBMS Backend

Spring Boot backend API for the INSA Construction Supervision and Building Management System.

## Technology Stack

- **Framework:** Spring Boot 3.3.6
- **Language:** Java 17
- **Database:** PostgreSQL 14+
- **Authentication:** Keycloak (OAuth2/JWT)
- **ORM:** Spring Data JPA (Hibernate)
- **Build Tool:** Maven 3.8+

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL 14+
- Keycloak server running

### Database Setup

```sql
CREATE DATABASE cmbms;
-- Tables should be created via migrations or manual setup
```

### Configuration

Edit `src/main/resources/application.properties`:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/cmbms
spring.datasource.username=postgres
spring.datasource.password=${DB_PASSWORD}

# Keycloak
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8090/realms/insa
```

### Running

**Development:**

```bash
mvn spring-boot:run
```

**Production:**

```bash
mvn clean package
java -jar target/cmbms-backend-1.0.0.jar
```

**With Keycloak:**

```bash
# Windows
START_BACKEND_KEYCLOAK.bat

# Linux/Mac
./start-with-keycloak.sh
```

## Project Structure

```
src/main/java/com/org/cmbms/
├── auth/                 # Authentication & Security
│   ├── security/        # Security configuration
│   └── jwt/             # JWT token handling
├── common/              # Shared utilities
│   ├── enums/          # Enums (Status, Role, etc.)
│   ├── exception/      # Exception handling
│   └── util/           # Utility classes
├── project/             # Project Management Module
│   ├── controller/     # REST controllers
│   ├── service/        # Business logic
│   ├── repository/     # Data access
│   ├── model/          # Entity models
│   └── dto/            # Data transfer objects
├── space/               # Space Booking Module
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── model/
│   └── dto/
├── maintenance/         # Maintenance Module
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── model/
│   └── dto/
├── user/                # User Management
├── division/            # Division Management
├── workflow/            # Workflow Engine
├── notification/        # Notification Service
├── reporting/           # Reports & Analytics
└── file/                # File Upload/Download
```

## API Endpoints

### Authentication

- All endpoints require JWT token from Keycloak
- Token must be included in `Authorization: Bearer <token>` header

### Projects

- `GET /api/projects` - List all projects
- `POST /api/projects` - Create project
- `GET /api/projects/{id}` - Get project details
- `PATCH /api/projects/{id}` - Update project
- `DELETE /api/projects/{id}` - Delete project
- `PATCH /api/projects/{id}/approve` - Approve project
- `PATCH /api/projects/{id}/reject` - Reject project

### Bookings

- `GET /api/bookings` - List all bookings
- `POST /api/bookings` - Create booking
- `GET /api/bookings/{id}` - Get booking details
- `PATCH /api/bookings/{id}` - Update booking
- `DELETE /api/bookings/{id}` - Delete booking

### Maintenance

- `GET /api/maintenance` - List all maintenance requests
- `POST /api/maintenance` - Create maintenance request
- `GET /api/maintenance/{id}` - Get maintenance details
- `PATCH /api/maintenance/{id}` - Update maintenance
- `DELETE /api/maintenance/{id}` - Delete maintenance

### Admin

- `PATCH /api/admin/assign` - Assign request to division/supervisor
- `PATCH /api/admin/assign-professional` - Assign to professional
- `PATCH /api/admin/approve` - Approve request
- `PATCH /api/admin/reject` - Reject request
- `PATCH /api/admin/review` - Start review

### Users

- `GET /api/users` - List all users
- `POST /api/users` - Create user
- `GET /api/users/{id}` - Get user details
- `PATCH /api/users/{id}` - Update user
- `DELETE /api/users/{id}` - Delete user

### Reports

- `GET /api/reports/overview` - System overview
- `GET /api/reports/mttr` - Mean Time To Resolution
- `GET /api/reports/analytics` - Analytics data

## Database Schema

### Main Tables

- `projects` - Project requests
- `bookings` - Space bookings
- `maintenance_requests` - Maintenance requests
- `users` - User accounts (legacy)
- `divisions` - Organizational divisions
- `status_history` - Audit trail
- `notifications` - User notifications
- `file_records` - Uploaded files

### Key Relationships

- Projects/Bookings/Maintenance → Users (created_by, assigned_to)
- Maintenance → Divisions (division_id)
- All requests → Status History (audit trail)

## Security

### Authentication

- Keycloak OAuth2/JWT
- Token validation on every request
- Automatic token refresh

### Authorization

- Role-based access control (RBAC)
- Method-level security with `@PreAuthorize`
- Division-based data isolation

### Data Protection

- SQL injection prevention (JPA)
- Input validation
- CORS configuration
- CSRF protection

## Testing

### Run Tests

```bash
mvn test
```

### Run Specific Test

```bash
mvn test -Dtest=ProjectServiceTest
```

### Skip Tests

```bash
mvn clean package -DskipTests
```

## Troubleshooting

### Port 8081 already in use

```bash
# Windows
netstat -ano | findstr :8081
taskkill /F /PID <PID>

# Linux/Mac
lsof -ti:8081 | xargs kill -9
```

### Database connection failed

1. Verify PostgreSQL is running
2. Check credentials in `application.properties`
3. Ensure database `cmbms` exists
4. Check firewall settings

### Keycloak connection failed

1. Verify Keycloak is running on port 8090
2. Check realm configuration
3. Verify issuer URI in `application.properties`

## Configuration Properties

### Database

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/cmbms
spring.datasource.username=postgres
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
```

### Keycloak

```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8090/realms/insa
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8090/realms/insa/protocol/openid-connect/certs
```

### File Upload

```properties
file.upload-dir=./uploads
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
```

## Logging

### Log Levels

```properties
logging.level.root=INFO
logging.level.com.org.cmbms=DEBUG
logging.level.org.springframework.security=DEBUG
```

### Log Files

Logs are output to console by default. Configure file logging:

```properties
logging.file.name=logs/cmbms.log
logging.file.max-size=10MB
logging.file.max-history=30
```

## Documentation

See [SYSTEM_DOCUMENTATION.md](../SYSTEM_DOCUMENTATION.md) for complete system documentation.

---

**Port:** 8081  
**Framework:** Spring Boot 3.3.6  
**Java Version:** 17  
**Status:** Production Ready ✅
