const mongoose = require('mongoose');

const bookingSchema = new mongoose.Schema({
  booking_id: { type: String, unique: true, required: true },
  post_id: { type: String, ref: 'CarpoolPost', required: true },
  passenger_id: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  seats_booked: { type: Number, default: 1, required: true },
  status: { 
    type: String, 
    enum: ['pending', 'accepted', 'rejected', 'confirmed', 'waitlisted', 'cancelled'], 
    default: 'pending',
    required: true 
  },
  booked_at: { type: Date, default: Date.now }
});

module.exports = mongoose.model('Booking', bookingSchema);