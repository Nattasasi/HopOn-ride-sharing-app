const express = require('express');
const router = express.Router();
const { 
  requestBooking, 
  respondToBooking, 
  markBookingArrived,
  confirmPassengerBoarded,
  cancelBooking, 
  getUserBookings, 
  getRideBookings 
} = require('../controllers/bookingsController');

router.post('/', requestBooking);
router.get('/me', getUserBookings);
router.get('/posts/:id', getRideBookings);
router.patch('/:id/respond', respondToBooking);
router.patch('/:id/arrive', markBookingArrived);
router.patch('/:id/confirm-boarded', confirmPassengerBoarded);
router.patch('/:id/cancel', cancelBooking);

module.exports = router;
