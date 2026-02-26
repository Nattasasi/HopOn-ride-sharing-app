const mongoose = require('mongoose');

const feedbackSchema = new mongoose.Schema({
  feedback_id: { type: String, unique: true, required: true },
  post_id: { type: mongoose.Schema.Types.ObjectId, ref: 'CarpoolPost', required: true },
  reviewer_id: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  reviewee_id: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  rating: { type: Number, min: 1, max: 5, required: true },
  comment: { type: String },
  created_at: { type: Date, default: Date.now }
});

module.exports = mongoose.model('Feedback', feedbackSchema);