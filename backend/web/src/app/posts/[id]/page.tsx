'use client';

import { useEffect, useState } from 'react';
import { useParams } from 'next/navigation';
import { io } from 'socket.io-client';
import GoogleMapReact from 'google-map-react';
import { Button, Input } from '@/components/ui';
import { useQuery } from '@tanstack/react-query';
import axios from 'axios';

const socket = io(process.env.NEXT_PUBLIC_API_URL);

export default function PostDetail() {
  const { id } = useParams();
  const [messages, setMessages] = useState([]);
  const [tracking, setTracking] = useState(null);

  const { data: post } = useQuery({
    queryKey: ['post', id],
    queryFn: () => axios.get(`/api/v1/posts/${id}`).then(res => res.data)
  });

  const { data: initialMessages } = useQuery({
    queryKey: ['messages', id],
    queryFn: () => axios.get(`/api/v1/messages/${id}`).then(res => res.data)
  });

  useEffect(() => {
    setMessages(initialMessages || []);
    socket.emit('join_post', { token: localStorage.getItem('token'), post_id: id });

    socket.on('new_message', msg => setMessages(prev => [...prev, msg]));
    socket.on('location_update', track => setTracking(track));

    return () => {
      socket.off('new_message');
      socket.off('location_update');
    };
  }, [initialMessages, id]);

  const sendMessage = (body: string) => {
    socket.emit('send_message', { post_id: id, body });
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
      <Button onClick={() => {/* join */}}>Join</Button>
      <div>Chat:
        {messages.map(m => <div key={m.message_id}>{m.body}</div>)}
        <Input onKeyDown={e => e.key === 'Enter' && sendMessage(e.target.value)} />
      </div>
      <Button onClick={emergency}>Emergency</Button>
    </div>
  );
}