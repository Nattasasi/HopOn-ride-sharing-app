const User = require('../models/User');
const CarpoolPost = require('../models/CarpoolPost');
const EmergencyAlert = require('../models/EmergencyAlert');
const Booking = require('../models/Booking');
const VerificationRequest = require('../models/VerificationRequest');
const mongoose = require('mongoose');
const {
  shouldUseCursorPagination,
  paginateFindQuery
} = require('../utils/pagination');

const getUsers = async (req, res) => {
  try {
    const { search } = req.query;
    const query = search ? { $or: [{ first_name: new RegExp(search, 'i') }, { email: new RegExp(search, 'i') }] } : {};
    if (shouldUseCursorPagination(req.query)) {
      const pageResult = await paginateFindQuery({
        model: User,
        query,
        cursor: req.query.cursor,
        limit: req.query.limit,
        sort: { _id: -1 }
      });
      return res.json(pageResult);
    }

    const users = await User.find(query).sort({ created_at: -1 });
    return res.json(users);
  } catch (error) {
    return res.status(500).json({ message: 'Error fetching users', error: error.message });
  }
};

const banUser = async (req, res) => {
  try {
    const { id } = req.params;
    const { is_banned } = req.body;
    await User.findByIdAndUpdate(id, { is_banned });
    res.json({ message: is_banned ? 'User banned' : 'User unbanned' });
  } catch (error) {
    res.status(500).json({ message: 'Error updating user status', error: error.message });
  }
};

const getPosts = async (req, res) => {
  try {
    const { status } = req.query;
    const query = (status && status.trim() !== "") ? { status } : {};

    if (shouldUseCursorPagination(req.query)) {
      const pageResult = await paginateFindQuery({
        model: CarpoolPost,
        query,
        cursor: req.query.cursor,
        limit: req.query.limit,
        sort: { _id: -1 },
        populate: 'driver_id first_name last_name email'
      });
      return res.json(pageResult);
    }
    
    const posts = await CarpoolPost.find(query)
      .populate('driver_id', 'first_name last_name email')
      .sort({ created_at: -1 });
      
    return res.json(posts);
  } catch (error) {
    return res.status(500).json({ message: 'Error fetching posts', error: error.message });
  }
};

const deletePost = async (req, res) => {
  try {
    const { id } = req.params;
    await CarpoolPost.findByIdAndDelete(id);
    await Booking.deleteMany({ post_id: id });
    res.json({ message: 'Post and associated bookings deleted' });
  } catch (error) {
    res.status(500).json({ message: 'Error deleting post', error: error.message });
  }
};

const getBookings = async (req, res) => {
  try {
    const shouldPaginate = shouldUseCursorPagination(req.query);
    const bookings = shouldPaginate
      ? await Booking.find(
          (() => {
            const query = {};
            const cursor = req.query.cursor;
            if (cursor && mongoose.Types.ObjectId.isValid(String(cursor))) {
              query._id = { $lt: new mongoose.Types.ObjectId(String(cursor)) };
            }
            return query;
          })()
        )
          .populate('passenger_id', 'first_name last_name email')
          .sort({ _id: -1 })
          .limit((Number(req.query.limit) > 0 ? Math.min(100, Math.floor(Number(req.query.limit))) : 20) + 1)
      : await Booking.find()
          .populate('passenger_id', 'first_name last_name email')
          .sort({ booked_at: -1 });

    let page = null;
    let effectiveBookings = bookings;
    if (shouldPaginate) {
      const parsedLimit = Number(req.query.limit);
      const limit = Number.isFinite(parsedLimit) && parsedLimit > 0 ? Math.min(100, Math.floor(parsedLimit)) : 20;
      const hasMore = bookings.length > limit;
      effectiveBookings = hasMore ? bookings.slice(0, limit) : bookings;
      page = {
        limit,
        has_more: hasMore,
        next_cursor: hasMore ? effectiveBookings[effectiveBookings.length - 1]?._id?.toString() || null : null
      };
    }

    const postPublicIds = [];
    const postMongoIds = [];
    for (const booking of effectiveBookings) {
      const value = booking.post_id;
      if (typeof value !== 'string' || value.trim() === '') continue;
      if (mongoose.Types.ObjectId.isValid(value)) {
        postMongoIds.push(new mongoose.Types.ObjectId(value));
      } else {
        postPublicIds.push(value);
      }
    }

    const postQuery = [];
    if (postPublicIds.length > 0) postQuery.push({ post_id: { $in: [...new Set(postPublicIds)] } });
    if (postMongoIds.length > 0) postQuery.push({ _id: { $in: postMongoIds } });

    const posts = postQuery.length === 0
      ? []
      : await CarpoolPost.find({ $or: postQuery })
      .populate('driver_id', '_id first_name last_name email')
      .lean();

    const postByPublicId = new Map(posts.map((post) => [post.post_id, post]));
    const postByMongoId = new Map(posts.map((post) => [post._id.toString(), post]));
    const hydrated = effectiveBookings.map((booking) => {
      const plain = booking.toObject();
      plain.post = postByPublicId.get(plain.post_id) || postByMongoId.get(plain.post_id) || null;
      return plain;
    });

    if (shouldPaginate) {
      return res.json({ items: hydrated, page });
    }

    return res.json(hydrated);
  } catch (error) {
    return res.status(500).json({ message: 'Error fetching bookings', error: error.message });
  }
};

