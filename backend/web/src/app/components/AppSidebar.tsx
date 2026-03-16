'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useMemo } from 'react';
import { useAuth } from './AuthContext';

type NavItem = {
  href: string;
  label: string;
};

const mainNav: NavItem[] = [
  { href: '/home', label: 'Home' },
  { href: '/posts', label: 'Browse Rides' },
  { href: '/posts/create', label: 'Create Ride' },
  { href: '/reports', label: 'Issue Reports' },
];

const adminNav: NavItem[] = [
  { href: '/admin/dashboard', label: 'Dashboard' },
  { href: '/admin/users', label: 'Users' },
  { href: '/admin/posts', label: 'Ride Posts' },
  { href: '/admin/bookings', label: 'Bookings' },
  { href: '/admin/verifications', label: 'Verifications' },
  { href: '/admin/reports', label: 'Reports' },
  { href: '/admin/emergency', label: 'Emergency' },
  { href: '/admin/settings', label: 'Settings' },
];

function itemClassName(active: boolean) {
  return [
    'block rounded-lg px-3 py-2.5 text-sm font-medium transition-colors',
    active
      ? 'bg-blue-100 text-blue-900 dark:bg-gray-700 dark:text-blue-200'
      : 'text-blue-600 hover:bg-blue-50 hover:text-blue-800 dark:text-blue-400 dark:hover:bg-gray-700/70 dark:hover:text-blue-200',
  ].join(' ');
}

export default function AppSidebar() {
  const pathname = usePathname();
  const { logout } = useAuth();
  const isAdminUser = useMemo(() => {
    if (typeof window === 'undefined') return false;
    const token = localStorage.getItem('token');
    if (!token) return false;
    try {
      const payload = JSON.parse(atob(token.split('.')[1] || ''));
      return payload?.role === 'admin';
    } catch {
      return false;
    }
  }, []);

  const isIdLike = (segment: string) => {
    const isNumeric = /^[0-9]+$/.test(segment);
    const isMongoId = /^[a-f0-9]{24}$/i.test(segment);
    const isUuid =
      /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(
        segment
      );

    return isNumeric || isMongoId || isUuid;
  };

  const isActive = (href: string) => {
    if (pathname === href) return true;

    if (!pathname.startsWith(`${href}/`)) return false;

    const remaining = pathname.slice(href.length + 1);
    const hasSingleSegment = remaining.length > 0 && !remaining.includes('/');

    // Only treat exact `/:id` routes as active descendants.
    if (hasSingleSegment && isIdLike(remaining)) return true;

    return false;
  };

  const isOnAdminRoute = pathname.startsWith('/admin');
  const showAdminOnly = isAdminUser || isOnAdminRoute;

  return (
    <aside className="w-72 shrink-0 border-r border-gray-200 bg-white px-5 py-6 shadow-sm dark:border-gray-700 dark:bg-gray-800">
      <div className="flex h-full flex-col">
        <div className="mb-6">
          <h2 className="text-2xl font-semibold text-black dark:text-white">Dashboard</h2>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-300">HopOn Carpool</p>
        </div>

        <nav className="space-y-6">
          {showAdminOnly ? (
            <div>
              <p className="mb-2 px-1 text-xs font-semibold uppercase tracking-wide text-gray-500 dark:text-gray-300">
                Admin
              </p>
              <ul className="space-y-1.5">
                {adminNav.map((item) => (
                  <li key={item.href}>
                    <Link href={item.href} className={itemClassName(isActive(item.href))}>
                      {item.label}
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          ) : (
            <div>
              <p className="mb-2 px-1 text-xs font-semibold uppercase tracking-wide text-gray-500 dark:text-gray-300">
                Main
              </p>
              <ul className="space-y-1.5">
                {mainNav.map((item) => (
                  <li key={item.href}>
                    <Link href={item.href} className={itemClassName(isActive(item.href))}>
                      {item.label}
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </nav>

        <div className="mt-auto pt-6">
          <button
            type="button"
            onClick={logout}
            className="w-full rounded-lg border border-red-200 bg-red-50 px-3 py-2.5 text-sm font-semibold text-red-700 transition-colors hover:bg-red-100 dark:border-red-300/30 dark:bg-red-900/20 dark:text-red-300 dark:hover:bg-red-900/30"
          >
            Logout
          </button>
        </div>
      </div>
    </aside>
  );
}
