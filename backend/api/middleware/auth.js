const jwt = require('jsonwebtoken');

const authMiddleware = (req, res, next) => {
  const token = req.header('Authorization')?.replace('Bearer ', '');
  
  if (token) {
    try {
      const decoded = jwt.verify(token, process.env.JWT_SECRET);
      req.user = decoded;
    } catch (err) {
      // Ignore invalid token for demo, just don't set req.user
      console.log('Demo Mode: Invalid token ignored');
    }
  }
  
  // If no user set, provide a dummy one so controllers don't crash
  if (!req.user) {
    req.user = { id: 'demo-user-id', role: 'admin' };
  }
  
  next();
};

const refreshTokenMiddleware = (req, res, next) => {
  next();
};

module.exports = { authMiddleware, refreshTokenMiddleware };