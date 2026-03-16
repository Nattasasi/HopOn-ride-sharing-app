const mongoose = require('mongoose');

const carpoolPostSchema = new mongoose.Schema({
  post_id: { type: String, unique: true, required: true },
  driver_id: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  start_location_name: { type: String, required: true },
  start_lat: { type: Number, required: true },
  start_lng: { type: Number, required: true },
  location: {
    type: {
      type: String,
      enum: ['Point'],
      required: true
    },
    coordinates: {
      type: [Number], // [longitude, latitude]
      required: true
    }
  },
  end_location_name: { type: String, required: true },
  end_lat: { type: Number, required: true },
  end_lng: { type: Number, required: true },
  end_location: {
    type: {
      type: String,
      enum: ['Point'],
      required: true
    },
    coordinates: {
      type: [Number], // [longitude, latitude]
      required: true
    }
  },
  departure_time: { type: Date, required: true },
  total_seats: { type: Number, required: true },
  available_seats: { type: Number, required: true },
  price_per_seat: { type: Number, required: true },
  status: { type: String, enum: ['active', 'in_progress', 'completed', 'cancelled'], default: 'active' },
  vehicle_info: { type: String, default: null },
  vehicle_plate: { type: String, default: null },
  vehicle_brand: { type: String, default: null },
  vehicle_color: { type: String, default: null },
  contact_info: { type: String, default: null },
  additional_notes: { type: String, default: null },
  wait_time_minutes: { type: Number, default: null },
  created_at: { type: Date, default: Date.now }
});

carpoolPostSchema.index({ location: '2dsphere' });
carpoolPostSchema.index({ end_location: '2dsphere' });
carpoolPostSchema.index({ departure_time: 1, status: 1, driver_id: 1 });

module.exports = mongoose.model('CarpoolPost', carpoolPostSchema);
