# HopOn

HopOn is a real-time ride-sharing app with:
- Android client (`/app`)
- Node.js + Express API (`/backend/api`)
- Admin web panel (`/backend/web`)

## Core Ride Flow

- Users can create or join rides.
- Creating a ride requires verification (`verified`).
- Ride visibility in discovery is joinable upcoming rides only:
  - `status = active`
  - `departure_time` is in the future
  - `available_seats > 0`
- Ride lifecycle:
  - `active -> in_progress -> completed`
  - or `active -> cancelled`

## Implemented Features

- Reports (ongoing/completed ride stages)
- Identity verification request/review flow (user + admin)
- Vehicle plate privacy (exposed only to host/confirmed passengers)
- Routes/Rides page with state tabs:
  - Ongoing, Upcoming, Completed, Cancelled
- One-active-ride constraints:
  - one active hosted ride per host
  - one active booking per passenger
- Cancellation cutoff (30 minutes before departure)
- Realtime cancellation updates via Socket.IO
- Passenger arrival + host boarded confirmation + host start-ride control

## Tech Stack

- Android: Kotlin, Jetpack Compose, Google Maps/Places
- API: Express, Mongoose, Socket.IO, JWT
- Admin web: Next.js

## Project Structure

- `app/`: Android app
- `backend/api/`: REST API + Socket.IO
- `backend/web/`: Admin dashboard

## Local Setup

### 1) Environment

- JDK 17+
- Android Studio + Android SDK
- Node.js 18+
- MongoDB connection string

### 2) Android keys (`local.properties`)

Do not commit this file.

```properties
sdk.dir=/PATH/TO/ANDROID/SDK
apiKey=YOUR_ANDROID_MAPS_KEY
routesApiKey=YOUR_ROUTES_KEY
```

### 3) Run backend API

```bash
cd backend/api
npm install
npm run dev
```

### 4) Run Android app

```bash
./gradlew :app:installDebug
```

## Socket Events (Key)

- `ride_cancelled`
- `passenger_arrived`
- `passenger_boarded`
- `passenger_left_behind`
- `ride_started`
- `verification_updated`

## Notes

- Root `README.md` is the primary tracked documentation file.
- Other Markdown files (`*.md`) are treated as local working notes and ignored by `.gitignore`.
