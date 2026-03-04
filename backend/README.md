# HopOn Backend + Web Technical README

This README documents the app starting from `/backend` only.

## 1) Project Structure

- `api/`: Express + MongoDB + Socket.IO server.
- `web/`: Next.js frontend (App Router) with React Query + Axios + Socket.IO client.
- `improvecarpool.postman_collection.json`: API collection for manual endpoint testing.

## 2) API Server (`/backend/api`)

### 2.1 Bootstrapping and Request Pipeline

Main file: `api/app.js`

Core boot sequence:
1. Creates Express app and HTTP server.
2. Creates Socket.IO server on the same HTTP server.
3. Applies global middleware in this order:
   - `cors({ origin: true, credentials: true })`
   - `express.json()`
   - `authMiddleware` (global; always sets `req.user` in demo mode)
4. Mounts REST route groups under `/api/v1/*`.
5. Registers Socket.IO handlers via `require('./socket')(io)`.
6. Applies `errorHandler` as terminal middleware.
7. Connects MongoDB via `mongoose.connect(process.env.MONGO_URI)`.
8. Starts server on `process.env.PORT || 5000`.

### 2.2 Route Groups and Endpoints

Base prefix: `/api/v1`

#### Auth Routes (`api/routes/auth.js`)
- `POST /auth/register` -> `authController.register`
- `POST /auth/login` -> `authController.login`
- `POST /auth/verify` -> `authController.verify`
- `POST /auth/refresh` -> `refreshTokenMiddleware`, then `authController.refresh`

#### Post Routes (`api/routes/posts.js`)
- `GET /posts` -> `postsController.getPosts`
- `GET /posts/me` -> `postsController.getMyPosts`
- `POST /posts` -> `postsController.createPost` (validator chain + handler)
- `GET /posts/:id` -> `postsController.getPost`
- `PATCH /posts/:id/status` -> `postsController.updatePostStatus`

#### Booking Routes (`api/routes/bookings.js`)
- `POST /bookings` -> `bookingsController.requestBooking`
- `GET /bookings/me` -> `bookingsController.getUserBookings`
- `GET /bookings/posts/:id` -> `bookingsController.getRideBookings`
- `PATCH /bookings/:id/respond` -> `bookingsController.respondToBooking`
- `PATCH /bookings/:id/cancel` -> `bookingsController.cancelBooking`

#### User Routes (`api/routes/users.js`)
- `GET /users/:id` -> `usersController.getUser`
- `PUT /users/:id` -> `usersController.updateUser` (validator chain + handler)
- `GET /users/:id/feedback` -> `usersController.getUserFeedback`

#### Admin Routes (`api/routes/admin.js`)
- `GET /admin/users` -> `adminController.getUsers`
- `PATCH /admin/users/:id/ban` -> `adminController.banUser`
- `GET /admin/posts` -> `adminController.getPosts`
- `DELETE /admin/posts/:id` -> `adminController.deletePost`
- `GET /admin/bookings` -> `adminController.getBookings`
- `PATCH /admin/bookings/:id/status` -> `adminController.updateBookingStatus`
- `GET /admin/dashboard` -> `adminController.getDashboard`

#### Tracking Routes (`api/routes/tracking.js`)
- `POST /tracking/:postId` -> `trackingController.updateTracking`
- `GET /tracking/:postId` -> `trackingController.getTracking`

#### Payments Routes (`api/routes/payments.js`)
- `POST /payments` -> `paymentsController.createPayment`
- `GET /payments/:bookingId` -> `paymentsController.getPayment`

#### Feedback Routes (`api/routes/feedback.js`)
- `POST /feedback` -> `feedbackController.createFeedback`
- `GET /feedback/users/:id/feedback` -> `feedbackController.getFeedback`

#### Emergency Routes (`api/routes/emergency.js`)
- `POST /emergency` -> `emergencyController.createEmergency`
- `GET /emergency` -> `emergencyController.getEmergencies`

#### Message Routes (`api/routes/messages.js`)
- `GET /messages/:postId` -> `messagesController.getMessages`

## 3) Core Controller Methods (Server Business Logic)

### 3.1 Authentication

`api/controllers/authController.js`
- `register`:
  - express-validator checks required fields (`first_name`, `last_name`, `email`, `dob`, `password`, `phone_number`, `role`).
  - Creates `User` with `password_hash = password` (hashing happens in User model pre-save hook).
  - Returns `{ userId, token, refreshToken }`.
