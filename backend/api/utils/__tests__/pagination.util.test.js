const mongoose = require('mongoose');
const {
  shouldUseCursorPagination,
  resolveLimit,
  normalizeCursorId
} = require('../pagination');

describe('pagination utility', () => {
  it('detects cursor pagination mode from query', () => {
    expect(shouldUseCursorPagination({})).toBe(false);
    expect(shouldUseCursorPagination({ limit: '10' })).toBe(true);
    expect(shouldUseCursorPagination({ cursor: 'abc' })).toBe(true);
  });

  it('clamps and defaults limit values', () => {
    expect(resolveLimit(undefined)).toBe(20);
    expect(resolveLimit('0')).toBe(20);
    expect(resolveLimit('5')).toBe(5);
    expect(resolveLimit('999')).toBe(100);
  });

  it('normalizes valid object id cursor and rejects invalid values', () => {
    const id = new mongoose.Types.ObjectId().toString();
    expect(normalizeCursorId(id)).toBeInstanceOf(mongoose.Types.ObjectId);
    expect(normalizeCursorId('bad-id')).toBeNull();
    expect(normalizeCursorId(undefined)).toBeNull();
  });
});
