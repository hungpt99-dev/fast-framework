#!/bin/bash
# Verify features on running server

BASE_URL="http://localhost:8080/api/orders"
REQ_ID=$(uuidgen)

echo "1. Creating Order (Idempotency Key: $REQ_ID)..."
curl -v -X POST $BASE_URL \
  -H "Content-Type: application/json" \
  -d "{
    \"requestId\": \"$REQ_ID\",
    \"customerId\": \"CUST-1\",
    \"total\": 100.00
  }"
echo -e "\n"

echo "2. Re-sending same request (Idempotency check)..."
curl -v -X POST $BASE_URL \
  -H "Content-Type: application/json" \
  -d "{
    \"requestId\": \"$REQ_ID\",
    \"customerId\": \"CUST-1\",
    \"total\": 100.00
  }"
echo -e "\n"

echo "3. Fetching Order (Cache Miss)..."
curl -v -X GET "$BASE_URL?customerId=CUST-1"
echo -e "\n"

echo "4. Validation Failure Check (Negative Total)..."
curl -v -X POST $BASE_URL \
  -H "Content-Type: application/json" \
  -d "{
    \"requestId\": \"$(uuidgen)\",
    \"customerId\": \"CUST-1\",
    \"total\": -50.00
  }"
echo -e "\n"
