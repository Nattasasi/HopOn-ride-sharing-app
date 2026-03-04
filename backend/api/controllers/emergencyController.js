const { v4: uuidv4 } = require('uuid');
const EmergencyAlert = require('../models/EmergencyAlert');
const CarpoolPost = require('../models/CarpoolPost');
const Booking = require('../models/Booking');

const createEmergency = async (req, res) => {
  const { post_id, lat, lng } = req.body || {};
  if (!post_id || typeof lat !== 'number' || typeof lng !== 'number') {
    return res.status(400).json({ message: 'post_id, lat and lng are required' });
  }

  const post = await CarpoolPost.findById(post_id) || await CarpoolPost.findOne({ post_id });
  if (!post) return res.status(404).json({ message: 'Ride not found' });

  const isHost = post.driver_id.toString() === req.user.id;
  let isPassenger = false;
  if (!isHost) {
    const booking = await Booking.findOne({
      post_id: post.post_id,
      passenger_id: req.user.id,
      status: { $in: ['accepted', 'confirmed'] }
    }).select('_id');
    isPassenger = Boolean(booking);
  }
  if (!isHost && !isPassenger && req.user.role !== 'admin') {
    return res.status(403).json({ message: 'Only ride participants can send emergency alerts' });
  }

  const alert = new EmergencyAlert({
    alert_id: uuidv4(),
    post_id: post._id,
    reporter_id: req.user.id,
    lat,
    lng
  });
  await alert.save();

  const populated = await alert.populate([
    { path: 'post_id', select: 'post_id start_location_name end_location_name status departure_time' },
    { path: 'reporter_id', select: 'first_name last_name email phone_number' }
  ]);

  // Emit to admin room
  const io = req.app.get('io');
  if (io) {
    io.to('admin').emit('emergency', populated);
  }

  res.status(201).json(populated);
};

const getEmergencies = async (req, res) => {
  const alerts = await EmergencyAlert.find({ resolved: false })
    .populate('post_id', 'post_id start_location_name end_location_name status departure_time')
    .populate('reporter_id', 'first_name last_name email phone_number')
    .sort({ created_at: -1 });
  res.json(alerts);
};

module.exports = { createEmergency, getEmergencies };
