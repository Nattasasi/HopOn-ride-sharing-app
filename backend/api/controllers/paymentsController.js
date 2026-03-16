const { v4: uuidv4 } = require('uuid');
const Payment = require('../models/Payment');
const { incrementCounter } = require('../middleware/metrics');

const toPaymentResponse = (paymentLike) => {
  if (!paymentLike) return null;
  const payment = typeof paymentLike.toObject === 'function' ? paymentLike.toObject() : paymentLike;
  return {
    _id: payment._id,
    payment_id: payment.payment_id,
    booking_id: payment.booking_id,
    amount: payment.amount,
    status: payment.status,
    transaction_ref: payment.transaction_ref || null,
    payment_type: payment.payment_type || null,
    payment_date: payment.payment_date || null,
    created_at: payment.created_at,
    idempotency_key: payment.idempotency_key || null
  };
};

const createPayment = async (req, res) => {
  const idempotencyKey = req.header('x-idempotency-key')?.trim();

  try {
    if (idempotencyKey) {
      const existing = await Payment.findOne({ idempotency_key: idempotencyKey });
      if (existing) {
        return res.status(200).json(toPaymentResponse(existing));
      }
    }

    const payment = new Payment({
      payment_id: uuidv4(),
      ...(idempotencyKey ? { idempotency_key: idempotencyKey } : {}),
      ...req.body
    });

    await payment.save();
    return res.status(201).json(toPaymentResponse(payment));
  } catch (error) {
    // Handle races where two requests with the same idempotency key arrive concurrently.
    if (idempotencyKey && error?.code === 11000) {
      const existing = await Payment.findOne({ idempotency_key: idempotencyKey });
      if (existing) {
        return res.status(200).json(toPaymentResponse(existing));
      }
    }
    incrementCounter('paymentCreateFailures');
    return res.status(500).json({ message: 'Failed to create payment', error: error.message });
  }
};

const getPayment = async (req, res) => {
  try {
    const payment = await Payment.findOne({ booking_id: req.params.bookingId });
    if (!payment) {
      return res.status(404).json({ message: 'Payment not found' });
    }
    return res.json(toPaymentResponse(payment));
  } catch (error) {
    incrementCounter('paymentFetchFailures');
    return res.status(500).json({ message: 'Failed to fetch payment', error: error.message });
  }
};

module.exports = { createPayment, getPayment };