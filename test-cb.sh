#!/bin/bash

GATEWAY_URL="http://localhost:8090/api/trades"
RATE_LIMIT=${RATE_LIMIT_LIMIT_FOR_PERIOD:-2}
TIME_WINDOW=${RATE_LIMIT_REFRESH_PERIOD:-5}

echo "=========================================="
echo "Rate Limiting Test Script"
echo "=========================================="
echo "Configuration: $RATE_LIMIT requests per $TIME_WINDOW seconds"
echo "Gateway URL: $GATEWAY_URL"
echo ""

# Function to send a request and return status
send_request() {
  local request_num=$1
  local test_type=$2
  local trade_id="TEST-${test_type}-${request_num}"
  
  # Use -s for silent, -i to include headers, -w to append status code
  # Store output in temp variable
  local temp_file=$(mktemp)
  
  response=$(curl -s -i -w "\nHTTP_CODE:%{http_code}" -X POST "$GATEWAY_URL" \
    -H "Content-Type: application/json" \
    -d "{
      \"tradeId\": \"$trade_id\",
      \"version\": 1,
      \"counterPartyId\": \"CP-1\",
      \"bookId\": \"B1\",
      \"maturityDate\": \"2025-12-31\"
    }" 2>&1)
  
  # Extract HTTP status code from the HTTP_CODE line
  http_code=$(echo "$response" | grep "^HTTP_CODE:" | cut -d: -f2 | tr -d ' ')
  
  # If HTTP_CODE not found, try to extract from HTTP status line (e.g., "HTTP/1.1 200 OK")
  if [ -z "$http_code" ]; then
    http_code=$(echo "$response" | grep -E "^HTTP/[0-9]" | head -1 | awk '{print $2}')
  fi
  
  # Extract X-RateLimit-Remaining header (case-insensitive, remove leading space)
  remaining=$(echo "$response" | grep -i "^x-ratelimit-remaining:" | cut -d: -f2 | sed 's/^[[:space:]]*//' | tr -d '\r')
  
  # Clean up temp file if it exists
  [ -f "$temp_file" ] && rm -f "$temp_file"
  
  if [ "$http_code" == "429" ]; then
    echo "  ✗ Request $request_num: RATE LIMITED (429)"
  elif [ "$http_code" == "200" ]; then
    if [ -n "$remaining" ]; then
      echo "  ✓ Request $request_num: SUCCESS (200) - Remaining: $remaining"
    else
      echo "  ✓ Request $request_num: SUCCESS (200)"
    fi
  elif [ -z "$http_code" ] || [ "$http_code" == "000" ]; then
    echo "  ✗ Request $request_num: FAILED (No response - check if gateway is running on port 8090)"
  else
    echo "  ? Request $request_num: HTTP $http_code"
  fi
}

# ==========================================
# Test 1: Sequential Requests
# ==========================================
echo "TEST 1: Sequential Requests"
echo "Sending 5 requests one after another..."
echo "Expected: First $RATE_LIMIT should succeed, rest should be rate limited"
echo ""

for i in {1..5}; do
  send_request $i "SEQ"
  # Small delay to ensure requests are truly sequential
  sleep 0.2
done

echo ""
echo "Waiting $TIME_WINDOW seconds for rate limit window to reset..."
echo ""
sleep $TIME_WINDOW

# ==========================================
# Test 2: Parallel Requests
# ==========================================
echo "TEST 2: Parallel Requests"
echo "Sending 5 requests simultaneously..."
echo "Expected: First $RATE_LIMIT should succeed, rest should be rate limited"
echo ""

# Send 5 requests in parallel
for i in {1..5}; do
  (
    send_request $i "PAR"
  ) &
done

# Wait for all background jobs to complete
wait

echo ""
echo "=========================================="
echo "Test Complete"
echo "=========================================="
echo ""
echo "Summary:"
echo "  - Sequential: First $RATE_LIMIT requests should succeed, then rate limited"
echo "  - Parallel: First $RATE_LIMIT requests should succeed, then rate limited"
echo ""
echo "If you see 429 responses, rate limiting is working correctly!"
echo ""