const mongoose = require('mongoose');

const reportSchema = new mongoose.Schema(
  {
    report_id: { type: String, unique: true, required: true },
    post_id: { type: mongoose.Schema.Types.ObjectId, ref: 'CarpoolPost', required: true },
    reporter_id: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
    reported_user_id: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
    booking_id: { type: String, default: null },
    stage: { type: String, enum: ['ongoing', 'completed'], required: true },
    category: { type: String, required: true, trim: true, maxlength: 100 },
    description: { type: String, required: true, trim: true, maxlength: 1000 },
    status: {
      type: String,
      enum: ['pending', 'reviewed', 'resolved', 'dismissed'],
      default: 'pending',
      required: true
    },
    resolution_notes: { type: String, default: null, trim: true, maxlength: 1000 },
    resolved_at: { type: Date, default: null }
  },
  {
    timestamps: { createdAt: 'created_at', updatedAt: 'updated_at' }
  }
);

reportSchema.index({ reporter_id: 1, created_at: -1 });
reportSchema.index({ status: 1, created_at: -1 });
reportSchema.index({ post_id: 1, reported_user_id: 1 });

module.exports = mongoose.model('Report', reportSchema);
