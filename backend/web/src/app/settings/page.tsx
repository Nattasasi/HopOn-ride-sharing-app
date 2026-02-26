'use client';

import { useState, useEffect } from 'react';
import axios from 'axios';
import { Input, Button } from '@/components/ui';

export default function Settings() {
  const [form, setForm] = useState({
    first_name: '',
    last_name: '',
    email: '',
    password: ''
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchUserData = async () => {
      const userId = localStorage.getItem('userId');
      if (!userId) {
        setError('User not logged in or ID not found.');
        setLoading(false);
        return;
      }

      try {
        setLoading(true);
        const response = await axios.get(`/api/v1/users/${userId}`);
        setForm({
          first_name: response.data.first_name || '',
          last_name: response.data.last_name || '',
          email: response.data.email || '',
          password: '' // Never pre-fill password for security
        });
        setError(null);
      } catch (err) {
        console.error('Failed to fetch user data:', err);
        setError('Failed to load user data. Please try again.');
      } finally {
        setLoading(false);
      }
    };

    fetchUserData();
  }, []);

  const update = async () => {
    const userId = localStorage.getItem('userId');
    if (!userId) {
      alert('User not logged in or ID not found.');
      return;
    }

    try {
      const payload = { ...form };
      if (payload.password === '') {
        delete payload.password; // Don't send empty password
      }
      await axios.put(`/api/v1/users/${userId}`, payload);
      alert('Settings updated successfully!');
    } catch (err) {
      console.error('Failed to update user data:', err);
      alert('Failed to update settings. Please try again.');
    }
  };

  if (loading) {
    return <div className="p-4">Loading user settings...</div>;
  }

  if (error) {
    return <div className="p-4 text-red-500">{error}</div>;
  }

  return (
    <div className="p-4 space-y-4">
      <h1 className="text-2xl font-bold">User Settings</h1>
      <Input
        placeholder="First Name"
        value={form.first_name}
        onChange={e => setForm({...form, first_name: e.target.value})}
      />
      <Input
        placeholder="Last Name"
        value={form.last_name}
        onChange={e => setForm({...form, last_name: e.target.value})}
      />
      <Input
        type="email"
        placeholder="Email"
        value={form.email}
        onChange={e => setForm({...form, email: e.target.value})}
      />
      <Input
        type="password"
        placeholder="Password (leave blank to keep current)"
        value={form.password}
        onChange={e => setForm({...form, password: e.target.value})}
      />
      <Button onClick={update}>Save Changes</Button>
    </div>
  );
}