const User = require('../models/User');
const CarpoolPost = require('../models/CarpoolPost');
const EmergencyAlert = require('../models/EmergencyAlert');
const Booking = require('../models/Booking');

const getUsers = async (req, res) => {
  try {
    const { search } = req.query;
    const query = search ? { $or: [{ first_name: new RegExp(search, 'i') }, { email: new RegExp(search, 'i') }] } : {};
    const users = await User.find(query).sort({ created_at: -1 });
    res.json(users);
  } catch (error) {
    res.status(500).json({ message: 'Error fetching users', error: error.message });
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
    
    const posts = await CarpoolPost.find(query)
      .populate('driver_id', 'first_name last_name email')
      .sort({ created_at: -1 });
      
    res.json(posts);
  } catch (error) {
    res.status(500).json({ message: 'Error fetching posts', error: error.message });
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
    const bookings = await Booking.find()
      .populate('passenger_id', 'first_name last_name email')
      .populate({
        path: 'post_id',
        populate: { path: 'driver_id', select: 'first_name last_name' }
      })
      .sort({ booked_at: -1 });
    res.json(bookings);
  } catch (error) {
    res.status(500).json({ message: 'Error fetching bookings', error: error.message });
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

module.exports = { 
  getUsers, 
  banUser, 
  getPosts, 
  deletePost,
  getBookings,
  updateBookingStatus,
  getDashboard 
};