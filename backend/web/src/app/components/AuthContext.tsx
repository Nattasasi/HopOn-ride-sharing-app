// web/src/app/components/AuthContext.tsx
'use client';

import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';

interface AuthContextType {
  isLoggedIn: boolean;
  userId: string | null;
  login: (id: string, token: string) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [userId, setUserId] = useState<string | null>(null);

  useEffect(() => {
    // Check localStorage on mount for existing session
    const storedUserId = localStorage.getItem('userId');
    const storedToken = localStorage.getItem('token');
    if (storedUserId && storedToken) {
      setIsLoggedIn(true);
      setUserId(storedUserId);
    }
  }, []);

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
