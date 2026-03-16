jest.mock('uuid', () => ({
  v4: () => 'booking-test-uuid'
}));

jest.mock('../../models/Booking', () => ({
  findById: jest.fn()
}));

jest.mock('../../models/CarpoolPost', () => ({
  findOne: jest.fn(),
  findByIdAndUpdate: jest.fn()
}));

jest.mock('../../middleware/metrics', () => ({
  incrementCounter: jest.fn()
}));

jest.mock('../../services/pushNotifications', () => ({
  sendToUsers: jest.fn(() => Promise.resolve())
}));

const Booking = require('../../models/Booking');
const CarpoolPost = require('../../models/CarpoolPost');
const { respondToBooking } = require('../bookingsController');

const makeRes = () => ({
  status: jest.fn().mockReturnThis(),
  json: jest.fn()
});

const makeReq = (status) => {
  const emit = jest.fn();
  return {
    params: { id: 'booking-mongo-id' },
    body: { status },
    user: { id: 'driver-1' },
    correlationId: 'corr-123',
    app: {
      get: jest.fn().mockReturnValue({
        to: jest.fn().mockReturnValue({ emit })
      })
    }
  };
};

describe('respondToBooking transition behavior', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('accept transition sets booking to confirmed and decrements available seats', async () => {
    const booking = {
      _id: { toString: () => 'booking-mongo-id' },
      post_id: 'post-uuid-1',
      passenger_id: { toString: () => 'passenger-1' },
      seats_booked: 2,
      status: 'pending',
      booked_at: new Date(),
      save: jest.fn().mockResolvedValue(undefined)
    };

    const post = {
      _id: { toString: () => 'post-mongo-id-1' },
      post_id: 'post-uuid-1',
      driver_id: { toString: () => 'driver-1' },
      available_seats: 3,
      save: jest.fn().mockResolvedValue(undefined)
    };

    Booking.findById.mockResolvedValue(booking);
    CarpoolPost.findOne.mockResolvedValue(post);

    const req = makeReq('accepted');
    const res = makeRes();

    await respondToBooking(req, res);

    expect(booking.status).toBe('confirmed');
    expect(post.available_seats).toBe(1);
    expect(post.save).toHaveBeenCalledTimes(1);
    expect(booking.save).toHaveBeenCalledTimes(1);
    expect(res.status).not.toHaveBeenCalled();
    expect(res.json).toHaveBeenCalledWith(booking);
  });

  it('reject transition sets booking to rejected and does not decrement seats', async () => {
    const booking = {
      _id: { toString: () => 'booking-mongo-id' },
      post_id: 'post-uuid-2',
      passenger_id: { toString: () => 'passenger-2' },
      seats_booked: 1,
      status: 'pending',
      booked_at: new Date(),
      save: jest.fn().mockResolvedValue(undefined)
    };

    const post = {
      _id: { toString: () => 'post-mongo-id-2' },
      post_id: 'post-uuid-2',
      driver_id: { toString: () => 'driver-1' },
      available_seats: 4,
      save: jest.fn().mockResolvedValue(undefined)
    };

    Booking.findById.mockResolvedValue(booking);
    CarpoolPost.findOne.mockResolvedValue(post);

    const req = makeReq('rejected');
    const res = makeRes();

    await respondToBooking(req, res);

    expect(booking.status).toBe('rejected');
    expect(post.available_seats).toBe(4);
    expect(post.save).not.toHaveBeenCalled();
    expect(booking.save).toHaveBeenCalledTimes(1);
    expect(res.status).not.toHaveBeenCalled();
    expect(res.json).toHaveBeenCalledWith(booking);
  });
});
