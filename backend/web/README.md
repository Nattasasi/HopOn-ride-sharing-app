# Carpool Platform

## Setup Steps
1. Clone repo
2. cd api && npm install express mongoose cors socket.io jsonwebtoken bcryptjs express-validator uuid firebase-admin node-geocoder (for geocoding)
3. cd ../web && npm install
4. Copy .env.example to .env and fill keys
5. npm run start in api (nodemon app.js)
6. npm run dev in web

## Seed Script
In api, create seed.js:
const mongoose = require('mongoose');
// Connect and insert sample data

node seed.js

## Swift Integration Notes
Use URLSession for API calls.

- GET /posts: Query params lat, lng, radius. Response: [{end_location_name, start_location_name, distance, departure_time, available_seats, total_seats, driver: {first_name}}]

- POST /bookings: Body {post_id}. Response: {booking_id, status}

- POST /tracking/:postId: Body {current_lat, current_lng, eta_minutes}

- GET /messages/:postId: Response: [{message_id, sender_id, body, sent_at}]

Socket.io events:
- location_update: {current_lat, current_lng, eta_minutes}
- new_message: {message_id, sender_id, body}
- booking_confirmed: {booking}
- waitlist_promoted: {booking}

FCM push payload: {notification: {title: 'Title', body: 'Body'}, data: {key: 'value'}}