const mongoose = require('mongoose');

const emergencyAlertSchema = new mongoose.Schema({
  alert_id: { type: String, unique: true, required: true },
  post_id: { type: mongoose.Schema.Types.ObjectId, ref: 'CarpoolPost', required: true },
  reporter_id: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  lat: { type: Number, required: true },
  lng: { type: Number, required: true },
  resolved: { type: Boolean, default: false },
  created_at: { type: Date, default: Date.now }
});

module.exports = mongoose.model('EmergencyAlert', emergencyAlertSchema);