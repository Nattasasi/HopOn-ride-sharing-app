'use client';

import { useEffect, useState } from 'react';
import { io } from 'socket.io-client';
import { useQuery } from '@tanstack/react-query';
import axios from '@/lib/axios';
import { Card } from '@/components/ui/card';

const socket = io('http://localhost:5000');

export default function DashboardPage() {
  const [alerts, setAlerts] = useState<any[]>([]);

  const { data: kpis, isLoading } = useQuery({
    queryKey: ['admin-dashboard'],
    queryFn: () => axios.get('/api/v1/admin/dashboard').then(res => res.data)
  });

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (token) {
      socket.emit('join_admin', token);
    }

    socket.on('emergency', (alert) => {
      setAlerts(prev => [alert, ...prev]);
    });

    return () => {
      socket.off('emergency');
    };
  }, []);

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
              alerts.map((a, idx) => (
                <div key={idx} className="p-3 bg-red-50 border border-red-100 rounded-md text-sm text-red-700">
                  <span className="font-bold">Alert:</span> {a.message || 'Emergency triggered'} at {a.lat}, {a.lng}
                </div>
              ))
            )}
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