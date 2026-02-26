const { v4: uuidv4 } = require('uuid');
const Feedback = require('../models/Feedback');
const User = require('../models/User');
const CarpoolPost = require('../models/CarpoolPost');

const createFeedback = async (req, res) => {
  const post = await CarpoolPost.findById(req.body.post_id);
  if (post.status !== 'completed') return res.status(400).json({ message: 'Ride not completed' });

  const feedback = new Feedback({
    feedback_id: uuidv4(),
    ...req.body
  });
  await feedback.save();

  // Recalculate average rating
  const reviews = await Feedback.find({ reviewee_id: req.body.reviewee_id });
  const avg = reviews.reduce((sum, r) => sum + r.rating, 0) / reviews.length;
  await User.findByIdAndUpdate(req.body.reviewee_id, { average_rating: avg });

  res.status(201).json(feedback);
};

const getFeedback = async (req, res) => {
  const feedback = await Feedback.find({ reviewee_id: req.params.id });
  res.json(feedback);
};

module.exports = { createFeedback, getFeedback };