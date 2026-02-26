const Message = require('../models/Message');

const getMessages = async (req, res) => {
  const messages = await Message.find({ post_id: req.params.postId })
    .populate('sender_id', 'first_name last_name')
    .sort({ sent_at: 1 });
  res.json(messages);
};

module.exports = { getMessages };