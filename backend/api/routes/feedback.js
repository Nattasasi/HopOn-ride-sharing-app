const express = require('express');
const router = express.Router();
const { createFeedback, getFeedback } = require('../controllers/feedbackController');

router.post('/', createFeedback);
router.get('/users/:id/feedback', getFeedback); // Duplicate with users, but as per spec

module.exports = router;