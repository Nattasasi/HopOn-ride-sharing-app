// web/src/app/components/LoginModal.tsx
'use client';

import React, { useState } from 'react';
import { useAuth } from './AuthContext';
import { Input, Button } from '@/components/ui';
import { isAxiosError } from 'axios';
import axios from '@/lib/axios'; // Import custom axios

interface LoginModalProps {
  onClose: () => void;
}

export default function LoginModal({ onClose }: LoginModalProps) {
  const { login } = useAuth();
  const [mode, setMode] = useState<'login' | 'forgot' | 'reset'>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [resetToken, setResetToken] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [info, setInfo] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false); // New state for loading indicator

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setInfo(null);
    setError(null);
    setIsSubmitting(true); // Set submitting state

    try {
      const response = await axios.post('/api/v1/auth/login', { email, password });
      // API returns { userId, token, refreshToken } on successful login.
      const { userId, token, refreshToken } = response.data;
      if (userId && token) {
        login(userId, token, refreshToken);
        onClose(); // Close modal on successful login
      } else {
        // Handle cases where API succeeds but doesn't return expected data
        setError('Login successful but missing user data.');
      }
    } catch (err: any) {
      if (isAxiosError(err)) {
        const apiMessage = err.response?.data?.message;
        if (apiMessage) {
          setError(apiMessage);
        } else if (err.code === 'ERR_NETWORK') {
          setError('Unable to reach server. Please ensure backend is running on port 3001.');
        } else {
          setError(err.message || 'Login failed. Please try again.');
        }
      } else {
        setError('An unexpected error occurred during login.');
      }
    } finally {
      setIsSubmitting(false); // Reset submitting state
    }
  };

  const handleForgotPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setInfo(null);
    setError(null);
    setIsSubmitting(true);

    try {
      const response = await axios.post('/api/v1/auth/forgot-password', { email });
      const devResetToken = response?.data?.devResetToken as string | undefined;
      setInfo(response?.data?.message || 'Recovery instructions generated.');

      if (devResetToken) {
        setResetToken(devResetToken);
        setInfo('Recovery token generated for local development. Use it below to reset your password.');
      }

      setMode('reset');
    } catch (err: any) {
      if (isAxiosError(err)) {
        setError(err.response?.data?.message || err.message || 'Could not start account recovery.');
      } else {
        setError('Could not start account recovery.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleResetPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setInfo(null);
    setError(null);

    if (newPassword !== confirmPassword) {
      setError('Passwords do not match.');
      return;
    }

    setIsSubmitting(true);
    try {
      await axios.post('/api/v1/auth/reset-password', {
        token: resetToken,
        newPassword,
      });
      setInfo('Password updated. You can now login with your new password.');
      setPassword('');
      setNewPassword('');
      setConfirmPassword('');
      setResetToken('');
      setMode('login');
    } catch (err: any) {
      if (isAxiosError(err)) {
        setError(err.response?.data?.message || err.message || 'Could not reset password.');
      } else {
        setError('Could not reset password.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const renderLoginForm = () => (
    <form onSubmit={handleLogin} className="space-y-4">
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
      <Button
        type="button"
        variant="ghost"
        className="w-full"
        onClick={() => {
          setError(null);
          setInfo(null);
          setMode('forgot');
        }}
        disabled={isSubmitting}
      >
        Forgot password?
      </Button>
      <Button type="button" onClick={onClose} variant="ghost" className=" bg-blue-300 w-full" disabled={isSubmitting}>Cancel</Button>
    </form>
  );

  const renderForgotForm = () => (
    <form onSubmit={handleForgotPassword} className="space-y-4">
      <Input
        type="email"
        placeholder="Email used for your account"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        required
        className="border border-gray-300 px-3 py-2 rounded-md text-gray-900 focus:outline-none focus:ring-2 focus:ring-blue-500 w-full"
      />
      <Button type="submit" className="w-full bg-blue-500 text-white hover:bg-blue-600" disabled={isSubmitting}>
        {isSubmitting ? 'Sending...' : 'Generate recovery token'}
      </Button>
      <Button
        type="button"
        variant="ghost"
        className="w-full"
        onClick={() => {
          setError(null);
          setInfo(null);
          setMode('login');
        }}
        disabled={isSubmitting}
      >
        Back to login
      </Button>
    </form>
  );

  const renderResetForm = () => (
    <form onSubmit={handleResetPassword} className="space-y-4">
      <Input
        type="text"
        placeholder="Reset token"
        value={resetToken}
        onChange={(e) => setResetToken(e.target.value)}
        required
        className="border border-gray-300 px-3 py-2 rounded-md text-gray-900 focus:outline-none focus:ring-2 focus:ring-blue-500 w-full"
      />
      <Input
        type="password"
        placeholder="New password"
        value={newPassword}
        onChange={(e) => setNewPassword(e.target.value)}
        required
        minLength={6}
        className="border border-gray-300 px-3 py-2 rounded-md text-gray-900 focus:outline-none focus:ring-2 focus:ring-blue-500 w-full"
      />
      <Input
        type="password"
        placeholder="Confirm new password"
        value={confirmPassword}
        onChange={(e) => setConfirmPassword(e.target.value)}
        required
        minLength={6}
        className="border border-gray-300 px-3 py-2 rounded-md text-gray-900 focus:outline-none focus:ring-2 focus:ring-blue-500 w-full"
      />
      <Button type="submit" className="w-full bg-blue-500 text-white hover:bg-blue-600" disabled={isSubmitting}>
        {isSubmitting ? 'Resetting...' : 'Reset password'}
      </Button>
      <Button
        type="button"
        variant="ghost"
        className="w-full"
        onClick={() => {
          setError(null);
          setInfo(null);
          setMode('login');
        }}
        disabled={isSubmitting}
      >
        Back to login
      </Button>
    </form>
  );

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex justify-center items-center z-50">
      <div className="bg-white p-6 rounded-lg shadow-lg max-w-sm w-full">
        <h2 className="text-2xl font-bold mb-4">Login</h2>
        <p className="text-sm text-gray-600 mb-4">
          {mode === 'login' && 'Sign in to continue.'}
          {mode === 'forgot' && 'Generate an account recovery token.'}
          {mode === 'reset' && 'Enter the recovery token and set a new password.'}
        </p>
        {error && <p className="text-red-500 text-sm mb-3">{error}</p>}
        {info && <p className="text-emerald-700 text-sm mb-3">{info}</p>}
        {mode === 'login' && renderLoginForm()}
        {mode === 'forgot' && renderForgotForm()}
        {mode === 'reset' && renderResetForm()}
      </div>
    </div>
  );
}
