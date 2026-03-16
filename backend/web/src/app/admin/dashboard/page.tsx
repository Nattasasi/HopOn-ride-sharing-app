'use client';

import { useEffect, useState } from 'react';
import { io } from 'socket.io-client';
import { useQuery } from '@tanstack/react-query';
import axios, { SOCKET_BASE_URL } from '@/lib/axios';
import { Card } from '@/components/ui/card';
import { DashboardPageSkeleton } from '@/app/components/PageSkeletons';
import { Button } from '@/components/ui/button';
import { formatRouteLabel } from '@/lib/locationLabel';

const socket = io(SOCKET_BASE_URL);

type EmergencyAlert = {
  alert_id?: string;
  created_at?: string;
  lat?: number;
  lng?: number;
  post_id?: {
    post_id?: string;
    start_location_name?: string;
    end_location_name?: string;
  };
  reporter_id?: {
    first_name?: string;
    last_name?: string;
  };
};

export default function DashboardPage() {
  const [alerts, setAlerts] = useState<EmergencyAlert[]>([]);
  const [alertPage, setAlertPage] = useState(1);
  const alertPageSize = 5;

  const { data: kpis, isLoading } = useQuery({
    queryKey: ['admin-dashboard'],
    queryFn: () => axios.get('/api/v1/admin/dashboard').then(res => res.data)
  });

  useEffect(() => {
    let mounted = true;
    axios.get('/api/v1/emergency')
      .then((res) => {
        if (mounted) setAlerts(Array.isArray(res.data) ? res.data : []);
      })
      .catch(() => {
        if (mounted) setAlerts([]);
      });

    const token = localStorage.getItem('token');
    if (token) {
      socket.emit('join_admin', token);
    }/*  */

    socket.on('emergency', (alert) => {
      setAlerts(prev => [alert, ...prev]);
      setAlertPage(1);
    });

    return () => {
      mounted = false;
      socket.off('emergency');
    };
  }, []);

  const alertTotalPages = Math.max(1, Math.ceil(alerts.length / alertPageSize));
  const currentAlertPage = Math.min(alertPage, alertTotalPages);
  const alertPageStart = (currentAlertPage - 1) * alertPageSize;
  const paginatedAlerts = alerts.slice(alertPageStart, alertPageStart + alertPageSize);

  if (isLoading) {
    return <DashboardPageSkeleton />;
  }

  return (
    <div className="p-6 space-y-6">
      <h1 className="text-2xl font-bold">Admin Dashboard</h1>
      
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card className="p-4 bg-white shadow-sm border">
          <div className="text-sm font-medium text-gray-500">Active Rides</div>
          <div className="text-2xl font-bold mt-1">{isLoading ? '...' : kpis?.activeRides}</div>
        </Card>
        
        <Card className="p-4 bg-white shadow-sm border">
          <div className="text-sm font-medium text-gray-500">Rides In Progress</div>
          <div className="text-2xl font-bold mt-1">{isLoading ? '...' : kpis?.inProgressRides}</div>
        </Card>

        <Card className="p-4 bg-white shadow-sm border">
          <div className="text-sm font-medium text-gray-500">Total Users</div>
          <div className="text-2xl font-bold mt-1">{isLoading ? '...' : kpis?.totalUsers}</div>
        </Card>

        <Card className="p-4 bg-white shadow-sm border">
          <div className="text-sm font-medium text-gray-500">Open Alerts</div>
          <div className="text-2xl font-bold mt-1 text-red-600">{isLoading ? '...' : kpis?.openAlerts}</div>
        </Card>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card className="p-4 border shadow-sm">
          <h2 className="font-semibold mb-4">Live Emergency Alerts</h2>
          <div className="space-y-3">
            {alerts.length === 0 ? (
              <p className="text-sm text-gray-500 text-center py-4">No active emergency alerts.</p>
            ) : (
              paginatedAlerts.map((a) => (
                <div key={a.alert_id || `${a.lat}-${a.lng}-${a.created_at}`} className="p-3 bg-red-50 border border-red-100 rounded-md text-sm text-red-700">
                  <div className="font-semibold">Emergency Alert</div>
                  <div>
                    {formatRouteLabel(
                      a.post_id?.start_location_name,
                      a.post_id?.end_location_name,
                      a.lat,
                      a.lng,
                      a.post_id?.post_id || 'Unknown ride'
                    )}
                  </div>
                  <div>
                    {a.lat}, {a.lng}
                  </div>
                  <div className="text-xs text-red-600">
                    {a.created_at ? new Date(a.created_at).toLocaleString() : 'Just now'}
                  </div>
                </div>
              ))
            )}
            {alerts.length > alertPageSize ? (
              <div className="flex items-center justify-between pt-1">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setAlertPage((p) => Math.max(1, p - 1))}
                  disabled={currentAlertPage <= 1}
                >
                  Prev
                </Button>
                <span className="text-xs text-gray-600">
                  Page {currentAlertPage} / {alertTotalPages}
                </span>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setAlertPage((p) => Math.min(alertTotalPages, p + 1))}
                  disabled={currentAlertPage >= alertTotalPages}
                >
                  Next
                </Button>
              </div>
            ) : null}
          </div>
        </Card>

        <Card className="p-4 border shadow-sm">
          <h2 className="font-semibold mb-4">Quick Links</h2>
          <div className="grid grid-cols-2 gap-3">
            <a href="/admin/users" className="p-3 border rounded-md hover:bg-gray-50 text-center text-sm font-medium">Manage Users</a>
            <a href="/admin/posts" className="p-3 border rounded-md hover:bg-gray-50 text-center text-sm font-medium">Manage Rides</a>
            <a href="/admin/bookings" className="p-3 border rounded-md hover:bg-gray-50 text-center text-sm font-medium">Manage Bookings</a>
            <a href="/admin/emergency" className="p-3 border rounded-md hover:bg-gray-50 text-center text-sm font-medium">View Alerts</a>
          </div>
        </Card>
      </div>
    </div>
  );
}
