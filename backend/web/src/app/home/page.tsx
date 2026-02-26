// web/src/app/home/page.tsx
'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import Image from "next/image";
import Link from "next/link";
import { useAuth } from '../components/AuthContext';
import { Button } from '@/components/ui';

export default function HomePage() {
  const { isLoggedIn, logout } = useAuth();
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
    <div className="flex min-h-screen bg-zinc-50 font-sans dark:bg-black">
      {/* Sidebar */}
      <aside className="w-64 bg-white dark:bg-gray-800 p-6 shadow-md flex flex-col justify-between">
        <div>
          <h2 className="text-2xl font-semibold mb-6 text-black dark:text-white">Dashboard</h2>
          <nav className="space-y-4">
            <Link href="/posts" className="block text-lg text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-200 hover:underline">
              View Carpool Posts
            </Link>
            <Link href="/posts/create" className="block text-lg text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-200 hover:underline">
              Create New Carpool Post
            </Link>
            <Link href="/admin/dashboard" className="block text-lg text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-200 hover:underline">
              Admin Dashboard
            </Link>
            <Link href="/settings" className="block text-lg text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-200 hover:underline">
              Settings
            </Link>
          </nav>
        </div>
        <div className="mt-8">
          <Button onClick={logout} className="w-full bg-red-600 text-white hover:bg-red-700">
            Logout
          </Button>
        </div>
      </aside>

      {/* Main Content */}
      <main className="flex-1 p-8">
        <h1 className="text-4xl font-bold mb-4 text-black dark:text-white">Welcome, Carpooler!</h1>
        <p className="text-lg text-zinc-600 dark:text-zinc-400 mb-8">
          Manage your carpool activities, find rides, and connect with others.
        </p>

        {/* This is where page content for /home would go. Currently blank as requested. */}
        <div className="border-2 border-dashed border-gray-300 dark:border-gray-600 p-8 text-center text-gray-500">
          Page content for Home goes here.
        </div>
      </main>
    </div>
  );
}
