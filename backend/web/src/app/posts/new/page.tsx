'use client';

import { useState } from 'react';
import GoogleMapReact from 'google-map-react';
import axios from '@/lib/axios';
import { Input, Button } from '@/components/ui';

export default function NewPost() {
  const [start, setStart] = useState(null);
  const [end, setEnd] = useState(null);
  // Other form states

  const handleMapClick = ({ lat, lng }, isStart) => {
    if (isStart) setStart({ lat, lng });
    else setEnd({ lat, lng });
  };

  const submit = async () => {
    if (!start || !end) {
      alert('Please select both start and end locations on the map.');
      return;
    }
    await axios.post('/api/v1/posts', {
      start_lat: start.lat,
      start_lng: start.lng,
      end_lat: end.lat,
      end_lng: end.lng,
      // Add other form fields here when available
      // e.g., title: postTitle, description: postDescription, etc.
    });
  };

  return (
    <div>
      <GoogleMapReact onClick={(e) => handleMapClick(e, true)}>
        {/* Markers */}
      </GoogleMapReact>
      {/* Form inputs */}
      <Button onClick={submit}>Create</Button>
    </div>
  );
}