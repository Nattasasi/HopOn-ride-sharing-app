const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
const http = require('http');
const { Server } = require('socket.io');
const { authMiddleware } = require('./middleware/auth');
const errorHandler = require('./middleware/error');
const {
  buildExpressCorsOptions,
  buildSocketCorsOptions,
  securityHeaders,
  apiRateLimiter,
  authRateLimiter
} = require('./middleware/security');
const {
  correlationIdMiddleware,
  requestLogger
} = require('./middleware/observability');
require('dotenv').config();

const app = express();
const server = http.createServer(app);
const io = new Server(server, {
  cors: buildSocketCorsOptions()
});
app.set('io', io);

app.use(correlationIdMiddleware);
app.use(requestLogger);
app.use(cors(buildExpressCorsOptions()));
app.use(securityHeaders);
app.use(apiRateLimiter);
app.use(express.json({ limit: '15mb' }));
app.use(express.urlencoded({ extended: true, limit: '15mb' }));

const { startExpireRidesJob } = require('./services/expireRides');

// Connect to MongoDB
mongoose.connect(process.env.MONGO_URI)
  .then(() => {
    console.log('MongoDB connected');
    startExpireRidesJob(io);
  })
  .catch(err => console.error('MongoDB connection error:', err));

// Routes
app.use('/api/v1/auth', authRateLimiter, require('./routes/auth'));
app.use(authMiddleware);
app.use('/api/v1/posts', require('./routes/posts'));
app.use('/api/v1/bookings', require('./routes/bookings'));
app.use('/api/v1/users', require('./routes/users'));
app.use('/api/v1/admin', require('./routes/admin'));
app.use('/api/v1/tracking', require('./routes/tracking'));
app.use('/api/v1/payments', require('./routes/payments'));
app.use('/api/v1/feedback', require('./routes/feedback'));
app.use('/api/v1/emergency', require('./routes/emergency'));
app.use('/api/v1/messages', require('./routes/messages'));
app.use('/api/v1/reports', require('./routes/reports'));
app.use('/api/v1/metrics', require('./routes/metrics'));

// Socket.io setup
require('./socket')(io);

app.use(errorHandler);

const PORT = process.env.PORT || 5000;
server.listen(PORT, () => console.log(`Server running on port ${PORT}`));
