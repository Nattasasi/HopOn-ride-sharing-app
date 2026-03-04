'use client';

import { useEffect, useState } from 'react';
import { io } from 'socket.io-client';
import { Button } from '@/components/ui/button';
import { SOCKET_BASE_URL } from '@/lib/axios';

const socket = io(SOCKET_BASE_URL);

export default function Emergency() {
  const [alerts, setAlerts] = useState([]);

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (token) {
      socket.emit('join_admin', token);
    }
    socket.on('emergency', (alert) => setAlerts(prev => [...prev, alert]));

    return () => {
      socket.off('emergency');
    };
  }, []);

  const resolve = async (id: string) => {
    // Patch to resolve
    setAlerts(prev => prev.filter(a => a.alert_id !== id));
  };

  return (
    <div>
      {alerts.map(a => (
        <div key={a.alert_id}>
          {a.lat}, {a.lng} <Button onClick={() => resolve(a.alert_id)}>Resolve</Button>
        </div>
      ))}
    </div>
  );
}
