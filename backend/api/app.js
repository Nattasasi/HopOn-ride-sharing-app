const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
const http = require('http');
const { Server } = require('socket.io');
const { authMiddleware } = require('./middleware/auth');
const errorHandler = require('./middleware/error');
require('dotenv').config();

const app = express();
const server = http.createServer(app);
const io = new Server(server, {
  cors: {
    origin: '*',
  }
});

app.use(cors({ origin: true, credentials: true }));
app.use(express.json());

// Connect to MongoDB
mongoose.connect(process.env.MONGO_URI)
  .then(() => console.log('MongoDB connected'))
  .catch(err => console.error('MongoDB connection error:', err));

// Routes
// Apply authMiddleware globally for demo to populate req.user but never block
app.use(authMiddleware);

app.use('/api/v1/auth', require('./routes/auth'));
app.use('/api/v1/posts', require('./routes/posts'));
app.use('/api/v1/bookings', require('./routes/bookings'));
app.use('/api/v1/users', require('./routes/users'));
app.use('/api/v1/admin', require('./routes/admin'));
app.use('/api/v1/tracking', require('./routes/tracking'));
app.use('/api/v1/payments', require('./routes/payments'));
app.use('/api/v1/feedback', require('./routes/feedback'));
app.use('/api/v1/emergency', require('./routes/emergency'));
app.use('/api/v1/messages', require('./routes/messages'));

// Socket.io setup
require('./socket')(io);

app.use(errorHandler);

const PORT = process.env.PORT || 5000;
server.listen(PORT, () => console.log(`Server running on port ${PORT}`));