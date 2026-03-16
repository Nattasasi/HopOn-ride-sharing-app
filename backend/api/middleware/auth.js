const jwt = require('jsonwebtoken');
const User = require('../models/User');
const { incrementCounter } = require('./metrics');

const authMiddleware = async (req, res, next) => {
  const token = req.header('Authorization')?.replace('Bearer ', '');

  if (!token) {
    return res.status(401).json({ message: 'Authentication required' });
  }

  try {
    const decoded = jwt.verify(token, process.env.JWT_SECRET);
    const user = await User.findById(decoded.id).select('_id role password_changed_at');
    if (!user) {
      return res.status(401).json({ message: 'Authentication required' });
    }

    const tokenIssuedAt = Number(decoded.iat || 0);
    const passwordChangedAtSec = user.password_changed_at
      ? Math.floor(new Date(user.password_changed_at).getTime() / 1000)
      : 0;

    if (passwordChangedAtSec && tokenIssuedAt && tokenIssuedAt < passwordChangedAtSec) {
      return res.status(401).json({ message: 'Session expired. Please login again.' });
    }

    req.user = { id: user._id.toString(), role: user.role };
    return next();
  } catch (_) {
    console.warn(JSON.stringify({
      level: 'warn',
      type: 'auth_access_token_invalid',
      correlationId: req.correlationId
    }));
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
    incrementCounter('authRefreshFailures');
    return res.status(401).json({ message: 'Refresh token is required' });
  }

  try {
    const decoded = jwt.verify(refreshToken, process.env.JWT_REFRESH_SECRET);
    const user = await User.findById(decoded.id).select('_id role password_changed_at');
    if (!user) {
      incrementCounter('authRefreshFailures');
      console.warn(JSON.stringify({
        level: 'warn',
        type: 'auth_refresh_user_not_found',
        correlationId: req.correlationId
      }));
      return res.status(401).json({ message: 'Invalid refresh token user' });
    }

    const tokenIssuedAt = Number(decoded.iat || 0);
    const passwordChangedAtSec = user.password_changed_at
      ? Math.floor(new Date(user.password_changed_at).getTime() / 1000)
      : 0;
    if (passwordChangedAtSec && tokenIssuedAt && tokenIssuedAt < passwordChangedAtSec) {
      incrementCounter('authRefreshFailures');
      return res.status(401).json({ message: 'Session expired. Please login again.' });
    }

    req.user = { id: user._id.toString(), role: user.role };
    return next();
  } catch (_) {
    incrementCounter('authRefreshFailures');
    console.warn(JSON.stringify({
      level: 'warn',
      type: 'auth_refresh_token_invalid',
      correlationId: req.correlationId
    }));
    return res.status(401).json({ message: 'Invalid or expired refresh token' });
  }
};

module.exports = { authMiddleware, refreshTokenMiddleware };
