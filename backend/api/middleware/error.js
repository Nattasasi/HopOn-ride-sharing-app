module.exports = (err, req, res, next) => {
  console.error(err.stack);
  if (err && (err.type === 'entity.too.large' || err.status === 413)) {
    return res.status(413).json({ message: 'Uploaded image is too large. Please choose a smaller photo.' });
  }
  const statusCode = err?.status || 500;
  const message = err?.message || 'Something went wrong!';
  return res.status(statusCode).json({ message, error: err?.message });
};
