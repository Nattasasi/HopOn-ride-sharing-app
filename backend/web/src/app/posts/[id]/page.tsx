'use client';

import { useEffect, useRef, useState } from 'react';
import { useParams } from 'next/navigation';
import { io } from 'socket.io-client';
import GoogleMapReact from 'google-map-react';
import { Button, Input } from '@/components/ui';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import axios, { SOCKET_BASE_URL } from '@/lib/axios';

const socket = io(SOCKET_BASE_URL, {
  reconnection: true,
  transports: ['websocket', 'polling']
});

type DeliveryStatus = 'sending' | 'sent' | 'failed';

type ChatMessage = {
  localId: string;
  senderId: string;
  senderDisplayName: string;
  body: string;
  sentAtLabel: string;
  deliveryStatus: DeliveryStatus;
};

type BookingPassenger = {
  _id: string;
  first_name?: string;
  last_name?: string;
  average_rating?: number | null;
  is_verified?: boolean;
  verification_status?: string;
};

type Booking = {
  _id: string;
  passenger_id?: BookingPassenger | null;
  status: string;
  seats_booked: number;
  booked_at?: string;
  created_at?: string;
};

const buildLocalTimeLabel = () => new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

const normalizeIncomingMessage = (raw: any): ChatMessage | null => {
  if (!raw) return null;
  const senderId = raw?.sender_id?._id || raw?.sender_id?.id || raw?.sender_id || '';
  const firstName = raw?.sender_id?.first_name || '';
  const lastName = raw?.sender_id?.last_name || '';
  const senderDisplayName = [firstName, lastName].filter(Boolean).join(' ').trim() || 'Unknown';
  const body = raw?.body || '';
  const sentAtLabel = raw?.sent_at ? new Date(raw.sent_at).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : buildLocalTimeLabel();
  if (!body) return null;

  return {
    localId: raw?._id || `remote-${senderId}-${raw?.sent_at || body}`,
    senderId,
    senderDisplayName,
    body,
    sentAtLabel,
    deliveryStatus: 'sent'
  };
};

