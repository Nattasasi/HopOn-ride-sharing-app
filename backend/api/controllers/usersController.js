const User = require('../models/User');
const Feedback = require('../models/Feedback');
const { body, validationResult } = require('express-validator');

const getUser = async (req, res) => {
  const user = await User.findById(req.params.id).select('-password_hash');
  if (!user) return res.status(404).json({ message: 'User not found' });
  res.json(user);
};

const updateUser = [
  body('first_name').optional().notEmpty(),
  // Add other fields...
  async (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) return res.status(400).json({ errors: errors.array() });

    const updated = await User.findByIdAndUpdate(req.params.id, req.body, { new: true });
    res.json(updated);
  }
];

const getUserFeedback = async (req, res) => {
  const feedback = await Feedback.find({ reviewee_id: req.params.id });
  res.json(feedback);
};

module.exports = { getUser, updateUser, getUserFeedback };