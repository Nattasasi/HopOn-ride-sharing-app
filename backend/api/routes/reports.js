const express = require('express');
const router = express.Router();
const { createReport, getMyReports } = require('../controllers/reportsController');

router.post('/', createReport);
router.get('/me', getMyReports);

module.exports = router;
