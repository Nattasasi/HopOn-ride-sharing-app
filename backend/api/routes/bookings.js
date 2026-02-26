const express = require('express');
const router = express.Router();
const { 
  requestBooking, 
  respondToBooking, 
  cancelBooking, 
  getUserBookings, 
  getRideBookings 
} = require('../controllers/bookingsController');

router.post('/', requestBooking);
router.get('/me', getUserBookings);
router.get('/posts/:id', getRideBookings);
router.patch('/:id/respond', respondToBooking);
router.patch('/:id/cancel', cancelBooking);

module.exports = router;