const mongoose = require('mongoose');

const userSchema = new mongoose.Schema({
  user_id: { type: String, unique: true, required: true },
  first_name: { type: String, required: true },
  last_name: { type: String, required: true },
  email: { type: String, unique: true, required: true },
  dob: { type: Date, required: true },
  password_hash: { type: String, required: true },
  phone_number: { type: String, required: true },
  role: { type: String, enum: ['rider', 'driver', 'admin'], required: true },
  average_rating: { type: Number, default: 0 },
  is_verified: { type: Boolean, default: false },
  is_banned: { type: Boolean, default: false },
  created_at: { type: Date, default: Date.now },
  updated_at: { type: Date, default: Date.now }
});

module.exports = mongoose.model('User', userSchema);