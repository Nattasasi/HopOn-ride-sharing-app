'use client';

import { Skeleton } from '@/components/ui/skeleton';

export function AdminTablePageSkeleton({
  title = true,
  hasFilterPills = false,
  columns = 6,
  rows = 6,
}: {
  title?: boolean;
  hasFilterPills?: boolean;
  columns?: number;
  rows?: number;
}) {
  return (
    <div className="p-6 space-y-6">
      {title ? <Skeleton className="h-8 w-56" /> : null}

      <div className="rounded-lg border border-zinc-200 bg-white p-4 dark:border-zinc-700 dark:bg-zinc-900">
        <div className="mb-4 flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
          <Skeleton className="h-10 w-full max-w-sm rounded-md" />
          {hasFilterPills ? (
            <div className="flex flex-wrap gap-2">
              <Skeleton className="h-9 w-16 rounded-md" />
              <Skeleton className="h-9 w-20 rounded-md" />
              <Skeleton className="h-9 w-24 rounded-md" />
              <Skeleton className="h-9 w-24 rounded-md" />
              <Skeleton className="h-9 w-24 rounded-md" />
            </div>
          ) : null}
        </div>

        <div className="overflow-hidden rounded-lg border border-zinc-200 dark:border-zinc-700">
          <div className="grid grid-cols-12 gap-3 border-b border-zinc-200 bg-zinc-50 p-3 dark:border-zinc-700 dark:bg-zinc-800">
            {Array.from({ length: columns }).map((_, i) => (
              <Skeleton key={`head-${i}`} className="col-span-2 h-4 rounded" />
            ))}
          </div>

          <div className="space-y-0 bg-white dark:bg-zinc-900">
            {Array.from({ length: rows }).map((_, rowIndex) => (
              <div
                key={`row-${rowIndex}`}
                className="grid grid-cols-12 gap-3 border-b border-zinc-200 p-3 last:border-b-0 dark:border-zinc-700"
              >
                {Array.from({ length: columns }).map((__, colIndex) => (
                  <Skeleton key={`cell-${rowIndex}-${colIndex}`} className="col-span-2 h-4 rounded" />
                ))}
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

export function DashboardPageSkeleton() {
  return (
    <div className="p-6 space-y-6">
      <Skeleton className="h-8 w-56" />

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
        {Array.from({ length: 4 }).map((_, i) => (
          <div key={i} className="rounded-lg border border-zinc-200 bg-white p-4 dark:border-zinc-700 dark:bg-zinc-900">
            <Skeleton className="h-4 w-24" />
            <Skeleton className="mt-3 h-8 w-12" />
          </div>
        ))}
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <div className="rounded-lg border border-zinc-200 bg-white p-4 dark:border-zinc-700 dark:bg-zinc-900">
          <Skeleton className="mb-4 h-5 w-44" />
          <div className="space-y-3">
            <Skeleton className="h-12 w-full" />
            <Skeleton className="h-12 w-full" />
            <Skeleton className="h-12 w-full" />
          </div>
        </div>
        <div className="rounded-lg border border-zinc-200 bg-white p-4 dark:border-zinc-700 dark:bg-zinc-900">
          <Skeleton className="mb-4 h-5 w-32" />
          <div className="grid grid-cols-2 gap-3">
            <Skeleton className="h-12 w-full" />
            <Skeleton className="h-12 w-full" />
            <Skeleton className="h-12 w-full" />
            <Skeleton className="h-12 w-full" />
          </div>
        </div>
      </div>
    </div>
  );
}

export function SettingsPageSkeleton() {
  return (
    <div className="mx-auto max-w-3xl p-4 md:p-6">
      <div className="rounded-xl border border-zinc-300 bg-white p-6 shadow-sm dark:border-zinc-700 dark:bg-zinc-900">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="mt-2 h-4 w-72" />

        <div className="mt-6 grid grid-cols-1 gap-4 md:grid-cols-2">
          <Skeleton className="h-10 w-full" />
          <Skeleton className="h-10 w-full" />
        </div>

        <Skeleton className="mt-4 h-10 w-full" />

        <div className="mt-4 grid grid-cols-1 gap-4 md:grid-cols-2">
          <Skeleton className="h-10 w-full" />
          <Skeleton className="h-10 w-full" />
        </div>

        <Skeleton className="mt-6 h-10 w-32" />
      </div>
    </div>
  );
}

export function PostsPageSkeleton() {
  return (
    <div className="space-y-4">
      <Skeleton className="h-[500px] w-full rounded-lg" />
      <div className="space-y-3">
        {Array.from({ length: 4 }).map((_, i) => (
          <div key={i} className="rounded-lg border border-zinc-300 bg-white p-4 dark:border-zinc-700 dark:bg-zinc-900">
            <Skeleton className="h-4 w-72" />
            <Skeleton className="mt-2 h-4 w-44" />
          </div>
        ))}
      </div>
    </div>
  );
}

export function ProfilePageSkeleton() {
  return (
    <div className="space-y-4 p-2">
      <Skeleton className="h-8 w-44" />
      <Skeleton className="h-5 w-28" />
      <div className="space-y-2">
        <Skeleton className="h-4 w-full max-w-lg" />
        <Skeleton className="h-4 w-full max-w-lg" />
        <Skeleton className="h-4 w-full max-w-lg" />
      </div>
    </div>
  );
}
