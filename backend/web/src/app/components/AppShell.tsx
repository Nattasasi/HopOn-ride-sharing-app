'use client';

import { ReactNode } from 'react';
import { usePathname } from 'next/navigation';
import AppSidebar from './AppSidebar';
import { useUserSocket } from '../hooks/useUserSocket';

const shellRoutePrefixes = ['/home', '/posts', '/reports', '/settings', '/admin'];

function isShellRoute(pathname: string) {
  return shellRoutePrefixes.some(
    (prefix) => pathname === prefix || pathname.startsWith(`${prefix}/`)
  );
}

export default function AppShell({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  useUserSocket();

  if (!isShellRoute(pathname)) {
    return <>{children}</>;
  }

  return (
    <div className="flex min-h-screen bg-zinc-50 font-sans dark:bg-black">
      <AppSidebar />
      <main className="min-w-0 flex-1 p-6 md:p-8">{children}</main>
    </div>
  );
}
