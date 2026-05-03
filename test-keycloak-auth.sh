#!/bin/bash

# Test Keycloak Service Account Authentication
echo "Testing Keycloak Service Account Authentication..."
echo ""

CLIENT_ID="insa-backend"
CLIENT_SECRET="6krziITC6UadIt5iTsKVuNZ5I976OwkM"
KEYCLOAK_URL="http://localhost:8090"
REALM="insa"

echo "Configuration:"
echo "  Keycloak URL: $KEYCLOAK_URL"
echo "  Realm: $REALM"
echo "  Client ID: $CLIENT_ID"
echo "  Client Secret: ${CLIENT_SECRET:0:10}..."
echo ""

echo "Attempting to get access token using client credentials..."
echo ""

curl -X POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=$CLIENT_ID" \
  -d "client_secret=$CLIENT_SECRET" \
  -v

echo ""
echo ""
echo "If you see a 401 error, check:"
echo "1. Service Accounts are enabled for insa-backend client in Keycloak"
echo "2. Client secret matches the one in Keycloak Credentials tab"
echo "3. The client exists in the 'insa' realm"
