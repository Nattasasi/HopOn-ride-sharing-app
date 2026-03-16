const mongoose = require('mongoose');

const paymentSchema = new mongoose.Schema({
  payment_id: { type: String, unique: true, required: true },
  idempotency_key: { type: String, default: null },
  booking_id: { type: mongoose.Schema.Types.ObjectId, ref: 'Booking', required: true },
  amount: { type: Number, required: true },
  status: { type: String, required: true },
  transaction_ref: { type: String },
  created_at: { type: Date, default: Date.now },
  payment_date: { type: Date },
  payment_type: { type: String }
});

// Enforce replay protection when a client supplies an idempotency key.
paymentSchema.index(
  { idempotency_key: 1 },
  { unique: true, sparse: true, partialFilterExpression: { idempotency_key: { $type: 'string' } } }
);

module.exports = mongoose.model('Payment', paymentSchema);