- `login`:
  - Finds user by email.
  - Verifies password with `bcrypt.compare(password, user.password_hash)`.
  - Returns `{ userId, token, refreshToken }`.
- `verify`: placeholder returning `{ message: 'Verified' }`.
- `refresh`: creates a new access token from `req.user`.

### 3.2 Ride Posts

`api/controllers/postsController.js`
- `getPosts`:
  - If `lat` and `lng` are present, runs Mongo `$geoNear` on `end_location` with optional `radius`.
  - Otherwise returns all posts populated with driver profile fields.
- `createPost` (validator chain + handler):
  - Validates required coordinates, location names, departure time, seats, pricing.
  - Stores both coordinate fields and GeoJSON points (`location`, `end_location`).
  - Sets `driver_id = req.user.id`, `available_seats = total_seats`.
- `getPost`:
  - Fetches by Mongo `_id` (`req.params.id`) and populates driver.
- `getMyPosts`:
  - Fetches posts by `driver_id = req.user.id`.
- `updatePostStatus`:
  - Updates status and if set to `completed`, creates pending feedback records for driver<->passenger pairs from confirmed bookings.

### 3.3 Bookings

`api/controllers/bookingsController.js`
- `requestBooking`:
  - Reads `post_id` (UUID-style field) and seats.
  - Blocks self-booking and overbooking.
  - Prevents duplicate active booking states.
  - Creates booking with status `pending`.
- `respondToBooking`:
  - Driver-only check by comparing `post.driver_id` with `req.user.id`.
  - Accept sets booking to `confirmed` and decrements available seats.
  - Reject sets booking to `rejected`.
- `cancelBooking`:
  - Allows passenger or driver cancellation.
  - If cancelling a confirmed booking, returns seats to the post.
- `getUserBookings`:
  - Returns passenger bookings for current user and manually populates each post.
- `getRideBookings`:
  - Returns all bookings for a post UUID and populates passenger details.

### 3.4 Admin

`api/controllers/adminController.js`
- `getUsers`: optional search by name/email regex.
- `banUser`: toggles `is_banned`.
- `getPosts`: optional status filter.
- `deletePost`: deletes post and all related bookings.
- `getBookings`: global booking list with passenger+driver data.
- `updateBookingStatus`: arbitrary status patch.
- `getDashboard`: KPIs (`activeRides`, `inProgressRides`, `totalUsers`, `openAlerts`).

### 3.5 Users, Feedback, Messages, Payments, Tracking, Emergency

- `usersController.getUser`: profile by id (excluding password hash).
- `usersController.updateUser`: partial update with validation.
- `usersController.getUserFeedback`: feedback for user.

- `feedbackController.createFeedback`:
  - Requires post status to be `completed`.
  - Saves feedback and recalculates `average_rating` for reviewee.
- `feedbackController.getFeedback`: feedback list by user id.

- `messagesController.getMessages`: sorted chat history by post.

- `paymentsController.createPayment` / `getPayment`: create and fetch booking payment.

- `trackingController.updateTracking` / `getTracking`: append and fetch latest location/ETA logs.

- `emergencyController.createEmergency` / `getEmergencies`: create and fetch unresolved alerts.

## 4) Socket.IO Core Methods and Events

File: `api/socket.js`

Server-side events:
- `join_post`:
  - Verifies JWT from payload.
  - Allows room join if user is post driver or confirmed passenger.
  - Room format: `post_<post_id>`.
- `send_message`:
  - Persists message.
  - Emits `new_message` to `post_<post_id>` room.
- `join_admin`:
  - Verifies token and joins `admin` room only if role is `admin`.

Notes:
- `trackingController.updateTracking` and `emergencyController.createEmergency` accept `io` argument but routes do not inject `io`; real-time emits in these controllers require refactor for reliable delivery.

## 5) Data Models and Core Schema Behavior

### 5.1 Important Schemas

- `User`: identity, role, verification, ban status, average rating.
- `CarpoolPost`: route metadata, departure, seat capacity, pricing, status.
- `Booking`: links passenger to post with seats and status.
- `Message`: per-post chat messages.
- `RidesTracking`: per-post location logs.
- `EmergencyAlert`: live alert records.
- `Feedback`: rider/driver ratings and comments.
- `Payment`: booking payment records.

### 5.2 Model-Level Middleware (Mongoose)

- `User` model defines `pre('save')` middleware to hash `password_hash` with bcrypt when changed.
- `CarpoolPost` defines geospatial and query indexes:
  - `location: 2dsphere`
  - `end_location: 2dsphere`
  - `{ departure_time: 1, status: 1, driver_id: 1 }`

