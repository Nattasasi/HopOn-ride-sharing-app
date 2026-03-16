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
const { bookingMutationRateLimiter } = require('../middleware/security');

router.post('/', bookingMutationRateLimiter, requestBooking);
router.get('/me', getUserBookings);
router.get('/posts/:id', getRideBookings);
router.patch('/:id/respond', bookingMutationRateLimiter, respondToBooking);
router.patch('/:id/arrive', bookingMutationRateLimiter, markBookingArrived);
router.patch('/:id/confirm-boarded', bookingMutationRateLimiter, confirmPassengerBoarded);
router.patch('/:id/cancel', bookingMutationRateLimiter, cancelBooking);

module.exports = router;
