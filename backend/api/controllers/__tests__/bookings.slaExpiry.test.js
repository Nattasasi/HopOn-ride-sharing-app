jest.mock('uuid', () => ({
  v4: () => 'test-booking-uuid'
}));

jest.mock('../../models/Booking', () => {
  const BookingCtor = jest.fn(function Booking(doc) {
    Object.assign(this, doc);
    this._id = { toString: () => 'new-booking-id' };
    this.save = jest.fn().mockResolvedValue(undefined);
  });

  BookingCtor.find = jest.fn();
  BookingCtor.findOne = jest.fn();
  BookingCtor.findById = jest.fn();

  return BookingCtor;
});

jest.mock('../../models/CarpoolPost', () => ({
  findOne: jest.fn(),
  find: jest.fn(),
  findByIdAndUpdate: jest.fn()
}));

const Booking = require('../../models/Booking');
const CarpoolPost = require('../../models/CarpoolPost');
const {
  requestBooking,
  respondToBooking,
  getUserBookings
} = require('../bookingsController');

describe('booking SLA expiry behavior', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    process.env.BOOKING_REQUEST_SLA_MINUTES = '10';
  });

  const makeRes = () => ({
    status: jest.fn().mockReturnThis(),
    json: jest.fn()
  });

  it('respondToBooking returns BOOKING_REQUEST_EXPIRED when pending booking is stale', async () => {
    const save = jest.fn().mockResolvedValue(undefined);
    const booking = {
      _id: { toString: () => 'booking-mongo-id' },
      post_id: 'post-uuid',
      passenger_id: { toString: () => 'passenger-id' },
      seats_booked: 1,
      status: 'pending',
      booked_at: new Date(Date.now() - 12 * 60 * 1000),
      save
    };

    const post = {
      _id: { toString: () => 'post-mongo-id' },
      post_id: 'post-uuid',
      driver_id: { toString: () => 'driver-id' }
    };

    Booking.findById.mockResolvedValue(booking);
    CarpoolPost.findOne.mockResolvedValue(post);

    const req = {
      params: { id: 'booking-mongo-id' },
      body: { status: 'accepted' },
      user: { id: 'driver-id' },
      app: { get: jest.fn().mockReturnValue(null) }
    };
    const res = makeRes();

    await respondToBooking(req, res);

    expect(booking.status).toBe('expired');
    expect(save).toHaveBeenCalledTimes(1);
    expect(res.status).toHaveBeenCalledWith(409);
    expect(res.json).toHaveBeenCalledWith({
      code: 'BOOKING_REQUEST_EXPIRED',
      message: 'This booking request has expired before response.'
    });
  });

  it('requestBooking expires stale pending booking before active booking conflict check', async () => {
    const stalePending = {
      status: 'pending',
      booked_at: new Date(Date.now() - 11 * 60 * 1000),
      passenger_id: { toString: () => 'rider-1' },
      _id: { toString: () => 'stale-booking' },
      post_id: 'old-post-uuid',
      save: jest.fn().mockResolvedValue(undefined)
    };

    const post = {
      _id: { toString: () => 'post-mongo-id' },
      post_id: 'target-post-uuid',
      driver_id: { toString: () => 'driver-1' },
      status: 'active',
      departure_time: new Date(Date.now() + 60 * 60 * 1000).toISOString(),
      available_seats: 3
    };

    Booking.find
      .mockResolvedValueOnce([stalePending])
      .mockReturnValueOnce({ sort: jest.fn().mockReturnThis(), lean: jest.fn().mockResolvedValue([]) });
    Booking.findOne.mockResolvedValue(null);

    CarpoolPost.findOne.mockResolvedValue(post);
    CarpoolPost.find.mockResolvedValue([]);

    const req = {
      body: { post_id: 'target-post-uuid', seats_booked: 1 },
      user: { id: 'rider-1' },
      app: { get: jest.fn().mockReturnValue(null) }
    };
    const res = makeRes();

    await requestBooking(req, res);

    expect(stalePending.status).toBe('expired');
    expect(stalePending.save).toHaveBeenCalled();
    expect(Booking).toHaveBeenCalledWith(expect.objectContaining({
      post_id: 'target-post-uuid',
      status: 'pending'
    }));
    expect(res.status).toHaveBeenCalledWith(201);
  });

  it('getUserBookings expires stale pending bookings before returning list', async () => {
    const staleForPassenger = {
      status: 'pending',
      booked_at: new Date(Date.now() - 20 * 60 * 1000),
      passenger_id: { toString: () => 'rider-3' },
      _id: { toString: () => 'stale-user-booking' },
      post_id: 'ride-post-uuid',
      save: jest.fn().mockResolvedValue(undefined)
    };

    Booking.find
      .mockResolvedValueOnce([staleForPassenger])
      .mockReturnValueOnce({ sort: jest.fn().mockReturnThis(), lean: jest.fn().mockResolvedValue([]) });

    const req = {
      user: { id: 'rider-3' },
      app: { get: jest.fn().mockReturnValue(null) }
    };
    const res = makeRes();

    await getUserBookings(req, res);

    expect(staleForPassenger.status).toBe('expired');
    expect(staleForPassenger.save).toHaveBeenCalled();
    expect(res.json).toHaveBeenCalledWith([]);
  });
});
