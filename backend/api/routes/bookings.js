const express = require('express');
const router = express.Router();
const { body, param } = require('express-validator');
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
const {
  handleValidationErrors,
  rejectUnknownBodyFields
} = require('../middleware/validation');

const bookingIdParamValidation = [
  param('id').isMongoId().withMessage('id must be a valid booking id'),
  handleValidationErrors
];

const postUuidParamValidation = [
  param('id').isUUID().withMessage('id must be a valid post uuid'),
  handleValidationErrors
];

const createBookingValidation = [
  body('post_id').isUUID().withMessage('post_id must be a valid UUID'),
  body('seats_booked')
    .optional()
    .isInt({ min: 1, max: 8 })
    .withMessage('seats_booked must be an integer between 1 and 8'),
  rejectUnknownBodyFields(['post_id', 'seats_booked']),
  handleValidationErrors
];

const respondBookingValidation = [
  ...bookingIdParamValidation,
  body('status')
    .isIn(['accepted', 'rejected'])
    .withMessage('status must be accepted or rejected'),
  rejectUnknownBodyFields(['status']),
  handleValidationErrors
];

router.post('/', bookingMutationRateLimiter, createBookingValidation, requestBooking);
router.get('/me', getUserBookings);
router.get('/posts/:id', postUuidParamValidation, getRideBookings);
router.patch('/:id/respond', bookingMutationRateLimiter, respondBookingValidation, respondToBooking);
router.patch('/:id/arrive', bookingMutationRateLimiter, bookingIdParamValidation, markBookingArrived);
router.patch('/:id/confirm-boarded', bookingMutationRateLimiter, bookingIdParamValidation, confirmPassengerBoarded);
router.patch('/:id/cancel', bookingMutationRateLimiter, bookingIdParamValidation, cancelBooking);

module.exports = router;
