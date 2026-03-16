jest.mock('../../models/Payment', () => {
  const PaymentCtor = jest.fn(function Payment(doc) {
    Object.assign(this, doc);
    this._id = 'payment-mongo-id';
    this.save = jest.fn().mockResolvedValue(undefined);
  });

  PaymentCtor.findOne = jest.fn();
  return PaymentCtor;
});

jest.mock('uuid', () => ({
  v4: () => 'payment-uuid-123'
}));

const Payment = require('../../models/Payment');
const { createPayment, getPayment } = require('../paymentsController');

describe('paymentsController', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('createPayment returns 201 with persisted payment payload', async () => {
    const req = {
      header: jest.fn().mockReturnValue(undefined),
      body: {
        booking_id: 'booking-mongo-id',
        amount: 120,
        status: 'paid',
        transaction_ref: 'txn-ref-1',
        payment_type: 'promptpay'
      }
    };
    const res = {
      status: jest.fn().mockReturnThis(),
      json: jest.fn()
    };

    await createPayment(req, res);

    expect(Payment.findOne).not.toHaveBeenCalled();

    expect(Payment).toHaveBeenCalledWith(
      expect.objectContaining({
        payment_id: 'payment-uuid-123',
        booking_id: 'booking-mongo-id',
        amount: 120,
        status: 'paid'
      })
    );

    expect(Payment).toHaveBeenCalledTimes(1);
    expect(res.status).toHaveBeenCalledWith(201);
    const responsePayload = res.json.mock.calls[0][0];
    expect(responsePayload).toEqual(
      expect.objectContaining({
        payment_id: 'payment-uuid-123',
        booking_id: 'booking-mongo-id',
        amount: 120,
        status: 'paid'
      })
    );
  });

  it('getPayment returns payment found by booking id', async () => {
    const found = {
      _id: 'payment-mongo-id',
      payment_id: 'payment-uuid-123',
      booking_id: 'booking-mongo-id',
      amount: 120,
      status: 'paid'
    };

    Payment.findOne.mockResolvedValue(found);

    const req = { params: { bookingId: 'booking-mongo-id' } };
    const res = { status: jest.fn().mockReturnThis(), json: jest.fn() };

    await getPayment(req, res);

    expect(Payment.findOne).toHaveBeenCalledWith({ booking_id: 'booking-mongo-id' });
    expect(res.status).not.toHaveBeenCalled();
    expect(res.json).toHaveBeenCalledWith(
      expect.objectContaining({
        _id: 'payment-mongo-id',
        payment_id: 'payment-uuid-123',
        booking_id: 'booking-mongo-id',
        amount: 120,
        status: 'paid',
        transaction_ref: null,
        payment_type: null,
        payment_date: null,
        idempotency_key: null
      })
    );
  });

  it('createPayment returns existing payment when idempotency key is replayed', async () => {
    const existing = {
      payment_id: 'payment-uuid-existing',
      idempotency_key: 'idem-key-1',
      booking_id: 'booking-mongo-id',
      amount: 120,
      status: 'paid'
    };

    Payment.findOne.mockResolvedValue(existing);

    const req = {
      header: jest.fn((name) => (name === 'x-idempotency-key' ? 'idem-key-1' : undefined)),
      body: {
        booking_id: 'booking-mongo-id',
        amount: 120,
        status: 'paid'
      }
    };
    const res = {
      status: jest.fn().mockReturnThis(),
      json: jest.fn()
    };

    await createPayment(req, res);

    expect(Payment.findOne).toHaveBeenCalledWith({ idempotency_key: 'idem-key-1' });
    expect(Payment).not.toHaveBeenCalled();
    expect(res.status).toHaveBeenCalledWith(200);
    expect(res.json).toHaveBeenCalledWith(
      expect.objectContaining({
        payment_id: 'payment-uuid-existing',
        idempotency_key: 'idem-key-1',
        booking_id: 'booking-mongo-id',
        amount: 120,
        status: 'paid'
      })
    );
  });

  it('getPayment returns 404 when no payment exists', async () => {
    Payment.findOne.mockResolvedValue(null);

    const req = { params: { bookingId: 'missing-booking' } };
    const res = { status: jest.fn().mockReturnThis(), json: jest.fn() };

    await getPayment(req, res);

    expect(res.status).toHaveBeenCalledWith(404);
    expect(res.json).toHaveBeenCalledWith({ message: 'Payment not found' });
  });
});
