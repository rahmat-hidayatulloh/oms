#!/bin/bash
# Seed test data for benchmarking
# Creates 50 products and outputs the first product ID

BASE_URL="http://localhost:8080"

# Admin password wajib dari environment (GitHub Actions secret ADMIN_PASSWORD).
# Jangan pernah hardcode password di script ini.
if [ -z "${ADMIN_PASSWORD:-}" ]; then
  echo "ERROR: ADMIN_PASSWORD environment variable is not set." >&2
  echo "Exports it first, e.g.: export ADMIN_PASSWORD=<value-from-github-secret>" >&2
  exit 1
fi

TOKEN=$(curl -s -X POST "$BASE_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"${ADMIN_PASSWORD}\"}" | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

echo "Token obtained: ${TOKEN:0:20}..."

# Create 50 products
CATEGORIES=("CLOTHING" "ELECTRONICS" "BOOKS" "FOOD_AND_BEVERAGE" "ACCESSORIES" "OTHER")
FIRST_PRODUCT_ID=""

for i in $(seq 1 50); do
  CAT_INDEX=$(( (i - 1) % ${#CATEGORIES[@]} ))
  CATEGORY="${CATEGORIES[$CAT_INDEX]}"
  PRICE=$(( (i * 1000 + 50000) ))
  STOCK=$(( (i * 10 + 50) ))

  RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/products" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN" \
    -d "{
      \"name\": \"Benchmark Product $i\",
      \"description\": \"Test product for benchmarking - item number $i in category $CATEGORY\",
      \"price\": $PRICE.00,
      \"stock\": $STOCK,
      \"category\": \"$CATEGORY\"
    }")

  PRODUCT_ID=$(echo "$RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin).get('id',''))" 2>/dev/null)

  if [ -z "$FIRST_PRODUCT_ID" ] && [ -n "$PRODUCT_ID" ]; then
    FIRST_PRODUCT_ID="$PRODUCT_ID"
  fi

  if [ $((i % 10)) -eq 0 ]; then
    echo "Created $i products..."
  fi
done

echo ""
echo "Seed data created successfully!"
echo "First product ID: $FIRST_PRODUCT_ID"
echo "Total products: 50"
echo ""
echo "export TOKEN=\"$TOKEN\"" > /tmp/oms-benchmark-env.sh
echo "export FIRST_PRODUCT_ID=\"$FIRST_PRODUCT_ID\"" >> /tmp/oms-benchmark-env.sh
echo "Token and first product ID saved to /tmp/oms-benchmark-env.sh"