// Listens to personal user-room socket events and surfaces tiered toast notifications.
// Critical (booking confirms/cancels, ride cancel) → toast.error / toast.success
// Secondary (booking requests, ride started) → toast / toast.info
'use client';

import { useEffect, useRef } from 'react';
import { io, Socket } from 'socket.io-client';
import { toast } from 'sonner';
import { useAuth } from '../components/AuthContext';
import { SOCKET_BASE_URL } from '@/lib/axios';

export function useUserSocket() {
  const { isLoggedIn } = useAuth();
  const socketRef = useRef<Socket | null>(null);

  useEffect(() => {
    if (!isLoggedIn) {
      socketRef.current?.disconnect();
      socketRef.current = null;
      return;
    }

    const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;
    if (!token) return;

    const socket = io(SOCKET_BASE_URL, { reconnection: true, forceNew: false });
    socketRef.current = socket;

    socket.on('connect', () => {
      socket.emit('join_user', token);
    });

    // Critical — booking confirmed or rejected
    socket.on('booking_status_changed', (data: { status?: string }) => {
      const status = data?.status;
      if (status === 'confirmed' || status === 'accepted') {
        toast.success('Booking confirmed', {
          description: 'Your seat has been confirmed. Check My Rides for details.',
        });
      } else if (status === 'rejected') {
        toast.error('Booking not accepted', {
          description: 'The driver did not accept your request. Browse for another ride.',
        });
      }
    });

    // Critical — ride cancelled by host
    socket.on('ride_cancelled', () => {
      toast.error('Ride cancelled', {
        description: 'The driver has cancelled this ride. Browse for alternatives.',
      });
    });

    // Critical — booking cancelled
    socket.on('booking_cancelled', () => {
      toast.warning('Booking cancelled', {
        description: 'Your booking has been cancelled.',
      });
    });

    // Secondary — ride started
    socket.on('ride_started', () => {
      toast.success('Ride started', {
        description: 'Your driver is on the way. Tap My Rides to track progress.',
      });
    });

    // Secondary — ride status changed (in progress / completed)
    socket.on('ride_status_changed', (data: { status?: string }) => {
      const status = data?.status;
      if (status === 'in_progress') {
        toast.success('Ride in progress', { description: 'Your ride has started.' });
      } else if (status === 'completed') {
        toast('Ride completed', { description: 'Your ride has ended. Leave a rating!' });
      }
    });

    // Secondary — new booking request (driver-side)
    socket.on('booking_requested', () => {
      toast('New ride request', {
        description: 'A passenger has requested to join your ride.',
      });
    });

    socket.on('connect_error', () => {
      // Silently log — avoid spamming toasts for intermittent network issues
      console.warn('[useUserSocket] connection error');
    });

    return () => {
      socket.off('booking_status_changed');
      socket.off('ride_cancelled');
      socket.off('booking_cancelled');
      socket.off('ride_started');
      socket.off('ride_status_changed');
      socket.off('booking_requested');
      socket.disconnect();
      socketRef.current = null;
    };
  }, [isLoggedIn]);
}
