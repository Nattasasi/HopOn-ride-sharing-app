const express = require('express');
const router = express.Router();
const { createEmergency, getEmergencies } = require('../controllers/emergencyController');

router.post('/', createEmergency);
router.get('/', getEmergencies);

module.exports = router;