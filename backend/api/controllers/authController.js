const { v4: uuidv4 } = require('uuid');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const crypto = require('crypto');
const User = require('../models/User');
const { body, validationResult } = require('express-validator');

const PASSWORD_RESET_TOKEN_EXPIRY_MS = Number(process.env.PASSWORD_RESET_TOKEN_EXPIRY_MS || 15 * 60 * 1000);

const hashToken = (token) => {
  return crypto.createHash('sha256').update(token).digest('hex');
};

const issueSessionTokens = ({ userId, role, tokenVersion }) => {
  const token = jwt.sign(
    { id: userId, role, tv: tokenVersion },
    process.env.JWT_SECRET,
    { expiresIn: '1h' }
  );
  const refreshToken = jwt.sign(
    { id: userId, tv: tokenVersion },
    process.env.JWT_REFRESH_SECRET,
    { expiresIn: '7d' }
  );
  return { token, refreshToken };
};

const persistRefreshTokenHash = async (user, refreshToken) => {
  user.refresh_token_hash = hashToken(refreshToken);
  await user.save();
};

// Register
const register = [
  body('first_name').notEmpty(),
  body('last_name').notEmpty(),
  body('email').isEmail(),
  body('dob').isDate(),
  body('password').isLength({ min: 6 }),
  body('phone_number').notEmpty(),
  body('role').isIn(['rider', 'driver', 'admin']),
  async (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) return res.status(400).json({ errors: errors.array() });

    const { first_name, last_name, email, dob, password, phone_number, role } = req.body;
    const existingUser = await User.findOne({ email });
    if (existingUser) return res.status(400).json({ message: 'User exists' });


    const user = new User({
      user_id: uuidv4(),
      first_name,
      last_name,
      email,
      dob,
      password_hash: password,
      phone_number,
      role,
    });
    await user.save();

    const { token, refreshToken } = issueSessionTokens({
      userId: user._id,
      role: user.role,
      tokenVersion: user.token_version || 0
    });
    await persistRefreshTokenHash(user, refreshToken);

    res.status(201).json({ userId: user._id, token, refreshToken });
  }
];

// Login
const login = async (req, res) => {
  const { email, password } = req.body;
  const user = await User.findOne({ email });
  if (!user || !await bcrypt.compare(password, user.password_hash)) {
    return res.status(401).json({ message: 'Invalid credentials' });
  }

  const { token, refreshToken } = issueSessionTokens({
    userId: user._id,
    role: user.role,
    tokenVersion: user.token_version || 0
  });
  await persistRefreshTokenHash(user, refreshToken);

  res.json({ userId: user._id, token, refreshToken });
};

// Verify (placeholder for email verification)
const verify = async (req, res) => {
  // Implement verification logic, e.g., via email token
  res.json({ message: 'Verified' });
};

// Refresh token
const refresh = async (req, res) => {
  const user = await User.findById(req.user.id).select('_id role token_version refresh_token_hash');
  if (!user) {
    return res.status(401).json({ message: 'Invalid refresh token user' });
  }

  const { token, refreshToken } = issueSessionTokens({
    userId: user._id,
    role: user.role,
    tokenVersion: user.token_version || 0
  });
  await persistRefreshTokenHash(user, refreshToken);

  res.json({ token, refreshToken });
};

const forgotPassword = [
  body('email').isEmail(),
  async (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) return res.status(400).json({ errors: errors.array() });

    const { email } = req.body;
    const genericResponse = {
      message: 'If the account exists, recovery instructions have been generated.'
    };

    const user = await User.findOne({ email });
    if (!user) {
      return res.json(genericResponse);
    }

    const resetToken = crypto.randomBytes(32).toString('hex');
    const tokenHash = crypto.createHash('sha256').update(resetToken).digest('hex');
    const expiresAt = new Date(Date.now() + PASSWORD_RESET_TOKEN_EXPIRY_MS);

    user.password_reset_token_hash = tokenHash;
    user.password_reset_expires_at = expiresAt;
    await user.save();

    if (process.env.NODE_ENV !== 'production') {
      return res.json({
        ...genericResponse,
        devResetToken: resetToken,
        expiresAt: expiresAt.toISOString()
      });
    }

    return res.json(genericResponse);
  }
];

const resetPassword = [
  body('token').isString().notEmpty(),
  body('newPassword').isLength({ min: 6 }),
  async (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) return res.status(400).json({ errors: errors.array() });

    const { token, newPassword } = req.body;
    const tokenHash = crypto.createHash('sha256').update(token).digest('hex');

    const user = await User.findOne({
      password_reset_token_hash: tokenHash,
      password_reset_expires_at: { $gt: new Date() }
    });

    if (!user) {
      return res.status(400).json({ message: 'Invalid or expired reset token' });
    }

    user.password_hash = newPassword;
    user.password_reset_token_hash = null;
    user.password_reset_expires_at = null;
    user.password_changed_at = new Date();
    user.refresh_token_hash = null;
    user.token_version = (user.token_version || 0) + 1;
    await user.save();

    return res.json({ message: 'Password updated successfully' });
  }
];

module.exports = { register, login, verify, refresh, forgotPassword, resetPassword };