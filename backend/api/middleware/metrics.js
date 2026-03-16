const counters = {
  authRefreshFailures: 0,
  bookingStatusTransitions: 0,
  bookingTransitionFailures: 0,
  paymentCreateFailures: 0,
  paymentFetchFailures: 0,
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

function getMetricsDashboardSnapshot() {
  const snapshot = getMetricsSnapshot();
  const thresholds = {
    authRefreshFailures: Number(process.env.ALERT_AUTH_REFRESH_FAILURES || 5),
    bookingTransitionFailures: Number(process.env.ALERT_BOOKING_TRANSITION_FAILURES || 3),
    paymentCreateFailures: Number(process.env.ALERT_PAYMENT_CREATE_FAILURES || 3),
    paymentFetchFailures: Number(process.env.ALERT_PAYMENT_FETCH_FAILURES || 5),
    socketJoinFailures: Number(process.env.ALERT_SOCKET_JOIN_FAILURES || 10),
    socketSendFailures: Number(process.env.ALERT_SOCKET_SEND_FAILURES || 10)
  };

  const alertSpecs = [
    {
      metric: 'authRefreshFailures',
      message: 'Auth refresh failures exceeded threshold',
      severity: 'high'
    },
    {
      metric: 'bookingTransitionFailures',
      message: 'Booking transition failures exceeded threshold',
      severity: 'high'
    },
    {
      metric: 'paymentCreateFailures',
      message: 'Payment creation failures exceeded threshold',
      severity: 'medium'
    },
    {
      metric: 'paymentFetchFailures',
      message: 'Payment lookup failures exceeded threshold',
      severity: 'medium'
    },
    {
      metric: 'socketJoinFailures',
      message: 'Socket join failures exceeded threshold',
      severity: 'high'
    },
    {
      metric: 'socketSendFailures',
      message: 'Socket send failures exceeded threshold',
      severity: 'high'
    }
  ];

  const alerts = alertSpecs
    .filter((spec) => snapshot.counters[spec.metric] >= thresholds[spec.metric])
    .map((spec) => ({
      metric: spec.metric,
      value: snapshot.counters[spec.metric],
      threshold: thresholds[spec.metric],
      severity: spec.severity,
      message: spec.message
    }));

  return {
    ...snapshot,
    thresholds,
    alerts,
    status: alerts.length > 0 ? 'degraded' : 'healthy'
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
  getMetricsDashboardSnapshot,
  resetMetrics
};