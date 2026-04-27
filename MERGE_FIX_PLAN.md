# Backend Merge Fix Plan

**Date**: April 27, 2026  
**Issue**: brexman1 merge caused 100+ compilation errors  
**Root Causes**:
1. Lombok dependency removed from pom.xml
2. Duplicate `update()` methods in controllers
3. Missing getter/setter methods across all DTOs and models

## Critical Issues

### 1. Missing Lombok Dependency
**Impact**: All `@Data`, `@Getter`, `@Setter`, `@Builder` annotations are not working  
**Affected**: All DTOs, Models, and entities  
**Fix**: Add Lombok dependency back to pom.xml

### 2. Duplicate Update Methods
**Files with duplicates**:
- `ProjectController.java` - Lines 40 & 119 ✅ FIXED
- `SpaceController.java` - Lines 32 & 98
- `MaintenanceController.java` - Line 67
- `SpaceService.java` - Line 381
- `MaintenanceService.java` - Line 165

**Fix**: Remove duplicate `@PutMapping` methods, keep only `@PatchMapping` versions

### 3. Missing Getters/Setters (Lombok Issue)
**Affected Classes** (100+ errors):
- `User` model - missing getId(), getName(), getEmail(), getPassword(), getRole(), getDivisionId()
- `AuthResponse` DTO - missing getToken()
- `RegisterRequest` DTO - missing all getters
- `LoginRequest` DTO - missing getEmail(), getPassword()
- `ForgotPasswordRequest` DTO - missing getEmail()
- `UserPrincipal` - missing getId(), getRole(), getDivisionId()
- `FileRecord` model - missing all getters/setters
- `RequestHistory` model - missing all setters
- `AdminAssignRequest` DTO - missing all getters
- `AdminAssignProfessionalRequest` DTO - missing all getters
- `AssignGenericRequest` DTO - missing getRequestId()
- `AdminReviewRequest` DTO - missing getRequestType(), getRequestId()
- `MaintenanceRequest` model - missing getId()
- `Project` model - missing getId()
- `Booking` model - missing getId()

## Fix Strategy

### Phase 1: Add Lombok Dependency ⏭️
1. Add Lombok to pom.xml
2. Run `mvn clean compile` to verify

### Phase 2: Remove Duplicate Methods ⏭️
1. SpaceController - remove @PutMapping update
2. MaintenanceController - remove duplicate update
3. SpaceService - remove duplicate update
4. MaintenanceService - remove duplicate update

### Phase 3: Verify Lombok Annotations ⏭️
Check that all affected classes have proper Lombok annotations:
- Models: `@Data` or `@Getter @Setter`
- DTOs: `@Data` or `@Getter @Setter`
- Builders: `@Builder` where needed

### Phase 4: Test Compilation ⏭️
Run `mvn clean compile -DskipTests` to verify all errors resolved

## Estimated Errors
- **Total**: 100 compilation errors
- **Duplicate methods**: 4 errors
- **Missing Lombok**: 96 errors

## Priority
🔴 **CRITICAL** - Backend cannot compile or run until fixed
