'use client';

import { useEffect, useState } from 'react';
import { io } from 'socket.io-client';
import { Button } from '@/components/ui/button';

const socket = io(process.env.NEXT_PUBLIC_API_URL);

export default function Emergency() {
  const [alerts, setAlerts] = useState([]);

  useEffect(() => {
    socket.emit('join_admin', localStorage.getItem('token'));
    socket.on('emergency', (alert) => setAlerts(prev => [...prev, alert]));
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