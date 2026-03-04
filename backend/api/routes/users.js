const express = require('express');
const router = express.Router();
const {
  getUser,
  updateUser,
  getUserFeedback,
  submitMyVerification,
  getMyVerification
} = require('../controllers/usersController');

router.post('/me/verification', submitMyVerification);
router.get('/me/verification', getMyVerification);
router.get('/:id', getUser);
router.put('/:id', updateUser);
router.get('/:id/feedback', getUserFeedback);

module.exports = router;
