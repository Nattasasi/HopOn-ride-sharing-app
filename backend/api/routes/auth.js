const express = require('express');
const router = express.Router();
const { register, login, verify, refresh } = require('../controllers/authController');
const { refreshTokenMiddleware } = require('../middleware/auth');

router.post('/register', register);
router.post('/login', login);
router.post('/verify', verify);
router.post('/refresh', refreshTokenMiddleware, refresh);

module.exports = router;