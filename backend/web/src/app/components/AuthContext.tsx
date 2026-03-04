// web/src/app/components/AuthContext.tsx
'use client';

import { useRouter } from 'next/navigation';
import React, { createContext, useContext, useState, ReactNode } from 'react';

interface AuthContextType {
  isLoggedIn: boolean;
  userId: string | null;
  login: (id: string, token: string) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const router = useRouter();
  const [userId, setUserId] = useState<string | null>(() => {
    if (typeof window === 'undefined') return null;
    return localStorage.getItem('userId');
  });
  const [isLoggedIn, setIsLoggedIn] = useState<boolean>(() => {
    if (typeof window === 'undefined') return false;
    const storedUserId = localStorage.getItem('userId');
    const storedToken = localStorage.getItem('token');
    return Boolean(storedUserId && storedToken);
  });

  const login = (id: string, token: string) => {
    localStorage.setItem('userId', id);
    localStorage.setItem('token', token);
    setIsLoggedIn(true);
    setUserId(id);
  };

  const logout = () => {
    localStorage.removeItem('userId');
    localStorage.removeItem('token');
    setIsLoggedIn(false);
    setUserId(null);
    router.push('/');
  };

  return (
    <AuthContext.Provider value={{ isLoggedIn, userId, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
