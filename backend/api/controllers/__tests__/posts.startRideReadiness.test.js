jest.mock('uuid', () => ({
  v4: () => 'test-post-uuid'
}));

jest.mock('../../models/CarpoolPost', () => ({
  findById: jest.fn()
}));

jest.mock('../../models/Booking', () => ({
  find: jest.fn()
}));

jest.mock('../../models/User', () => ({}));
jest.mock('../../models/Feedback', () => ({}));

const CarpoolPost = require('../../models/CarpoolPost');
const Booking = require('../../models/Booking');
const { startPostRide } = require('../postsController');

const makeChain = (result) => ({
  select: jest.fn().mockReturnThis(),
  lean: jest.fn().mockResolvedValue(result),
  then: (onFulfilled, onRejected) => Promise.resolve(result).then(onFulfilled, onRejected),
  catch: (onRejected) => Promise.resolve(result).catch(onRejected)
});

const makeReq = (overrides = {}) => {
  const ioRoom = { emit: jest.fn() };
  const io = { to: jest.fn(() => ioRoom) };
  return {
    params: { id: 'post-mongo-id' },
    user: { id: 'driver-id', role: 'driver' },
    app: { get: jest.fn(() => io) },
    ...overrides
  };
};

const makeRes = () => ({
  status: jest.fn().mockReturnThis(),
  json: jest.fn()
});

const makePost = (overrides = {}) => ({
  _id: { toString: () => 'post-mongo-id' },
  post_id: 'post-uuid-id',
  driver_id: { toString: () => 'driver-id' },
  status: 'active',
  departure_time: new Date(Date.now() - 60 * 60 * 1000).toISOString(),
  wait_time_minutes: 0,
  save: jest.fn().mockResolvedValue(undefined),
  ...overrides
});

describe('start ride readiness enforcement', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('blocks ride start when there are pending booking requests', async () => {
    CarpoolPost.findById.mockResolvedValue(makePost());
    Booking.find.mockResolvedValue([
      { status: 'pending', pickup_status: 'not_arrived' }
    ]);

    const req = makeReq();
    const res = makeRes();

    await startPostRide(req, res);

    expect(res.status).toHaveBeenCalledWith(409);
    expect(res.json).toHaveBeenCalledWith({
      code: 'RIDE_START_CHECKLIST_INCOMPLETE',
      message: 'Review the pending booking request before starting the ride.',
      blockers: [
        {
          code: 'PENDING_BOOKING_REQUESTS',
          message: 'Review the pending booking request before starting the ride.'
        }
      ]
    });
  });

  it('blocks ride start when confirmed passengers are not boarded and the wait timer is still active', async () => {
    CarpoolPost.findById.mockResolvedValue(
      makePost({
        departure_time: new Date(Date.now() + 20 * 60 * 1000).toISOString(),
        wait_time_minutes: 10
      })
    );
    Booking.find.mockResolvedValue([
      { status: 'confirmed', pickup_status: 'arrived' },
      { status: 'confirmed', pickup_status: 'boarded' }
    ]);

    const req = makeReq();
    const res = makeRes();

    await startPostRide(req, res);

    expect(res.status).toHaveBeenCalledWith(409);
    expect(res.json.mock.calls[0][0]).toEqual({
      code: 'RIDE_START_CHECKLIST_INCOMPLETE',
      message: 'A confirmed passenger is not boarded yet.',
      blockers: [
        {
          code: 'UNBOARDED_CONFIRMED_PASSENGERS',
          message: 'A confirmed passenger is not boarded yet.'
        },
        {
          code: 'WAIT_TIMER_ACTIVE',
          message: 'Wait timer is still active. Please wait before starting the ride.',
          wait_seconds_remaining: expect.any(Number)
        }
      ],
      wait_seconds_remaining: expect.any(Number)
    });
  });

  it('keeps pending-request blocking even while the wait timer is active', async () => {
    CarpoolPost.findById.mockResolvedValue(
      makePost({
        departure_time: new Date(Date.now() + 20 * 60 * 1000).toISOString(),
        wait_time_minutes: 10
      })
    );
    Booking.find.mockResolvedValue([
      { status: 'pending', pickup_status: 'not_arrived' },
      { status: 'accepted', pickup_status: 'boarded' }
    ]);

    const req = makeReq();
    const res = makeRes();

    await startPostRide(req, res);

    expect(res.status).toHaveBeenCalledWith(409);
    expect(res.json).toHaveBeenCalledWith({
      code: 'RIDE_START_CHECKLIST_INCOMPLETE',
      message: 'Review the pending booking request before starting the ride.',
      blockers: [
        {
          code: 'PENDING_BOOKING_REQUESTS',
          message: 'Review the pending booking request before starting the ride.'
        }
      ]
    });
  });

  it('starts the ride after the wait timer expires and marks missing confirmed riders left behind', async () => {
    const post = makePost({
      departure_time: new Date(Date.now() - 20 * 60 * 1000).toISOString(),
      wait_time_minutes: 10
    });
    const leftBehindBooking = {
      status: 'confirmed',
      pickup_status: 'arrived',
      passenger_id: { toString: () => 'passenger-1' },
      _id: { toString: () => 'booking-1' },
      save: jest.fn().mockResolvedValue(undefined)
    };
    const boardedBooking = {
      status: 'confirmed',
      pickup_status: 'boarded',
      passenger_id: { toString: () => 'passenger-2' },
      _id: { toString: () => 'booking-2' },
      save: jest.fn().mockResolvedValue(undefined)
    };
    CarpoolPost.findById.mockResolvedValue(post);
    Booking.find
      .mockResolvedValueOnce([leftBehindBooking, boardedBooking])
      .mockReturnValueOnce(makeChain([
        { passenger_id: { toString: () => 'passenger-1' } },
        { passenger_id: { toString: () => 'passenger-2' } }
      ]));

    const req = makeReq();
    const res = makeRes();

    await startPostRide(req, res);

    expect(leftBehindBooking.pickup_status).toBe('left_behind');
    expect(leftBehindBooking.save).toHaveBeenCalledTimes(1);
    expect(post.status).toBe('in_progress');
    expect(post.save).toHaveBeenCalledTimes(1);
    expect(res.json).toHaveBeenCalledWith(post);
  });

  it('starts the ride when the checklist is clear', async () => {
    const post = makePost();
    CarpoolPost.findById.mockResolvedValue(post);
    Booking.find
      .mockResolvedValueOnce([
        { status: 'confirmed', pickup_status: 'boarded' },
        { status: 'accepted', pickup_status: 'boarded' }
      ])
      .mockReturnValueOnce(makeChain([]));

    const req = makeReq();
    const res = makeRes();

    await startPostRide(req, res);

    expect(post.status).toBe('in_progress');
    expect(post.save).toHaveBeenCalledTimes(1);
    expect(res.json).toHaveBeenCalledWith(post);
  });
});