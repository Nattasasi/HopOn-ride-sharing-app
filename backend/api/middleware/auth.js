const jwt = require('jsonwebtoken');
const User = require('../models/User');

const authMiddleware = (req, res, next) => {
  const token = req.header('Authorization')?.replace('Bearer ', '');

  if (!token) {
    return res.status(401).json({ message: 'Authentication required' });
  }

  try {
    const decoded = jwt.verify(token, process.env.JWT_SECRET);
    req.user = decoded;
    return next();
  } catch (_) {
    return res.status(401).json({ message: 'Invalid or expired access token' });
  }
};

const refreshTokenMiddleware = async (req, res, next) => {
  const authHeader = req.header('Authorization') || '';
  const tokenFromHeader = authHeader.startsWith('Bearer ')
    ? authHeader.replace('Bearer ', '')
    : null;
  const refreshToken = req.body?.refreshToken || tokenFromHeader;

  if (!refreshToken) {
    return res.status(401).json({ message: 'Refresh token is required' });
  }

  try {
    const decoded = jwt.verify(refreshToken, process.env.JWT_REFRESH_SECRET);
    const user = await User.findById(decoded.id).select('_id role');
    if (!user) {
      return res.status(401).json({ message: 'Invalid refresh token user' });
    }
    req.user = { id: user._id.toString(), role: user.role };
    return next();
  } catch (_) {
    return res.status(401).json({ message: 'Invalid or expired refresh token' });
  }
};

module.exports = { authMiddleware, refreshTokenMiddleware };
