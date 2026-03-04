const { v4: uuidv4 } = require('uuid');
const RidesTracking = require('../models/RidesTracking');

const updateTracking = async (req, res) => {
  const { current_lat, current_lng, eta_minutes } = req.body;
  const log = new RidesTracking({
    log_id: uuidv4(),
    post_id: req.params.postId,
    current_lat,
    current_lng,
    eta_minutes
  });
  await log.save();

  // Emit to post room
  const io = req.app.get('io');
  if (io) {
    io.to(`post_${req.params.postId}`).emit('location_update', log);
  }

  res.json(log);
};

const getTracking = async (req, res) => {
  const logs = await RidesTracking.find({ post_id: req.params.postId }).sort({ updated_at: -1 }).limit(1);
  res.json(logs[0] || null);
};

module.exports = { updateTracking, getTracking };
