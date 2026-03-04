'use client';

import { useState, useEffect, type FormEvent } from 'react';
import axios from "@/lib/axios";
import { Input, Button } from '@/components/ui';
import { SettingsPageSkeleton } from '@/app/components/PageSkeletons';

export default function Settings() {
  const [form, setForm] = useState({
    first_name: '',
    last_name: '',
    email: '',
    new_password: '',
    current_password: '',
  });
  const [originalProfile, setOriginalProfile] = useState({
    first_name: '',
    last_name: '',
    email: '',
  });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  useEffect(() => {
    const fetchUserData = async () => {
      const userId = localStorage.getItem('userId');
      if (!userId) {
        setError('User not logged in or ID not found.');
        setLoading(false);
        return;
      }
      console.log('Fetching user data for ID:', userId);

      try {
        setLoading(true);
        const response = await axios.get(`/api/v1/users/${userId}`);
        const nextProfile = {
          first_name: response.data.first_name || '',
          last_name: response.data.last_name || '',
          email: response.data.email || '',
        };
        setOriginalProfile(nextProfile);
        setForm({
          ...nextProfile,
          new_password: '',
          current_password: '',
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

  const update = async (e: FormEvent) => {
    e.preventDefault();
    const userId = localStorage.getItem('userId');
    if (!userId) {
      setError('User not logged in or ID not found.');
      return;
    }

    const hasNewPassword = form.new_password.trim().length > 0;
    const hasCurrentPassword = form.current_password.trim().length > 0;

    if (hasNewPassword && form.new_password.trim().length < 6) {
      setError('New Password must be at least 6 characters.');
      setSuccess(null);
      return;
    }

    try {
      setSaving(true);
      setError(null);
      setSuccess(null);

      const payload: Record<string, string> = {};

      const trimmedFirstName = form.first_name.trim();
      const trimmedLastName = form.last_name.trim();
      const trimmedEmail = form.email.trim();

      if (trimmedFirstName !== originalProfile.first_name) {
        payload.first_name = trimmedFirstName;
      }
      if (trimmedLastName !== originalProfile.last_name) {
        payload.last_name = trimmedLastName;
      }
      if (trimmedEmail !== originalProfile.email) {
        payload.email = trimmedEmail;
      }

      if (hasNewPassword) {
        payload.password = form.new_password.trim();
      }

      const hasProfileChanges = Object.keys(payload).length > 0;
      if (!hasProfileChanges && !hasNewPassword) {
        setSuccess('No changes to save.');
        return;
      }

      if (!hasCurrentPassword) {
        setError('Current Password is required to save profile changes.');
        setSuccess(null);
        return;
      }

      payload.current_password = form.current_password;

      await axios.put(`/api/v1/users/${userId}`, payload);
      setSuccess('Settings updated successfully.');
      setOriginalProfile({
        first_name: trimmedFirstName,
        last_name: trimmedLastName,
        email: trimmedEmail,
      });
      setForm((prev) => ({
        ...prev,
        first_name: trimmedFirstName,
        last_name: trimmedLastName,
        email: trimmedEmail,
        new_password: '',
        current_password: '',
      }));
    } catch (err: any) {
      console.error('Failed to update user data:', err);
      const message =
        err?.response?.data?.message ||
        (Array.isArray(err?.response?.data?.errors) && err.response.data.errors[0]?.msg) ||
        'Failed to update settings. Please try again.';
      setError(message);
      setSuccess(null);
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return <SettingsPageSkeleton />;
  }

  return (
    <div className="mx-auto max-w-3xl p-4 md:p-6">
      <div className="rounded-xl border border-zinc-300 bg-white p-6 shadow-sm dark:border-zinc-700 dark:bg-zinc-900">
        <div className="mb-6">
          <h1 className="text-2xl font-bold text-zinc-900 dark:text-zinc-100">User Settings</h1>
          <p className="mt-1 text-sm text-zinc-600 dark:text-zinc-400">
            Update your profile information and password.
          </p>
        </div>

        <form onSubmit={update} className="space-y-5">
          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            <div className="space-y-2">
              <label className="text-sm font-medium text-zinc-700 dark:text-zinc-300">First Name</label>
              <Input
                placeholder="Enter first name"
                value={form.first_name}
                onChange={(e) => setForm({ ...form, first_name: e.target.value })}
                className="w-full rounded-md border border-zinc-300 px-3 py-2 text-zinc-900 focus:outline-none focus:ring-2 focus:ring-blue-500 dark:border-zinc-600 dark:bg-zinc-800 dark:text-zinc-100"
                required
              />
            </div>

            <div className="space-y-2">
              <label className="text-sm font-medium text-zinc-700 dark:text-zinc-300">Last Name</label>
              <Input
                placeholder="Enter last name"
                value={form.last_name}
                onChange={(e) => setForm({ ...form, last_name: e.target.value })}
                className="w-full rounded-md border border-zinc-300 px-3 py-2 text-zinc-900 focus:outline-none focus:ring-2 focus:ring-blue-500 dark:border-zinc-600 dark:bg-zinc-800 dark:text-zinc-100"
                required
              />
            </div>
          </div>

          <div className="space-y-2">
            <label className="text-sm font-medium text-zinc-700 dark:text-zinc-300">Email</label>
            <Input
              type="email"
              placeholder="Enter email"
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
              className="w-full rounded-md border border-zinc-300 px-3 py-2 text-zinc-900 focus:outline-none focus:ring-2 focus:ring-blue-500 dark:border-zinc-600 dark:bg-zinc-800 dark:text-zinc-100"
              required
            />
          </div>

          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            <div className="space-y-2">
              <label className="text-sm font-medium text-zinc-700 dark:text-zinc-300">Current Password</label>
              <Input
                type="password"
                placeholder="Enter current password"
                value={form.current_password}
                onChange={(e) => setForm({ ...form, current_password: e.target.value })}
                className="w-full rounded-md border border-zinc-300 px-3 py-2 text-zinc-900 focus:outline-none focus:ring-2 focus:ring-blue-500 dark:border-zinc-600 dark:bg-zinc-800 dark:text-zinc-100"
              />
            </div>

            <div className="space-y-2">
              <label className="text-sm font-medium text-zinc-700 dark:text-zinc-300">New Password</label>
              <Input
                type="password"
                placeholder="Enter new password"
                value={form.new_password}
                onChange={(e) => setForm({ ...form, new_password: e.target.value })}
                className="w-full rounded-md border border-zinc-300 px-3 py-2 text-zinc-900 focus:outline-none focus:ring-2 focus:ring-blue-500 dark:border-zinc-600 dark:bg-zinc-800 dark:text-zinc-100"
              />
            </div>
          </div>

          {error && <p className="text-sm text-red-500">{error}</p>}
          {success && <p className="text-sm text-green-600 dark:text-green-400">{success}</p>}

          <div className="pt-2">
            <Button
              type="submit"
              disabled={saving}
              className="rounded-md bg-blue-600 px-5 py-2.5 font-semibold text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-70"
            >
              {saving ? 'Saving...' : 'Save Changes'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
