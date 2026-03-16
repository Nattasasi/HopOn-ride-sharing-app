const { randomUUID } = require('crypto');

function correlationIdMiddleware(req, res, next) {
  const incomingCorrelationId = req.header('x-correlation-id');
  const correlationId = incomingCorrelationId || randomUUID();

  req.correlationId = correlationId;
  res.setHeader('x-correlation-id', correlationId);
  next();
}

function requestLogger(req, res, next) {
  const startedAt = Date.now();

  res.on('finish', () => {
    const durationMs = Date.now() - startedAt;
    console.info(JSON.stringify({
      level: 'info',
      type: 'http_request',
      method: req.method,
      path: req.originalUrl,
      status: res.statusCode,
      durationMs,
      correlationId: req.correlationId
    }));
  });

  next();
}

module.exports = {
  correlationIdMiddleware,
  requestLogger
};