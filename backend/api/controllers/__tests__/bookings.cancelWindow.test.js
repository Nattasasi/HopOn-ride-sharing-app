jest.mock('uuid', () => ({
  v4: () => 'test-cancel-window-booking-uuid'
}));

jest.mock('../../models/Booking', () => {
  const BookingCtor = jest.fn(function Booking(doc) {
    Object.assign(this, doc);
    this._id = { toString: () => 'cancel-window-booking-id' };
    this.booked_at = doc.booked_at ?? new Date();
    this.save = jest.fn().mockResolvedValue(undefined);
    this.toObject = jest.fn().mockImplementation(() => ({
      _id: this._id,
      booking_id: this.booking_id,
      post_id: this.post_id,
      passenger_id: this.passenger_id,
      seats_booked: this.seats_booked,
      status: this.status,
      pickup_status: this.pickup_status,
      booked_at: this.booked_at
    }));
  });
  BookingCtor.find = jest.fn();
  BookingCtor.findOne = jest.fn();
  BookingCtor.findById = jest.fn();
  return BookingCtor;
});

jest.mock('../../models/CarpoolPost', () => ({
  findOne: jest.fn(),
  find: jest.fn()
}));

const Booking = require('../../models/Booking');
const CarpoolPost = require('../../models/CarpoolPost');
const { requestBooking, getUserBookings } = require('../bookingsController');

/**
 * Creates a chainable mock compatible with patterns like:
 *   await Model.find()                   -- direct await (thenable)
 *   await Model.find().sort().lean()     -- chained sort + lean
 *   await Model.findOne().populate()     -- chained populate
 */
const makeChain = (result) => {
  const chain = {
    sort: jest.fn().mockReturnThis(),
    lean: jest.fn().mockResolvedValue(result),
    select: jest.fn().mockReturnThis(),
    populate: jest.fn().mockReturnThis(),
    toObject: jest.fn().mockReturnValue(result),
    // Make the chain thenable so `await chain` resolves to result
    then: (onFulfilled, onRejected) => Promise.resolve(result).then(onFulfilled, onRejected),
    catch: (onRejected) => Promise.resolve(result).catch(onRejected),
  };
  return chain;
};

describe('booking free-cancel window', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    process.env.FREE_CANCEL_WINDOW_MINUTES = '15';
    process.env.BOOKING_REQUEST_SLA_MINUTES = '10';
  });

  const makeRes = () => ({
    status: jest.fn().mockReturnThis(),
    json: jest.fn()
  });

  const makeReq = (overrides = {}) => ({
    body: { post_id: 'post-uuid-001', seats_booked: 1 },
    user: { id: 'passenger-id-001' },
    app: { get: jest.fn(() => null) },
    correlationId: 'test-corr-id',
    ...overrides
  });

  it('requestBooking response includes cancellation_cutoff_at and seconds_until_free_cancel for a new pending booking', async () => {
    // First find: expiry sweep (direct await, no chain needed but makeChain handles both)
    // Second find: active status conflict check (.sort().lean() chained)
    Booking.find
      .mockReturnValueOnce(makeChain([]))   // expiry sweep
      .mockReturnValueOnce(makeChain([]));  // active status bookings check
    Booking.findOne.mockResolvedValue(null);

    const activePost = {
      _id: { toString: () => 'post-mongo-id' },
      post_id: 'post-uuid-001',
      driver_id: { toString: () => 'driver-id' },
      status: 'active',
      available_seats: 3,
      departure_time: new Date(Date.now() + 3 * 60 * 60 * 1000) // 3h from now
    };
    CarpoolPost.findOne.mockResolvedValue(activePost);

    const req = makeReq();
    const res = makeRes();

    await requestBooking(req, res);

    expect(res.status).toHaveBeenCalledWith(201);
    const payload = res.json.mock.calls[0][0];
    expect(payload).toHaveProperty('cancellation_cutoff_at');
    expect(payload).toHaveProperty('seconds_until_free_cancel');

    // cutoff should be ~15 minutes in the future
    const cutoffMs = new Date(payload.cancellation_cutoff_at).getTime();
    expect(cutoffMs).toBeGreaterThan(Date.now());
    expect(cutoffMs).toBeLessThan(Date.now() + 16 * 60 * 1000);

    // seconds_until_free_cancel should be positive and ≤ FREE_CANCEL_WINDOW length
    expect(payload.seconds_until_free_cancel).toBeGreaterThan(0);
    expect(payload.seconds_until_free_cancel).toBeLessThanOrEqual(15 * 60);
  });

  it('getUserBookings attaches cancel window to pending bookings', async () => {
    const bookedAt = new Date(Date.now() - 2 * 60 * 1000); // 2 min ago → still in 15-min window
    const lean = {
      _id: 'b-id',
      booking_id: 'b-uuid',
      post_id: 'post-uuid-001',
      passenger_id: 'passenger-id-001',
      status: 'pending',
      pickup_status: 'not_arrived',
      booked_at: bookedAt
    };

    Booking.find
      .mockReturnValueOnce(makeChain([]))     // expiry sweep (direct await)
      .mockReturnValueOnce(makeChain([lean])); // user bookings (.sort().lean())

    const postObj = { post_id: 'post-uuid-001' };
    CarpoolPost.findOne.mockReturnValue(
      makeChain({ ...postObj, toObject: () => postObj })
    );

    const req = makeReq({ body: {}, user: { id: 'passenger-id-001' } });
    const res = makeRes();

    await getUserBookings(req, res);

    const bookings = res.json.mock.calls[0][0];
    expect(bookings).toHaveLength(1);
    expect(bookings[0]).toHaveProperty('cancellation_cutoff_at');
    expect(bookings[0].seconds_until_free_cancel).toBeGreaterThan(0);
  });

  it('getUserBookings does NOT attach cancel window to cancelled bookings', async () => {
    const lean = {
      _id: 'b-id-2',
      booking_id: 'b-uuid-2',
      post_id: 'post-uuid-002',
      passenger_id: 'passenger-id-001',
      status: 'cancelled',
      pickup_status: 'not_arrived',
      booked_at: new Date(Date.now() - 2 * 60 * 1000)
    };

    Booking.find
      .mockReturnValueOnce(makeChain([]))     // expiry sweep
      .mockReturnValueOnce(makeChain([lean])); // user bookings

    const postObj = { post_id: 'post-uuid-002' };
    CarpoolPost.findOne.mockReturnValue(
      makeChain({ ...postObj, toObject: () => postObj })
    );

    const req = makeReq({ body: {}, user: { id: 'passenger-id-001' } });
    const res = makeRes();

    await getUserBookings(req, res);

    const bookings = res.json.mock.calls[0][0];
    expect(bookings).toHaveLength(1);
    expect(bookings[0]).not.toHaveProperty('cancellation_cutoff_at');
  });
});
