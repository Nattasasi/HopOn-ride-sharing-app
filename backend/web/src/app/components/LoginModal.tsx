// web/src/app/components/LoginModal.tsx
'use client';

import React, { useState } from 'react';
import { useAuth } from './AuthContext';
import { Input, Button } from '@/components/ui';
import axios from '@/lib/axios'; // Import custom axios

interface LoginModalProps {
  onClose: () => void;
}

export default function LoginModal({ onClose }: LoginModalProps) {
  const { login } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false); // New state for loading indicator

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setIsSubmitting(true); // Set submitting state

    try {
      const response = await axios.post('/api/v1/auth/login', { email, password });
      // Assuming the API returns { userId, token } on successful login
      const { userId, token } = response.data;
      if (userId && token) {
        login(userId, token);
        onClose(); // Close modal on successful login
      } else {
        // Handle cases where API succeeds but doesn't return expected data
        setError('Login successful but missing user data.');
      }
    } catch (err: any) {
      // Check if it's an Axios error with a response
      if (axios.isAxiosError(err) && err.response && err.response.data && err.response.data.message) {
        setError(err.response.data.message);
      } else {
        setError('An unexpected error occurred during login.');
      }
    } finally {
      setIsSubmitting(false); // Reset submitting state
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex justify-center items-center z-50">
      <div className="bg-white p-6 rounded-lg shadow-lg max-w-sm w-full">
        <h2 className="text-2xl font-bold mb-4">Login</h2>
        <form onSubmit={handleLogin} className="space-y-4">
          {error && <p className="text-red-500 text-sm">{error}</p>}
          <Input
            type="email"
            placeholder="Email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            className="border border-gray-300 px-3 py-2 rounded-md text-gray-900 focus:outline-none focus:ring-2 focus:ring-blue-500 w-full"
          />
          <Input
            type="password"
            placeholder="Password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            className="border border-gray-300 px-3 py-2 rounded-md text-gray-900 focus:outline-none focus:ring-2 focus:ring-blue-500 w-full"
          />
          <Button type="submit" className="w-full bg-blue-500 text-white hover:bg-blue-600" disabled={isSubmitting}>
            {isSubmitting ? 'Logging in...' : 'Login'}
          </Button>
          <Button type="button" onClick={onClose} variant="ghost" className=" bg-blue-300 w-full" disabled={isSubmitting}>Cancel</Button>
        </form>
      </div>
    </div>
  );
}
