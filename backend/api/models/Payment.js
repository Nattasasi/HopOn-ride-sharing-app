const mongoose = require('mongoose');

const paymentSchema = new mongoose.Schema({
  payment_id: { type: String, unique: true, required: true },
  booking_id: { type: mongoose.Schema.Types.ObjectId, ref: 'Booking', required: true },
  amount: { type: Number, required: true },
  status: { type: String, required: true },
  transaction_ref: { type: String },
  created_at: { type: Date, default: Date.now },
  payment_date: { type: Date },
  payment_type: { type: String }
});

module.exports = mongoose.model('Payment', paymentSchema);