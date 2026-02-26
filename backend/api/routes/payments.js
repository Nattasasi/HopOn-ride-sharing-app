const express = require('express');
const router = express.Router();
const { createPayment, getPayment } = require('../controllers/paymentsController');

router.post('/', createPayment);
router.get('/:bookingId', getPayment);

module.exports = router;