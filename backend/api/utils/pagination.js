const mongoose = require('mongoose');

const DEFAULT_LIMIT = 20;
const MAX_LIMIT = 100;

const shouldUseCursorPagination = (query = {}) => {
  return query.limit !== undefined || query.cursor !== undefined;
};

const resolveLimit = (rawLimit) => {
  const parsed = Number(rawLimit);
  if (!Number.isFinite(parsed) || parsed <= 0) return DEFAULT_LIMIT;
  return Math.min(MAX_LIMIT, Math.floor(parsed));
};

const normalizeCursorId = (cursor) => {
  if (!cursor || typeof cursor !== 'string') return null;
  const trimmed = cursor.trim();
  if (!mongoose.Types.ObjectId.isValid(trimmed)) return null;
  return new mongoose.Types.ObjectId(trimmed);
};

const paginateFindQuery = async ({
  model,
  query = {},
  cursor,
  limit,
  sort = { _id: -1 },
  projection,
  populate,
  lean = true
}) => {
  const normalizedLimit = resolveLimit(limit);
  const normalizedCursor = normalizeCursorId(cursor);
  const nextQuery = { ...query };

  if (normalizedCursor) {
    nextQuery._id = nextQuery._id
      ? { ...nextQuery._id, $lt: normalizedCursor }
      : { $lt: normalizedCursor };
  }

  let dbQuery = model.find(nextQuery, projection)
    .sort(sort)
    .limit(normalizedLimit + 1);

  if (populate) {
    if (Array.isArray(populate)) {
      populate.forEach((item) => {
        dbQuery = dbQuery.populate(item);
      });
    } else {
      dbQuery = dbQuery.populate(populate);
    }
  }

  if (lean) {
    dbQuery = dbQuery.lean();
  }

  const rows = await dbQuery;
  const hasMore = rows.length > normalizedLimit;
  const items = hasMore ? rows.slice(0, normalizedLimit) : rows;
  const nextCursor = hasMore && items.length > 0
    ? String(items[items.length - 1]._id)
    : null;

  return {
    items,
    page: {
      limit: normalizedLimit,
      has_more: hasMore,
      next_cursor: nextCursor
    }
  };
};

module.exports = {
  shouldUseCursorPagination,
  paginateFindQuery,
  resolveLimit,
  normalizeCursorId,
  DEFAULT_LIMIT,
  MAX_LIMIT
};
