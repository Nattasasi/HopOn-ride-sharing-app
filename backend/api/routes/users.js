const express = require('express');
const router = express.Router();
const { getUser, updateUser, getUserFeedback } = require('../controllers/usersController');

router.get('/:id', getUser);
router.put('/:id', updateUser);
router.get('/:id/feedback', getUserFeedback);

module.exports = router;