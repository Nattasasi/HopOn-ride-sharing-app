const { v4: uuidv4 } = require('uuid');
const Payment = require('../models/Payment');

const createPayment = async (req, res) => {
  const payment = new Payment({
    payment_id: uuidv4(),
    ...req.body
  });
  await payment.save();
  res.status(201).json(payment);
};

const getPayment = async (req, res) => {
  const payment = await Payment.findOne({ booking_id: req.params.bookingId });
  res.json(payment);
};

module.exports = { createPayment, getPayment };