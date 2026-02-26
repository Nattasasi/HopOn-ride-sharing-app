const express = require('express');
const router = express.Router();
const { updateTracking, getTracking } = require('../controllers/trackingController');

// Note: io is passed from app if needed, but for simplicity, assume global io or inject
router.post('/:postId', updateTracking);
router.get('/:postId', getTracking);

module.exports = router;