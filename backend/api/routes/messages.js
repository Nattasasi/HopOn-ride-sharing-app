const express = require('express');
const router = express.Router();
const { getMessages } = require('../controllers/messagesController');

router.get('/:postId', getMessages);

module.exports = router;