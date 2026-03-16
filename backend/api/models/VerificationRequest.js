const mongoose = require('mongoose');
const { v4: uuidv4 } = require('uuid');

const verificationRequestSchema = new mongoose.Schema({
  request_id: { type: String, unique: true, default: uuidv4 },
  user_id: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  verification_type: { type: String, enum: ['national_id', 'student_id'], required: true },
  verification_doc_url: { type: String, required: true },
  verification_notes: { type: String, default: null },
  status: {
    type: String,
    enum: ['pending', 'approved', 'rejected'],
    default: 'pending'
  },
  reviewed_by: { type: mongoose.Schema.Types.ObjectId, ref: 'User', default: null },
  reviewed_at: { type: Date, default: null },
  created_at: { type: Date, default: Date.now },
  updated_at: { type: Date, default: Date.now }
});

// Common reads: latest request per user and review queue by status.
verificationRequestSchema.index({ user_id: 1, created_at: -1 });
verificationRequestSchema.index({ status: 1, created_at: -1 });

module.exports = mongoose.model('VerificationRequest', verificationRequestSchema);

