'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from './components/AuthContext';
import LoginModal from './components/LoginModal';
import { Button } from '@/components/ui';

export default function LandingPage() {
  const { isLoggedIn } = useAuth();
  const router = useRouter();
  const [showLoginModal, setShowLoginModal] = useState(false);

  useEffect(() => {
    if (isLoggedIn) {
      router.push('/home');
    }
  }, [isLoggedIn, router]);

  if (isLoggedIn) {
    // Render nothing or a small loading indicator while redirecting
    return <div className="p-8 text-center">Redirecting to home...</div>;
  }

  return (
    <div className="flex flex-col items-center justify-center min-h-screen bg-gradient-to-r from-blue-500 to-purple-600 text-white p-4">
      <h1 className="text-5xl font-extrabold mb-4 animate-fade-in-down">
        Welcome to Carpool Platform
      </h1>
      <p className="text-xl mb-8 text-center max-w-2xl animate-fade-in-up">
        Connect with commuters, share rides, and make your daily travel efficient and eco-friendly.
      </p>
      <Button
        onClick={() => setShowLoginModal(true)}
        className="bg-yellow-400 text-gray-900 hover:bg-yellow-300 px-8 py-3 rounded-full text-lg font-semibold shadow-lg transition-transform transform hover:scale-105 animate-pop-in"
      >
        Login to Get Started
      </Button>

      {showLoginModal && (
        <LoginModal onClose={() => setShowLoginModal(false)} />
      )}
    </div>
  );
}