## 6) Frontend Pages (`/backend/web/src/app`) and What They Use

### 6.1 App-Level Providers

- `layout.tsx` wraps app with:
  - `QueryClientProvider` for data fetching/caching.
  - `AuthProvider` for local session state.

### 6.2 Authentication/UI Shell

- `/` (`page.tsx`): landing + login modal.
- `components/LoginModal.tsx`:
  - Calls `POST /api/v1/auth/login`.
  - On success stores `userId` + `token` via `AuthContext.login`.
- `components/AuthContext.tsx`:
  - Core methods: `login(id, token)`, `logout()`, `useAuth()`.
  - Reads/writes `localStorage` keys `userId` and `token`.

### 6.3 Main User Pages

- `/home`: dashboard shell and navigation links.
- `/posts`:
  - Calls `GET /api/v1/posts` via React Query.
  - Displays map routes (`RouteDisplay`) + post list.
- `/posts/create`:
  - Form submission to `POST /api/v1/posts` with Bearer token.
- `/posts/new`:
  - Lightweight map-based creation, also calls `POST /api/v1/posts`.
- `/posts/[id]`:
  - Calls `GET /api/v1/posts/:id` and `GET /api/v1/messages/:postId`.
  - Socket events: `join_post`, `send_message`, receives `new_message`, `location_update`.
  - Emergency action posts to `POST /api/v1/emergency`.
- `/profile/[id]`:
  - Calls `GET /api/v1/users/:id` and `GET /api/v1/users/:id/feedback`.
- `/settings`:
  - Calls `GET /api/v1/users/:id` and `PUT /api/v1/users/:id`.

### 6.4 Admin Pages

- `/admin/dashboard`:
  - Calls `GET /api/v1/admin/dashboard`.
  - Joins `admin` socket room and listens for `emergency`.
- `/admin/users`:
  - Calls `GET /api/v1/admin/users?search=...`.
  - Calls `PATCH /api/v1/admin/users/:id/ban`.
- `/admin/posts`:
  - Calls `GET /api/v1/admin/posts?status=...`.
  - Calls `DELETE /api/v1/admin/posts/:id`.
- `/admin/bookings`:
  - Calls `GET /api/v1/admin/bookings`.
  - Calls `PATCH /api/v1/admin/bookings/:id/status`.
- `/admin/emergency`:
  - Socket-based emergency list (no persistence endpoint call in current page).
- `/admin/feedback`:
  - Placeholder UI.

### 6.5 Core Frontend HTTP Helper

File: `web/src/lib/axios.ts`
- Creates Axios instance with `baseURL: http://localhost:5000`.
- Request interceptor reads `localStorage.token` and injects `Authorization: Bearer <token>`.

## 7) Middleware Analysis (Direct Answer)

Yes, middleware exists in this app.

### 7.1 Express Middleware

- `authMiddleware` (`api/middleware/auth.js`):
  - Applied globally with `app.use(authMiddleware)`.
  - Parses JWT from `Authorization` header.
  - In current demo mode, invalid/missing token does not block requests.
  - Fallback user is forced to `{ id: 'demo-user-id', role: 'admin' }`.

- `refreshTokenMiddleware` (`api/middleware/auth.js`):
  - Used only on `POST /api/v1/auth/refresh`.
  - Currently no-op (`next()` only).

- `errorHandler` (`api/middleware/error.js`):
  - Final error middleware returning HTTP 500 JSON.

- `role` middleware (`api/middleware/role.js`):
  - Exists but is not mounted in any route.
  - Currently no-op and allows all requests.

### 7.2 Model Middleware

- `User` model `pre('save')` hook hashes passwords.

### 7.3 Client-Side Interceptor Middleware

- Axios request interceptor appends JWT for browser requests.

### 7.4 Next.js Middleware File

- No `web/src/middleware.ts` (or equivalent) found.
- Route protection on frontend is handled inside client components (`useAuth` + redirects), not with Next middleware.

## 8) Notable Implementation Gaps to Be Aware Of

- API auth/role enforcement is effectively bypassed due to demo middleware defaults.
- Some controllers mix Mongo `_id` and custom UUID fields (`post_id`), which can cause integration confusion.
- Real-time emit flow from REST controllers needing `io` injection is incomplete.
- Frontend uses both custom Axios instance and raw `axios` import in different pages, causing inconsistent baseURL/auth behavior.

