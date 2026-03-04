const mongoose = require('mongoose');
const { v4: uuidv4 } = require('uuid');
const { body, validationResult } = require('express-validator');
const Report = require('../models/Report');
const Booking = require('../models/Booking');
const CarpoolPost = require('../models/CarpoolPost');
const User = require('../models/User');

const REPORT_STAGES = ['ongoing', 'completed'];
const REPORT_STATUSES = ['pending', 'reviewed', 'resolved', 'dismissed'];
const ACTIVE_BOOKING_STATUSES = ['accepted', 'confirmed'];

const emitAdminReportEvent = (req, eventName, report) => {
  const io = req.app.get('io');
  if (!io) return;
  io.to('admin').emit(eventName, report);
};

const validate = (req, res) => {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    res.status(400).json({ errors: errors.array() });
    return false;
  }
  return true;
};

const normalizeId = (value) => (value || '').toString().trim();

const findPostByAnyId = async (postIdInput) => {
  const normalized = normalizeId(postIdInput);
  if (!normalized) return null;

  if (mongoose.Types.ObjectId.isValid(normalized)) {
    const byObjectId = await CarpoolPost.findById(normalized);
    if (byObjectId) return byObjectId;
  }

  return CarpoolPost.findOne({ post_id: normalized });
};

const isStageAllowedForPostStatus = (stage, postStatus) => {
  if (stage === 'ongoing') return postStatus === 'in_progress';
  if (stage === 'completed') return postStatus === 'completed';
  return false;
};

const findParticipantBooking = async (postPublicId, passengerId) => {
  return Booking.findOne({
    post_id: postPublicId,
    passenger_id: passengerId,
    status: { $in: ACTIVE_BOOKING_STATUSES }
  });
};

const resolveParticipation = async (post, reporterId, reportedUserId, bookingIdInput) => {
  if (!mongoose.Types.ObjectId.isValid(reporterId) || !mongoose.Types.ObjectId.isValid(reportedUserId)) {
    return {
      ok: false,
      statusCode: 400,
      message: 'Invalid reporter or reported user id'
    };
  }

  if (post.driver_id.toString() === reporterId) {
    return {
      ok: false,
      statusCode: 403,
      message: 'Driver-side reporting is not enabled yet'
    };
  }

  const reporterBooking = await findParticipantBooking(post.post_id, reporterId);
  if (!reporterBooking) {
    return {
      ok: false,
      statusCode: 403,
      message: 'Rider can report only if user participated in that ride'
    };
  }

  if (post.driver_id.toString() !== reportedUserId) {
    return {
      ok: false,
      statusCode: 403,
      message: 'Rider can only report the ride host for this ride'
    };
  }

  return {
    ok: true,
    booking_id: bookingIdInput || reporterBooking.booking_id || null
  };
};

const createReportValidators = [
  body('post_id').notEmpty().withMessage('post_id is required'),
  body('reported_user_id').isMongoId().withMessage('reported_user_id must be a valid user id'),
  body('stage').isIn(REPORT_STAGES).withMessage('stage must be ongoing or completed'),
  body('category')
    .trim()
    .notEmpty()
    .withMessage('category is required')
    .isLength({ max: 100 })
    .withMessage('category must be at most 100 characters'),
  body('description')
    .trim()
    .notEmpty()
    .withMessage('description is required')
    .isLength({ max: 1000 })
    .withMessage('description must be at most 1000 characters'),
  body('booking_id')
    .optional()
    .isString()
    .withMessage('booking_id must be a string')
    .isLength({ max: 100 })
    .withMessage('booking_id must be at most 100 characters')
];

const createReport = [
  ...createReportValidators,
  async (req, res) => {
    try {
      if (!validate(req, res)) return;

      const post = await findPostByAnyId(req.body.post_id);
      if (!post) return res.status(404).json({ message: 'Ride not found' });

      const reportedUser = await User.findById(req.body.reported_user_id).select('_id');
      if (!reportedUser) return res.status(404).json({ message: 'Reported user not found' });

      if (!isStageAllowedForPostStatus(req.body.stage, post.status)) {
        const message = req.body.stage === 'ongoing'
          ? 'stage=ongoing is allowed only when ride status is in_progress'
          : 'stage=completed is allowed only when ride status is completed';
        return res.status(400).json({ message });
      }

      const reporterId = normalizeId(req.user.id);
      const reportedUserId = normalizeId(req.body.reported_user_id);

      if (post.driver_id.toString() !== reportedUserId) {
        return res.status(403).json({ message: 'reported_user_id must match the ride host' });
      }

      const participation = await resolveParticipation(
        post,
        reporterId,
        reportedUserId,
        req.body.booking_id
      );
      if (!participation.ok) {
        return res.status(participation.statusCode).json({ message: participation.message });
      }

      const report = new Report({
        report_id: uuidv4(),
        post_id: post._id,
        reporter_id: reporterId,
        reported_user_id: reportedUserId,
        booking_id: participation.booking_id,
        stage: req.body.stage,
        category: req.body.category,
        description: req.body.description
      });

      await report.save();
      const saved = await Report.findById(report._id)
        .populate('reporter_id', 'first_name last_name role')
        .populate('reported_user_id', 'first_name last_name role')
        .populate('post_id', 'post_id status departure_time');

      emitAdminReportEvent(req, 'report_created', saved);

      return res.status(201).json(saved);
    } catch (error) {
      return res.status(500).json({ message: error.message });
    }
  }
];

const getMyReports = async (req, res) => {
  try {
    const reports = await Report.find({ reporter_id: req.user.id })
      .populate('reported_user_id', 'first_name last_name role')
      .populate('post_id', 'post_id status departure_time')
      .sort({ created_at: -1 });
    return res.json(reports);
  } catch (error) {
    return res.status(500).json({ message: error.message });
  }
};

const getAdminReports = async (req, res) => {
  try {
    const { status, stage } = req.query;
    const query = {};

    if (status && REPORT_STATUSES.includes(status)) query.status = status;
    if (stage && REPORT_STAGES.includes(stage)) query.stage = stage;

    const reports = await Report.find(query)
      .populate('reporter_id', 'first_name last_name email role')
      .populate('reported_user_id', 'first_name last_name email role')
      .populate('post_id', 'post_id status departure_time')
      .sort({ created_at: -1 });
    return res.json(reports);
  } catch (error) {
    return res.status(500).json({ message: error.message });
  }
};

const updateReportStatus = [
  body('status').isIn(REPORT_STATUSES).withMessage('Invalid report status'),
  async (req, res) => {
    try {
      if (!validate(req, res)) return;

      const report = await Report.findById(req.params.id);
      if (!report) return res.status(404).json({ message: 'Report not found' });

      report.status = req.body.status;
      await report.save();
      const updated = await Report.findById(report._id)
        .populate('reporter_id', 'first_name last_name email role')
        .populate('reported_user_id', 'first_name last_name email role')
        .populate('post_id', 'post_id status departure_time');

      emitAdminReportEvent(req, 'report_updated', updated);

      return res.json({ message: 'Report status updated', report: updated });
    } catch (error) {
      return res.status(500).json({ message: error.message });
    }
  }
];

module.exports = {
  createReport,
  getMyReports,
  getAdminReports,
  updateReportStatus,
  __private: {
    findPostByAnyId,
    isStageAllowedForPostStatus,
    resolveParticipation
  }
};
