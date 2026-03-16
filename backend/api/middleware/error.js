module.exports = (err, req, res, next) => {
  console.error(JSON.stringify({
    level: 'error',
    type: 'unhandled_error',
    path: req?.originalUrl,
    correlationId: req?.correlationId,
    message: err?.message,
    stack: err?.stack
  }));
  if (err && (err.type === 'entity.too.large' || err.status === 413)) {
    return res.status(413).json({
      message: 'Uploaded image is too large. Please choose a smaller photo.',
      correlationId: req?.correlationId
    });
  }
  const statusCode = err?.status || 500;
  const message = err?.message || 'Something went wrong!';
  return res.status(statusCode).json({
    message,
    error: err?.message,
    correlationId: req?.correlationId
  });
};
