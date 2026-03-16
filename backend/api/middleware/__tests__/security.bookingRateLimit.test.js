const express = require('express');
const request = require('supertest');

describe('bookingMutationRateLimiter', () => {
  beforeEach(() => {
    process.env.BOOKING_MUTATION_RATE_LIMIT_MAX = '2';
    jest.resetModules();
  });

  afterAll(() => {
    delete process.env.BOOKING_MUTATION_RATE_LIMIT_MAX;
  });

  it('returns 429 after configured request threshold is exceeded', async () => {
    const { bookingMutationRateLimiter } = require('../security');

    const app = express();
    app.use(express.json());
    app.post('/bookings/mutate', bookingMutationRateLimiter, (req, res) => {
      res.status(200).json({ ok: true });
    });

    const first = await request(app).post('/bookings/mutate').send({});
    const second = await request(app).post('/bookings/mutate').send({});
    const third = await request(app).post('/bookings/mutate').send({});

    expect(first.status).toBe(200);
    expect(second.status).toBe(200);
    expect(third.status).toBe(429);
    expect(third.body).toEqual({ message: 'Too many booking updates. Please try again later.' });
  });
});
