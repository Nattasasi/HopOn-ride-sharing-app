#!/bin/bash
set -e

BASE="http://localhost:3001/api/v1"

echo "=== 1. Login (rider) ==="
LOGIN=$(curl -s -X POST "$BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"rider@hopon.test","password":"password123"}')
TOKEN=$(echo "$LOGIN" | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")
echo "Token obtained: ${TOKEN:0:50}..."

echo ""
echo "=== 2. GET /posts?lat=13.73&lng=100.52 ==="
POSTS=$(curl -s "$BASE/posts?lat=13.73&lng=100.52" -H "Authorization: Bearer $TOKEN")
echo "$POSTS" | python3 -c "
import sys, json
arr = json.load(sys.stdin)
if not isinstance(arr, list):
    print('  ERROR:', arr)
    sys.exit(1)
print('  Posts returned:', len(arr))
if arr:
    p = arr[0]
    drv = p.get('driver_id', {})
    print('  [0] _id          :', p.get('_id'))
    print('  [0] post_id      :', p.get('post_id'))
    print('  [0] driver._id   :', drv.get('_id') if isinstance(drv,dict) else drv)
    print('  [0] driver.name  :', drv.get('first_name','?') if isinstance(drv,dict) else '-', drv.get('last_name','') if isinstance(drv,dict) else '')
    print('  [0] avg_rating   :', drv.get('average_rating') if isinstance(drv,dict) else '-')
    print('  [0] start_lat/lng:', p.get('start_lat'), p.get('start_lng'))
    print('  [0] end_lat/lng  :', p.get('end_lat'), p.get('end_lng'))
    print('  [0] price_per_seat:', p.get('price_per_seat'))
    print('  [0] status       :', p.get('status'))
"

# grab the UUID post_id for booking test
POST_UUID=$(echo "$POSTS" | python3 -c "import sys,json; arr=json.load(sys.stdin); print(arr[0]['post_id']) if arr else print('')")

echo ""
echo "=== 3. POST /bookings (rider books first post) ==="
BOOKING=$(curl -s -X POST "$BASE/bookings" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{\"post_id\":\"$POST_UUID\"}")
echo "  Raw response: $BOOKING"
BOOKING_ID=$(echo "$BOOKING" | python3 -c "import sys,json; b=json.load(sys.stdin); print(b.get('_id','NO_ID'))")
echo "  Booking _id: $BOOKING_ID"
echo "  Is wrapped?:" $(echo "$BOOKING" | python3 -c "import sys,json; b=json.load(sys.stdin); print('YES - key:' + str(list(b.keys()))) if 'booking' in b else print('NO - bare object')")

echo ""
echo "=== 4. PATCH /bookings/:id/respond (driver accepts via _id) ==="
# login as driver
DRIVER_TOKEN=$(curl -s -X POST "$BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"driver@hopon.test","password":"password123"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")
RESPOND=$(curl -s -X PATCH "$BASE/bookings/$BOOKING_ID/respond" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $DRIVER_TOKEN" \
  -d '{"status":"accepted"}')
echo "  Respond result: $RESPOND"
echo "  Booking status:" $(echo "$RESPOND" | python3 -c "import sys,json; b=json.load(sys.stdin); print(b.get('status', b))")

echo ""
echo "=== All smoke tests done ==="
