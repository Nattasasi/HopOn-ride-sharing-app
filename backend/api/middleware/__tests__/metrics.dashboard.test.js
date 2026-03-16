const {
  incrementCounter,
  getMetricsDashboardSnapshot,
  resetMetrics
} = require('../metrics');

describe('metrics dashboard snapshot', () => {
  beforeEach(() => {
    resetMetrics();
    process.env.ALERT_AUTH_REFRESH_FAILURES = '2';
    process.env.ALERT_SOCKET_SEND_FAILURES = '3';
  });

  afterEach(() => {
    delete process.env.ALERT_AUTH_REFRESH_FAILURES;
    delete process.env.ALERT_SOCKET_SEND_FAILURES;
  });

  it('returns healthy status with no alerts when counters are below thresholds', () => {
    incrementCounter('authRefreshFailures', 1);

    const snapshot = getMetricsDashboardSnapshot();

    expect(snapshot.status).toBe('healthy');
    expect(snapshot.alerts).toEqual([]);
  });

  it('returns degraded status with alerts when threshold is reached', () => {
    incrementCounter('authRefreshFailures', 2);
    incrementCounter('socketSendFailures', 3);

    const snapshot = getMetricsDashboardSnapshot();

    expect(snapshot.status).toBe('degraded');
    expect(snapshot.alerts).toEqual(
      expect.arrayContaining([
        expect.objectContaining({ metric: 'authRefreshFailures', threshold: 2 }),
        expect.objectContaining({ metric: 'socketSendFailures', threshold: 3 })
      ])
    );
  });
});
