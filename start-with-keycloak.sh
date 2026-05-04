#!/bin/bash

echo "=========================================="
echo "Starting Backend with Keycloak Profile"
echo "=========================================="
echo ""
echo "This will start the backend with Keycloak authentication."
echo "Look for this line in the logs:"
echo "  'The following profiles are active: keycloak'"
echo ""
echo "=========================================="
echo ""

mvn spring-boot:run "-Dspring-boot.run.profiles=keycloak"