export default function PostDetail() {
  const { id } = useParams();
  const [liveMessages, setLiveMessages] = useState<ChatMessage[]>([]);
  const [draftMessage, setDraftMessage] = useState('');
  const [isSocketConnected, setIsSocketConnected] = useState(false);
  const [isChatStale, setIsChatStale] = useState(false);
  const [tracking, setTracking] = useState(null);
  const hasConnectedOnce = useRef(false);
  const currentUserId = typeof window !== 'undefined' ? localStorage.getItem('id') || '' : '';
  const currentUserName = typeof window !== 'undefined' ? localStorage.getItem('displayName') || 'Me' : 'Me';

  const { data: post } = useQuery({
    queryKey: ['post', id],
    queryFn: () => axios.get(`/api/v1/posts/${id}`).then(res => res.data)
  });

  const { data: initialMessages = [], refetch: refetchMessages } = useQuery({
    queryKey: ['messages', id],
    queryFn: () => axios.get(`/api/v1/messages/${id}`).then(res => res.data)
  });

  const queryClient = useQueryClient();
  const rawPost = post as any;
  const postUuid: string | undefined = rawPost?.post_id;
  const driverMongoId: string | undefined =
    typeof rawPost?.driver_id === 'object' ? rawPost?.driver_id?._id : rawPost?.driver_id;
  const isHost = !!currentUserId && !!driverMongoId && currentUserId === driverMongoId;

  const { data: bookings = [] } = useQuery<Booking[]>({
    queryKey: ['bookings', postUuid],
    queryFn: () => axios.get(`/api/v1/bookings/posts/${postUuid}`).then(res => res.data),
    enabled: isHost && !!postUuid
  });

  const respondMutation = useMutation({
    mutationFn: ({ bookingId, status }: { bookingId: string; status: 'confirmed' | 'rejected' }) =>
      axios.patch(`/api/v1/bookings/${bookingId}/respond`, { status }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['bookings', postUuid] })
  });

  const pendingBookings = (bookings as Booking[])
    .filter(b => b.status === 'pending')
    .sort((a, b) => {
      const ratingDiff = (b.passenger_id?.average_rating ?? 0) - (a.passenger_id?.average_rating ?? 0);
      if (ratingDiff !== 0) return ratingDiff;
      const seatsDiff = (a.seats_booked ?? 1) - (b.seats_booked ?? 1);
      if (seatsDiff !== 0) return seatsDiff;
      return (a.booked_at ?? '').localeCompare(b.booked_at ?? '');
    });

  useEffect(() => {
    socket.emit('join_post', { token: localStorage.getItem('token'), post_id: id });
    setIsSocketConnected(socket.connected);

    const onConnect = () => {
      setIsSocketConnected(true);
      if (hasConnectedOnce.current) {
        setIsChatStale(false);
        refetchMessages();
      } else {
        hasConnectedOnce.current = true;
      }
      socket.emit('join_post', { token: localStorage.getItem('token'), post_id: id });
    };

    const onDisconnect = () => {
      setIsSocketConnected(false);
      setIsChatStale(true);
    };

    const onConnectError = () => {
      setIsSocketConnected(false);
      setIsChatStale(true);
    };

    const onNewMessage = (msg: any) => {
      const incoming = normalizeIncomingMessage(msg);
      if (!incoming) return;

      setLiveMessages(prev => {
        if (incoming.senderId && incoming.senderId === currentUserId) {
          const pendingIndex = prev.findIndex(
            p =>
              p.senderId === currentUserId &&
              p.body === incoming.body &&
              (p.deliveryStatus === 'sending' || p.deliveryStatus === 'failed')
          );
          if (pendingIndex >= 0) {
            const next = [...prev];
            next[pendingIndex] = {
              ...incoming,
              localId: prev[pendingIndex].localId,
              deliveryStatus: 'sent'
            };
            return next;
          }
        }
        return [...prev, incoming];
      });
    };

    socket.on('connect', onConnect);
    socket.on('disconnect', onDisconnect);
    socket.on('connect_error', onConnectError);
    socket.on('new_message', onNewMessage);
    socket.on('location_update', track => setTracking(track));

    return () => {
      socket.off('connect', onConnect);
      socket.off('disconnect', onDisconnect);
      socket.off('connect_error', onConnectError);
      socket.off('new_message', onNewMessage);
      socket.off('location_update');
    };
  }, [id, refetchMessages, currentUserId]);

  const sendMessage = (body: string) => {
    const text = body.trim();
    if (!text) return;

    const localId = `local-${Date.now()}-${Math.random().toString(16).slice(2)}`;
    const pending: ChatMessage = {
      localId,
      senderId: currentUserId,
      senderDisplayName: currentUserName,
      body: text,
      sentAtLabel: buildLocalTimeLabel(),
      deliveryStatus: 'sending'
    };
    setLiveMessages(prev => [...prev, pending]);

    if (!socket.connected) {
      setIsChatStale(true);
      setLiveMessages(prev => prev.map(m => (m.localId === localId ? { ...m, deliveryStatus: 'failed' } : m)));
      return;
    }

    socket.emit('send_message', { post_id: id, body: text });
    setDraftMessage('');
  };

  const retryMessage = (localId: string) => {
    const failed = liveMessages.find(m => m.localId === localId && m.deliveryStatus === 'failed');
    if (!failed) return;
    setLiveMessages(prev => prev.map(m => (m.localId === localId ? { ...m, deliveryStatus: 'sending' } : m)));
    if (!socket.connected) {
      setIsChatStale(true);
      setLiveMessages(prev => prev.map(m => (m.localId === localId ? { ...m, deliveryStatus: 'failed' } : m)));
      return;
    }
    socket.emit('send_message', { post_id: id, body: failed.body });
  };

  const emergency = () => {
    window.location.href = 'tel://911';
    axios.post('/api/v1/emergency', { post_id: id, lat: 0, lng: 0 });
  };

  // Rating modal if pending

  return (
    <div>
      <GoogleMapReact>
        {/* Route overlay with tracking */}
      </GoogleMapReact>
      {!isHost && <Button onClick={() => {/* join */}}>Join</Button>}

      {isHost && (
        <div style={{ padding: '16px 0' }}>
          <p style={{ fontWeight: 600, fontSize: 16, marginBottom: 12 }}>
            Booking Requests{pendingBookings.length > 0 ? ` (${pendingBookings.length} pending)` : ''}
          </p>
          {pendingBookings.length === 0 ? (
            <p style={{ color: '#6b7280', fontSize: 14 }}>No pending booking requests.</p>
          ) : (
            pendingBookings.map(booking => {
              const passenger = booking.passenger_id;
              const fullName = [passenger?.first_name, passenger?.last_name].filter(Boolean).join(' ') || 'Unknown passenger';
              const rating = passenger?.average_rating;
              const isVerified = passenger?.verification_status?.toLowerCase() === 'verified';
              return (
                <div
                  key={booking._id}
                  style={{
                    border: '1px solid #e5e7eb',
                    borderRadius: 12,
                    padding: '12px 16px',
                    marginBottom: 10,
                    background: '#fff'
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 6 }}>
                    <span style={{ fontWeight: 600, fontSize: 15 }}>{fullName}</span>
                    <span style={{ fontSize: 12, background: '#fef9c3', color: '#713f12', borderRadius: 999, padding: '2px 10px' }}>Pending</span>
                  </div>
                  <div style={{ display: 'flex', gap: 16, fontSize: 13, color: '#4b5563', marginBottom: 8 }}>
                    <span>
                      {rating != null && rating > 0 ? `${rating.toFixed(1)} ★` : 'No rating'}
                    </span>
                    <span>{booking.seats_booked ?? 1} seat{(booking.seats_booked ?? 1) !== 1 ? 's' : ''}</span>
                    {isVerified && (
                      <span style={{ color: '#16a34a', fontWeight: 500 }}>✓ Verified</span>
                    )}
                  </div>
                  <div style={{ display: 'flex', gap: 8 }}>
                    <Button
                      onClick={() => respondMutation.mutate({ bookingId: booking._id, status: 'confirmed' })}
                      disabled={respondMutation.isPending}
                    >
                      Accept
                    </Button>
                    <Button
                      onClick={() => respondMutation.mutate({ bookingId: booking._id, status: 'rejected' })}
                      disabled={respondMutation.isPending}
                    >
                      Reject
                    </Button>
                  </div>
                </div>
              );
            })
          )}
        </div>
      )}
      <div>
        <p>Chat:</p>
        {!isSocketConnected && (
          <p style={{ color: '#a16207', fontSize: 12, marginBottom: 8 }}>
            Reconnecting chat...
          </p>
        )}
        {isChatStale && !isSocketConnected && (
          <p style={{ color: '#6b7280', fontSize: 12, marginBottom: 8 }}>
            Messages may be out of date until connection is restored.
          </p>
        )}
        {[...(initialMessages || []).map((m: any) => normalizeIncomingMessage(m)).filter(Boolean) as ChatMessage[], ...liveMessages].map(m => (
          <div key={m.localId} style={{ marginBottom: 8 }}>
            <div>{m.body}</div>
            <div style={{ fontSize: 11, color: '#6b7280' }}>
              {m.sentAtLabel}
              {m.senderId === currentUserId && m.deliveryStatus === 'sending' && ' • sending'}
              {m.senderId === currentUserId && m.deliveryStatus === 'failed' && (
                <>
                  {' • failed '}
                  <button type="button" onClick={() => retryMessage(m.localId)} style={{ fontSize: 11, textDecoration: 'underline' }}>
                    retry
                  </button>
                </>
              )}
            </div>
          </div>
        ))}
        <Input
          value={draftMessage}
          onChange={e => setDraftMessage((e.target as HTMLInputElement).value)}
          onKeyDown={e => e.key === 'Enter' && sendMessage(draftMessage)}
        />
      </div>
      <Button onClick={emergency}>Emergency</Button>
    </div>
  );
}
