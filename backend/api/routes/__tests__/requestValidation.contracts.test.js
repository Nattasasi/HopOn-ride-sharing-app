const express = require('express');
const request = require('supertest');

jest.mock('../../controllers/bookingsController', () => ({
  requestBooking: jest.fn((req, res) => res.status(201).json({ ok: true })),
  respondToBooking: jest.fn((req, res) => res.status(200).json({ ok: true })),
  markBookingArrived: jest.fn((req, res) => res.status(200).json({ ok: true })),
  confirmPassengerBoarded: jest.fn((req, res) => res.status(200).json({ ok: true })),
  cancelBooking: jest.fn((req, res) => res.status(200).json({ ok: true })),
  getUserBookings: jest.fn((req, res) => res.status(200).json([])),
  getRideBookings: jest.fn((req, res) => res.status(200).json([]))
}));

jest.mock('../../controllers/paymentsController', () => ({
  createPayment: jest.fn((req, res) => res.status(201).json({ ok: true })),
  getPayment: jest.fn((req, res) => res.status(200).json({ ok: true }))
}));

jest.mock('../../middleware/security', () => ({
  bookingMutationRateLimiter: (req, res, next) => next()
}));

const bookingControllers = require('../../controllers/bookingsController');
const paymentControllers = require('../../controllers/paymentsController');
const bookingsRouter = require('../bookings');
const paymentsRouter = require('../payments');

describe('request validation contracts', () => {
  let app;

  beforeEach(() => {
    jest.clearAllMocks();
    app = express();
    app.use(express.json());
    app.use((req, res, next) => {
      req.user = { id: 'user-1', role: 'driver' };
      next();
    });
    app.use('/bookings', bookingsRouter);
    app.use('/payments', paymentsRouter);
  });

  it('rejects booking create request with invalid UUID post_id', async () => {
    const res = await request(app)
      .post('/bookings')
      .send({ post_id: 'not-a-uuid' });

    expect(res.status).toBe(400);
    expect(res.body.message).toBe('Validation failed');
    expect(bookingControllers.requestBooking).not.toHaveBeenCalled();
  });

  it('rejects booking respond request with invalid status value', async () => {
    const bookingId = '507f1f77bcf86cd799439011';
    const res = await request(app)
      .patch(`/bookings/${bookingId}/respond`)
      .send({ status: 'approved' });

    expect(res.status).toBe(400);
    expect(res.body.message).toBe('Validation failed');
    expect(bookingControllers.respondToBooking).not.toHaveBeenCalled();
  });

  it('rejects payment create request with unknown fields', async () => {
    const res = await request(app)
      .post('/payments')
      .send({
        booking_id: '507f1f77bcf86cd799439011',
        amount: 100,
        status: 'completed',
        unexpected_field: 'boom'
      });

    expect(res.status).toBe(400);
    expect(res.body.message).toBe('Validation failed');
    expect(paymentControllers.createPayment).not.toHaveBeenCalled();
  });

  it('accepts valid payment creation payload and reaches controller', async () => {
    const res = await request(app)
      .post('/payments')
      .set('x-idempotency-key', 'idem-123')
      .send({
        booking_id: '507f1f77bcf86cd799439011',
        amount: 120.5,
        status: 'completed',
        payment_type: 'cash'
      });

    expect(res.status).toBe(201);
    expect(paymentControllers.createPayment).toHaveBeenCalledTimes(1);
  });

  it('rejects payment get request with invalid booking id param', async () => {
    const res = await request(app).get('/payments/not-an-id');

    expect(res.status).toBe(400);
    expect(res.body.message).toBe('Validation failed');
    expect(paymentControllers.getPayment).not.toHaveBeenCalled();
  });
});
