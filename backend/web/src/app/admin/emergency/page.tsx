'use client';

import { useEffect, useState } from 'react';
import { io } from 'socket.io-client';
import axios, { SOCKET_BASE_URL } from '@/lib/axios';
import { Button } from '@/components/ui/button';

const socket = io(SOCKET_BASE_URL);

type EmergencyAlert = {
  alert_id?: string;
  lat?: number;
  lng?: number;
  created_at?: string;
  post_id?: {
    post_id?: string;
    start_location_name?: string;
    end_location_name?: string;
    status?: string;
  };
  reporter_id?: {
    first_name?: string;
    last_name?: string;
    email?: string;
    phone_number?: string;
  };
};

export default function Emergency() {
  const [alerts, setAlerts] = useState<EmergencyAlert[]>([]);

  useEffect(() => {
    let mounted = true;
    const token = localStorage.getItem('token');
    if (token) {
      socket.emit('join_admin', token);
    }
    axios.get('/api/v1/emergency')
      .then((res) => {
        if (mounted) setAlerts(Array.isArray(res.data) ? res.data : []);
      })
      .catch(() => {
        if (mounted) setAlerts([]);
      });

    socket.on('emergency', (alert) => setAlerts(prev => [alert, ...prev]));

    return () => {
      mounted = false;
      socket.off('emergency');
    };
  }, []);

  const openInMaps = (lat?: number, lng?: number) => {
    if (typeof lat !== 'number' || typeof lng !== 'number') return;
    const url = `https://www.google.com/maps?q=${lat},${lng}`;
    window.open(url, '_blank', 'noopener,noreferrer');
  };

  return (
    <div className="p-6 space-y-4">
      <h1 className="text-2xl font-bold">Emergency Alerts</h1>
      {alerts.length === 0 ? (
        <p className="text-sm text-gray-500">No active emergency alerts.</p>
      ) : null}
      {alerts.map((a) => {
        const reporterName = [a.reporter_id?.first_name, a.reporter_id?.last_name]
          .filter(Boolean)
          .join(' ')
          .trim();
        const routeLabel = [a.post_id?.start_location_name, a.post_id?.end_location_name]
          .filter(Boolean)
          .join(' to ');

        return (
          <div key={a.alert_id} className="rounded-md border border-red-200 bg-red-50 p-4 space-y-2">
            <div className="text-sm font-semibold text-red-700">
              Emergency Alert {a.alert_id ? `#${a.alert_id}` : ''}
            </div>
            <div className="text-sm text-gray-800">
              Reporter: {reporterName || a.reporter_id?.email || 'Unknown'}
            </div>
            <div className="text-sm text-gray-800">
              Ride: {routeLabel || a.post_id?.post_id || 'Unknown ride'}
            </div>
            <div className="text-sm text-gray-800">
              Location: {a.lat ?? '-'}, {a.lng ?? '-'}
            </div>
            {a.created_at ? (
              <div className="text-xs text-gray-500">
                Created: {new Date(a.created_at).toLocaleString()}
              </div>
            ) : null}
            <Button
              onClick={() => openInMaps(a.lat, a.lng)}
              disabled={typeof a.lat !== 'number' || typeof a.lng !== 'number'}
            >
              Open in Google Maps
            </Button>
          </div>
        );
      })}
    </div>
  );
}
