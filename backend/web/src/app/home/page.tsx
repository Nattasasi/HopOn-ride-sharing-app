// web/src/app/home/page.tsx
'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '../components/AuthContext';

export default function HomePage() {
  const { isLoggedIn } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!isLoggedIn) {
      router.push('/'); // Redirect to landing page if not logged in
    }
  }, [isLoggedIn, router]);

  if (!isLoggedIn) {
    return <div className="p-8 text-center">Redirecting to login...</div>;
  }

  return (
    <div>
      <h1 className="mb-4 text-4xl font-bold text-black dark:text-white">Welcome, Carpooler!</h1>
      <p className="mb-8 text-lg text-zinc-600 dark:text-zinc-400">
        Manage your carpool activities, find rides, and connect with others.
      </p>
      <div className="rounded-xl border-2 border-dashed border-gray-300 p-8 text-center text-gray-500 dark:border-gray-600">
        Page content for Home goes here.
      </div>
    </div>
  );
}
