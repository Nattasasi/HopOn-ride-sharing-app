jest.mock('uuid', () => ({
  v4: () => 'auth-test-uuid'
}));

jest.mock('jsonwebtoken', () => ({
  sign: jest.fn()
}));

jest.mock('../../models/User', () => ({
  findById: jest.fn()
}));

const User = require('../../models/User');
const jwt = require('jsonwebtoken');
const { refresh } = require('../authController');

describe('authController.refresh', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    process.env.JWT_SECRET = 'test-access-secret';
    process.env.JWT_REFRESH_SECRET = 'test-refresh-secret';
  });

  it('returns rotated access + refresh tokens and persists the new refresh hash', async () => {
    jwt.sign
      .mockReturnValueOnce('new-access-token')
      .mockReturnValueOnce('new-refresh-token');

    const save = jest.fn().mockResolvedValue(undefined);
    User.findById.mockReturnValue({
      select: jest.fn().mockResolvedValue({
        _id: 'user-123',
        role: 'rider',
        token_version: 2,
        refresh_token_hash: null,
        save
      })
    });

    const req = {
      user: {
        id: 'user-123',
        role: 'rider'
      }
    };
    const res = {
      json: jest.fn()
    };

    await refresh(req, res);

    expect(jwt.sign).toHaveBeenNthCalledWith(
      1,
      { id: 'user-123', role: 'rider', tv: 2 },
      'test-access-secret',
      { expiresIn: '1h' }
    );
    expect(jwt.sign).toHaveBeenNthCalledWith(
      2,
      { id: 'user-123', tv: 2 },
      'test-refresh-secret',
      { expiresIn: '7d' }
    );
    expect(save).toHaveBeenCalledTimes(1);
    expect(res.json).toHaveBeenCalledWith({
      token: 'new-access-token',
      refreshToken: 'new-refresh-token'
    });
  });
});
