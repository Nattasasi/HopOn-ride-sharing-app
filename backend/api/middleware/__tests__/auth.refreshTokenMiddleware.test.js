const jwt = require('jsonwebtoken');
const User = require('../../models/User');
const { refreshTokenMiddleware } = require('../auth');
const { getMetricsSnapshot, resetMetrics } = require('../metrics');

jest.mock('jsonwebtoken', () => ({
  verify: jest.fn()
}));

jest.mock('../../models/User', () => ({
  findById: jest.fn()
}));

describe('refreshTokenMiddleware', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    resetMetrics();
  });

  it('returns 401 and increments authRefreshFailures when token is missing', async () => {
    const req = {
      header: jest.fn().mockReturnValue(''),
      body: {}
    };
    const res = {
      status: jest.fn().mockReturnThis(),
      json: jest.fn()
    };
    const next = jest.fn();

    await refreshTokenMiddleware(req, res, next);

    expect(res.status).toHaveBeenCalledWith(401);
    expect(res.json).toHaveBeenCalledWith({ message: 'Refresh token is required' });
    expect(next).not.toHaveBeenCalled();
    expect(getMetricsSnapshot().counters.authRefreshFailures).toBe(1);
  });

  it('returns 401 and increments authRefreshFailures when token is invalid', async () => {
    jwt.verify.mockImplementation(() => {
      throw new Error('invalid token');
    });

    const req = {
      header: jest.fn().mockReturnValue('Bearer bad-refresh-token'),
      body: {}
    };
    const res = {
      status: jest.fn().mockReturnThis(),
      json: jest.fn()
    };
    const next = jest.fn();

    await refreshTokenMiddleware(req, res, next);

    expect(res.status).toHaveBeenCalledWith(401);
    expect(res.json).toHaveBeenCalledWith({ message: 'Invalid or expired refresh token' });
    expect(next).not.toHaveBeenCalled();
    expect(getMetricsSnapshot().counters.authRefreshFailures).toBe(1);
  });

  it('attaches req.user and calls next when refresh token is valid', async () => {
    jwt.verify.mockReturnValue({ id: '507f1f77bcf86cd799439011' });
    User.findById.mockReturnValue({
      select: jest.fn().mockResolvedValue({
        _id: '507f1f77bcf86cd799439011',
        role: 'rider'
      })
    });

    const req = {
      header: jest.fn().mockReturnValue('Bearer valid-refresh-token'),
      body: {}
    };
    const res = {
      status: jest.fn().mockReturnThis(),
      json: jest.fn()
    };
    const next = jest.fn();

    await refreshTokenMiddleware(req, res, next);

    expect(next).toHaveBeenCalledTimes(1);
    expect(req.user).toEqual({ id: '507f1f77bcf86cd799439011', role: 'rider' });
    expect(res.status).not.toHaveBeenCalled();
    expect(getMetricsSnapshot().counters.authRefreshFailures).toBe(0);
  });
});
