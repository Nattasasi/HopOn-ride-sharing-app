const helmet = require('helmet');
const rateLimit = require('express-rate-limit');

const DEFAULT_LOCAL_ORIGINS = [
  'http://localhost:3000',
  'http://127.0.0.1:3000'
];

function parseAllowedOrigins() {
  const raw = process.env.CORS_ALLOWED_ORIGINS || '';
  const fromEnv = raw
    .split(',')
    .map((origin) => origin.trim())
    .filter(Boolean);

  if (fromEnv.length > 0) return fromEnv;
  if (process.env.NODE_ENV === 'production') return [];
  return DEFAULT_LOCAL_ORIGINS;
}

function isOriginAllowed(origin, allowedOrigins) {
  if (!origin) return true;
  return allowedOrigins.includes(origin);
}

function buildExpressCorsOptions() {
  const allowedOrigins = parseAllowedOrigins();

  return {
    credentials: true,
    origin(origin, callback) {
      if (isOriginAllowed(origin, allowedOrigins)) {
        return callback(null, true);
      }
      return callback(new Error('CORS origin not allowed'));
    }
  };
}

function buildSocketCorsOptions() {
  const allowedOrigins = parseAllowedOrigins();

  return {
    credentials: true,
    origin(origin, callback) {
      if (isOriginAllowed(origin, allowedOrigins)) {
        return callback(null, true);
      }
      return callback(new Error('Socket origin not allowed'));
    }
  };
}

const securityHeaders = helmet({
  contentSecurityPolicy: false
});

const apiRateLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: Number(process.env.API_RATE_LIMIT_MAX || 600),
  standardHeaders: true,
  legacyHeaders: false,
  message: { message: 'Too many requests, please try again later.' }
});

const authRateLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: Number(process.env.AUTH_RATE_LIMIT_MAX || 25),
  standardHeaders: true,
  legacyHeaders: false,
  message: { message: 'Too many authentication attempts. Please try again later.' }
});

const bookingMutationRateLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: Number(process.env.BOOKING_MUTATION_RATE_LIMIT_MAX || 120),
  standardHeaders: true,
  legacyHeaders: false,
  message: { message: 'Too many booking updates. Please try again later.' }
});

function createSocketEventRateGate({ windowMs, max }) {
  const bucketByKey = new Map();

  return function isAllowed(key) {
    const now = Date.now();
    const bucket = bucketByKey.get(key) || [];
    const recent = bucket.filter((ts) => now - ts < windowMs);

    if (recent.length >= max) {
      bucketByKey.set(key, recent);
      return false;
    }

    recent.push(now);
    bucketByKey.set(key, recent);
    return true;
  };
}

module.exports = {
  buildExpressCorsOptions,
  buildSocketCorsOptions,
  securityHeaders,
  apiRateLimiter,
  authRateLimiter,
  bookingMutationRateLimiter,
  createSocketEventRateGate
};