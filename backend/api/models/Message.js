const mongoose = require('mongoose');

const messageSchema = new mongoose.Schema({
  message_id: { type: String, unique: true, required: true },
  post_id: { type: mongoose.Schema.Types.ObjectId, ref: 'CarpoolPost', required: true },
  sender_id: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  body: { type: String, required: true },
  sent_at: { type: Date, default: Date.now }
});

module.exports = mongoose.model('Message', messageSchema);