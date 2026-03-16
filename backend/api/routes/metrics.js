const express = require('express');
const requireRole = require('../middleware/role');
const { getMetricsSnapshot, getMetricsDashboardSnapshot } = require('../middleware/metrics');

const router = express.Router();

router.get('/', requireRole('admin'), (req, res) => {
  res.json(getMetricsSnapshot());
});

router.get('/dashboard', requireRole('admin'), (req, res) => {
  res.json(getMetricsDashboardSnapshot());
});

module.exports = router;