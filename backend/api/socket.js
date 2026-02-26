const jwt = require('jsonwebtoken');
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

        // Check if driver or confirmed passenger
        const post = await CarpoolPost.findById(post_id);
        if (post.driver_id.toString() === userId) {
          socket.join(`post_${post_id}`);
          return;
        }
        const booking = await Booking.findOne({ post_id, passenger_id: userId, status: 'confirmed' });
        if (!booking) return socket.disconnect();

        socket.join(`post_${post_id}`);
      } catch (err) {
        socket.disconnect();
      }
    });

    socket.on('send_message', async (data) => {
      const { post_id, body } = data;
      try {
        const message = new Message({
          message_id: uuidv4(),
          post_id,
          sender_id: socket.userId,
          body
        });
        await message.save();
        const populated = await message.populate('sender_id', 'first_name last_name');
        io.to(`post_${post_id}`).emit('new_message', populated);
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

    // Other events like location_update handled in controllers if needed
  });
};