const updateBookingStatus = async (req, res) => {
  try {
    const { id } = req.params;
    const { status } = req.body;
    await Booking.findByIdAndUpdate(id, { status });
    res.json({ message: 'Booking status updated' });
  } catch (error) {
    res.status(500).json({ message: 'Error updating booking', error: error.message });
  }
};

const getDashboard = async (req, res) => {
  try {
    const activeRides = await CarpoolPost.countDocuments({ status: 'active' });
    const inProgressRides = await CarpoolPost.countDocuments({ status: 'in_progress' });
    const totalUsers = await User.countDocuments();
    const openAlerts = await EmergencyAlert.countDocuments({ resolved: false });
    res.json({ activeRides, inProgressRides, totalUsers, openAlerts });
  } catch (error) {
    res.status(500).json({ message: 'Error fetching dashboard stats', error: error.message });
  }
};

const getVerifications = async (req, res) => {
  try {
    const { status } = req.query;
    const query = {};
    if (status && status.trim() !== '') {
      query.status = status.trim();
    }

    if (shouldUseCursorPagination(req.query)) {
      const pageResult = await paginateFindQuery({
        model: VerificationRequest,
        query,
        cursor: req.query.cursor,
        limit: req.query.limit,
        sort: { _id: -1 },
        populate: [
          'user_id first_name last_name email role is_verified verification_status verification_type verification_doc_url verified_at',
          'reviewed_by first_name last_name email role'
        ],
        lean: false
      });

      const serialized = pageResult.items.map((item) => item.toObject());
      return res.json({ items: serialized, page: pageResult.page });
    }

    const requests = await VerificationRequest.find(query)
      .populate('user_id', 'first_name last_name email role is_verified verification_status verification_type verification_doc_url verified_at')
      .populate('reviewed_by', 'first_name last_name email role')
      .sort({ created_at: -1 });
    return res.json(requests);
  } catch (error) {
    return res.status(500).json({ message: 'Error fetching verifications', error: error.message });
  }
};

const approveVerification = async (req, res) => {
  try {
    const { id } = req.params;
    const request = await VerificationRequest.findById(id).populate('user_id');
    if (!request || !request.user_id) {
      return res.status(404).json({ message: 'Verification request not found' });
    }
    const user = request.user_id;

    user.verification_status = 'verified';
    user.is_verified = true;
    user.verified_at = new Date();
    user.verification_type = request.verification_type;
    user.verification_doc_url = request.verification_doc_url;
    user.verification_notes = req.body?.verification_notes || request.verification_notes || null;
    user.updated_at = new Date();
    request.status = 'approved';
    request.reviewed_by = req.user.id;
    request.reviewed_at = new Date();
    request.updated_at = new Date();
    if (req.body?.verification_notes) request.verification_notes = req.body.verification_notes;
    await Promise.all([user.save(), request.save()]);

    const io = req.app.get('io');
    const payload = await VerificationRequest.findById(request._id)
      .populate('user_id', 'first_name last_name email role is_verified verification_status verification_type verification_doc_url verified_at')
      .populate('reviewed_by', 'first_name last_name email role')
      .lean();
    if (io && payload) {
      io.to('admin').emit('verification_updated', payload);
      io.to(`user_${user._id.toString()}`).emit('verification_updated', payload);
    }

    res.json({ message: 'Verification approved' });
  } catch (error) {
    res.status(500).json({ message: 'Error approving verification', error: error.message });
  }
};

const rejectVerification = async (req, res) => {
  try {
    const { id } = req.params;
    const request = await VerificationRequest.findById(id).populate('user_id');
    if (!request || !request.user_id) {
      return res.status(404).json({ message: 'Verification request not found' });
    }
    const user = request.user_id;

    user.verification_status = 'rejected';
    user.is_verified = false;
    user.verified_at = null;
    user.verification_type = request.verification_type;
    user.verification_doc_url = request.verification_doc_url;
    user.verification_notes = req.body?.verification_notes || request.verification_notes || null;
    user.updated_at = new Date();
    request.status = 'rejected';
    request.reviewed_by = req.user.id;
    request.reviewed_at = new Date();
    request.updated_at = new Date();
    if (req.body?.verification_notes) request.verification_notes = req.body.verification_notes;
    await Promise.all([user.save(), request.save()]);

    const io = req.app.get('io');
    const payload = await VerificationRequest.findById(request._id)
      .populate('user_id', 'first_name last_name email role is_verified verification_status verification_type verification_doc_url verified_at')
      .populate('reviewed_by', 'first_name last_name email role')
      .lean();
    if (io && payload) {
      io.to('admin').emit('verification_updated', payload);
      io.to(`user_${user._id.toString()}`).emit('verification_updated', payload);
    }

    res.json({ message: 'Verification rejected' });
  } catch (error) {
    res.status(500).json({ message: 'Error rejecting verification', error: error.message });
  }
};

module.exports = { 
  getUsers, 
  banUser, 
  getPosts, 
  deletePost,
  getBookings,
  updateBookingStatus,
  getDashboard,
  getVerifications,
  approveVerification,
  rejectVerification
};
