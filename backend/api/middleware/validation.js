const { validationResult } = require('express-validator');

const handleValidationErrors = (req, res, next) => {
  const errors = validationResult(req);
  if (errors.isEmpty()) {
    return next();
  }

  return res.status(400).json({
    message: 'Validation failed',
    errors: errors.array().map((error) => ({
      field: error.path,
      message: error.msg
    }))
  });
};

const rejectUnknownBodyFields = (allowedFields) => (req, res, next) => {
  const payload = req.body;
  if (!payload || typeof payload !== 'object' || Array.isArray(payload)) {
    return next();
  }

  const unknownFields = Object.keys(payload).filter((key) => !allowedFields.includes(key));
  if (unknownFields.length === 0) {
    return next();
  }

  return res.status(400).json({
    message: 'Validation failed',
    errors: unknownFields.map((field) => ({
      field,
      message: 'Unknown field'
    }))
  });
};

module.exports = {
  handleValidationErrors,
  rejectUnknownBodyFields
};
