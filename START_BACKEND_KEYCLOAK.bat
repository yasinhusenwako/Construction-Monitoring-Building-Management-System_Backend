@echo off
echo ========================================
echo Starting Backend with Keycloak Profile
echo ========================================
echo.
echo IMPORTANT: Look for these startup messages:
echo   1. "The following 1 profile is active: keycloak"
echo   2. "Started CmbmsApplication in X seconds"
echo.
echo If logs keep repeating endlessly:
echo   - Press Ctrl+C to stop
echo   - Check if PostgreSQL is running (port 5432)
echo   - Check if Keycloak is running (docker ps)
echo   - See Backend/FIX_BACKEND_LOOPING.md for help
echo.
echo Logging has been reduced to INFO level to minimize output.
echo.
pause
echo.
echo Starting...
echo.
mvn spring-boot:run "-Dspring-boot.run.profiles=keycloak"
