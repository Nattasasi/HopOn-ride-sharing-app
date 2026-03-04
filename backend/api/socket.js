const jwt = require('jsonwebtoken');
const mongoose = require('mongoose');
const Booking = require('./models/Booking');
const Message = require('./models/Message');
const CarpoolPost = require('./models/CarpoolPost');
const { v4: uuidv4 } = require('uuid');

module.exports = (io) => {
  io.on('connection', (socket) => {
    socket.on('join_post', async (data) => {
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
          return;
        }
        const booking = await Booking.findOne({
          post_id: post.post_id,
          passenger_id: userId,
          status: 'confirmed'
        });
        if (!booking) return socket.disconnect();

        socket.join(roomName);
      } catch (err) {
        socket.disconnect();
      }
    });

    socket.on('send_message', async (data) => {
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
        console.error('send_message error:', err);
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

    // Other events like location_update handled in controllers if needed
  });
};
