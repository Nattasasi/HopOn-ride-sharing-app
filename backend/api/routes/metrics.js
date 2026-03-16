const express = require('express');
const requireRole = require('../middleware/role');
const { getMetricsSnapshot } = require('../middleware/metrics');

const router = express.Router();

router.get('/', requireRole('admin'), (req, res) => {
  res.json(getMetricsSnapshot());
});

module.exports = router;