const express = require('express');
const router = express.Router();
const { body, header, param } = require('express-validator');
const { createPayment, getPayment } = require('../controllers/paymentsController');
const {
	handleValidationErrors,
	rejectUnknownBodyFields
} = require('../middleware/validation');

const paymentCreateValidation = [
	body('booking_id').isMongoId().withMessage('booking_id must be a valid booking id'),
	body('amount').isFloat({ gt: 0 }).withMessage('amount must be a number greater than 0'),
	body('status')
		.isIn(['pending', 'completed', 'paid', 'failed', 'refunded'])
		.withMessage('status must be one of pending, completed, paid, failed, refunded'),
	body('transaction_ref')
		.optional({ nullable: true })
		.isString()
		.isLength({ min: 1, max: 128 })
		.withMessage('transaction_ref must be a non-empty string up to 128 characters'),
	body('payment_type')
		.optional({ nullable: true })
		.isString()
		.isLength({ min: 1, max: 40 })
		.withMessage('payment_type must be a non-empty string up to 40 characters'),
	header('x-idempotency-key')
		.optional()
		.isString()
		.isLength({ min: 1, max: 128 })
		.withMessage('x-idempotency-key must be a non-empty string up to 128 characters'),
	rejectUnknownBodyFields(['booking_id', 'amount', 'status', 'transaction_ref', 'payment_type']),
	handleValidationErrors
];

const paymentGetValidation = [
	param('bookingId').isMongoId().withMessage('bookingId must be a valid booking id'),
	handleValidationErrors
];

router.post('/', paymentCreateValidation, createPayment);
router.get('/:bookingId', paymentGetValidation, getPayment);

module.exports = router;