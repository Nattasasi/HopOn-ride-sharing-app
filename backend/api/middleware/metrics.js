const counters = {
  authRefreshFailures: 0,
  bookingStatusTransitions: 0,
  socketJoinFailures: 0,
  socketSendFailures: 0
};

const startedAt = new Date().toISOString();

function incrementCounter(name, value = 1) {
  if (!Object.prototype.hasOwnProperty.call(counters, name)) return;
  counters[name] += value;
}

function getMetricsSnapshot() {
  return {
    startedAt,
    generatedAt: new Date().toISOString(),
    counters: { ...counters }
  };
}

function resetMetrics() {
  Object.keys(counters).forEach((name) => {
    counters[name] = 0;
  });
}

module.exports = {
  incrementCounter,
  getMetricsSnapshot,
  resetMetrics
};