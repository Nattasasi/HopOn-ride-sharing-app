const mongoose = require('mongoose');

const ridesTrackingSchema = new mongoose.Schema({
  log_id: { type: String, unique: true, required: true },
  post_id: { type: mongoose.Schema.Types.ObjectId, ref: 'CarpoolPost', required: true },
  current_lat: { type: Number, required: true },
  current_lng: { type: Number, required: true },
  eta_minutes: { type: Number },
  updated_at: { type: Date, default: Date.now }
});

module.exports = mongoose.model('RidesTracking', ridesTrackingSchema);
