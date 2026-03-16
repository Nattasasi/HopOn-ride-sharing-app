const jwt = require('jsonwebtoken');
const mongoose = require('mongoose');
const Booking = require('./models/Booking');
const Message = require('./models/Message');
const CarpoolPost = require('./models/CarpoolPost');
const { v4: uuidv4 } = require('uuid');
const { createSocketEventRateGate } = require('./middleware/security');
const { incrementCounter } = require('./middleware/metrics');

const isJoinPostAllowed = createSocketEventRateGate({
  windowMs: 60 * 1000,
  max: Number(process.env.SOCKET_JOIN_RATE_LIMIT_MAX || 30)
});
const isSendMessageAllowed = createSocketEventRateGate({
  windowMs: 60 * 1000,
  max: Number(process.env.SOCKET_SEND_RATE_LIMIT_MAX || 120)
});

module.exports = (io) => {
  io.on('connection', (socket) => {
    console.info(JSON.stringify({
      level: 'info',
      type: 'socket_connected',
      socketId: socket.id
    }));

    socket.on('join_post', async (data) => {
      if (!isJoinPostAllowed(`join_post:${socket.id}`)) {
        incrementCounter('socketJoinFailures');
        socket.emit('rate_limit', { event: 'join_post' });
        console.warn(JSON.stringify({
          level: 'warn',
          type: 'socket_join_post_rate_limited',
          socketId: socket.id
        }));
        return;
      }

      const { token, post_id } = data;
      try {
        const decoded = jwt.verify(token, process.env.JWT_SECRET);
        const userId = decoded.id;

        // Persist userId on the socket for send_message
        socket.userId = userId;

        // Accept both MongoDB _id and UUID post_id, then normalize to Mongo _id room.
        const post = mongoose.Types.ObjectId.isValid(post_id)
          ? await CarpoolPost.findById(post_id)
          : await CarpoolPost.findOne({ post_id });
        if (!post) return socket.disconnect();

        const postMongoId = post._id.toString();
        const roomName = `post_${postMongoId}`;
        socket.currentPostId = postMongoId;
        socket.currentPostRoom = roomName;

        // Check if driver or confirmed passenger
        if (post.driver_id.toString() === userId) {
          socket.join(roomName);
          console.info(JSON.stringify({
            level: 'info',
            type: 'socket_join_post_success',
            socketId: socket.id,
            room: roomName,
            userId,
            role: 'driver'
          }));
          return;
        }
        const booking = await Booking.findOne({
          post_id: post.post_id,
          passenger_id: userId,
          status: 'confirmed'
        });
        if (!booking) return socket.disconnect();

        socket.join(roomName);
        console.info(JSON.stringify({
          level: 'info',
          type: 'socket_join_post_success',
          socketId: socket.id,
          room: roomName,
          userId,
          role: 'passenger'
        }));
      } catch (err) {
        incrementCounter('socketJoinFailures');
        console.warn(JSON.stringify({
          level: 'warn',
          type: 'socket_join_post_failed',
          socketId: socket.id,
          message: err?.message
        }));
        socket.disconnect();
      }
    });

    socket.on('send_message', async (data) => {
      if (!isSendMessageAllowed(`send_message:${socket.id}`)) {
        incrementCounter('socketSendFailures');
        socket.emit('rate_limit', { event: 'send_message' });
        console.warn(JSON.stringify({
          level: 'warn',
          type: 'socket_send_message_rate_limited',
          socketId: socket.id
        }));
        return;
      }

      const { body } = data;
      try {
        if (!socket.userId || !socket.currentPostId || !socket.currentPostRoom) return;
        const message = new Message({
          message_id: uuidv4(),
          post_id: socket.currentPostId,
          sender_id: socket.userId,
          body
        });
        await message.save();
        const populated = await message.populate('sender_id', 'first_name last_name');
        io.to(socket.currentPostRoom).emit('new_message', populated);
      } catch (err) {
        incrementCounter('socketSendFailures');
        console.error(JSON.stringify({
          level: 'error',
          type: 'socket_send_message_failed',
          socketId: socket.id,
          room: socket.currentPostRoom,
          message: err?.message
        }));
      }
    });

    // Admin room
    socket.on('join_admin', (token) => {
      try {
        const decoded = jwt.verify(token, process.env.JWT_SECRET);
        if (decoded.role === 'admin') socket.join('admin');
      } catch (err) {}
    });

    // User room (for personal realtime events, e.g. verification updates)
    socket.on('join_user', (token) => {
      try {
        const decoded = jwt.verify(token, process.env.JWT_SECRET);
        socket.join(`user_${decoded.id}`);
      } catch (err) {}
    });

    socket.on('disconnect', () => {
      console.info(JSON.stringify({
        level: 'info',
        type: 'socket_disconnected',
        socketId: socket.id,
        room: socket.currentPostRoom || null
      }));
    });

    // Other events like location_update handled in controllers if needed
  });
};
