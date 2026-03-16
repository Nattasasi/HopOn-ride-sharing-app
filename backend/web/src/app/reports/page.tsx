'use client';

import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import axios from '@/lib/axios';
import { Card } from '@/components/ui/card';
import { Input } from '@/components/ui/input';

const STATUS_META: Record<string, { label: string; color: string; guidance: string }> = {
  pending: {
    label: 'Pending review',
    color: 'bg-amber-100 text-amber-800',
    guidance: 'Your report is in queue. Moderators will review details soon.',
  },
  reviewed: {
    label: 'Reviewed',
    color: 'bg-blue-100 text-blue-800',
    guidance: 'A moderator has reviewed the report and may ask for follow-up context.',
  },
  resolved: {
    label: 'Resolved',
    color: 'bg-emerald-100 text-emerald-800',
    guidance: 'Action has been completed based on policy and available evidence.',
  },
  dismissed: {
    label: 'Dismissed',
    color: 'bg-zinc-200 text-zinc-700',
    guidance: 'The report did not meet policy/action criteria with current evidence.',
  },
};

type UserLite = {
  _id?: string;
  first_name?: string;
  last_name?: string;
  email?: string;
};

type PostLite = {
  _id?: string;
  post_id?: string;
  status?: string;
  departure_time?: string;
};

type ReportRow = {
  _id: string;
  report_id: string;
  stage: 'ongoing' | 'completed';
  category: string;
  description: string;
  status: 'pending' | 'reviewed' | 'resolved' | 'dismissed';
  resolution_notes?: string | null;
  resolved_at?: string | null;
  created_at?: string;
  post_id?: string | PostLite;
  reported_user_id?: string | UserLite;
};

function asUser(value: ReportRow['reported_user_id']): UserLite | null {
  if (!value || typeof value === 'string') return null;
  return value;
}

function asPost(value: ReportRow['post_id']): PostLite | null {
  if (!value || typeof value === 'string') return null;
  return value;
}

function displayUserName(user: UserLite | null): string {
  if (!user) return '-';
  const full = `${user.first_name ?? ''} ${user.last_name ?? ''}`.trim();
  return full || user.email || '-';
}

const categoryHints: Array<{ key: string; title: string; hint: string }> = [
  {
    key: 'payment',
    title: 'Payment mismatch',
    hint: 'Include amount expected/paid, payment method, and any screenshot reference.',
  },
  {
    key: 'unsafe',
    title: 'Safety concern',
    hint: 'Include location/time and a clear sequence of events for moderation.',
  },
  {
    key: 'behavior',
    title: 'Driver behavior',
    hint: 'Describe behavior impact and whether the trip could continue safely.',
  },
  {
    key: 'other',
    title: 'Other issue',
    hint: 'Provide concise facts, what was expected, and what resolution you need.',
  },
];

export default function ReportsPage() {
  const [search, setSearch] = useState('');

  const { data: reports = [], isLoading } = useQuery({
    queryKey: ['my-reports'],
    queryFn: () => axios.get('/api/v1/reports/me').then((res) => res.data as ReportRow[]),
  });

  const filtered = useMemo(() => {
    const query = search.trim().toLowerCase();
    if (!query) return reports;
    return reports.filter((report) => {
      const reportedUser = asUser(report.reported_user_id);
      const post = asPost(report.post_id);
      const bag = [
        report.report_id,
        report.category,
        report.description,
        report.status,
        report.stage,
        displayUserName(reportedUser),
        post?.post_id,
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase();
      return bag.includes(query);
    });
  }, [reports, search]);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-black">Issue Resolution Tracker</h1>
        <p className="mt-1 text-sm text-zinc-600">
          Track report progress and next actions for completed or in-progress ride issues.
        </p>
      </div>

      <Card className="p-4">
        <p className="text-sm font-semibold text-black">What to include in new reports</p>
        <div className="mt-3 grid gap-3 md:grid-cols-2">
          {categoryHints.map((item) => (
            <div key={item.key} className="rounded-lg border border-zinc-200 bg-zinc-50 p-3">
              <p className="text-sm font-semibold text-black">{item.title}</p>
              <p className="mt-1 text-xs text-zinc-600">{item.hint}</p>
            </div>
          ))}
        </div>
      </Card>

      <Card className="p-4">
        <Input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Search by report id, category, status, or driver"
          className="max-w-md"
        />

        {isLoading ? (
          <p className="mt-4 text-sm text-zinc-500">Loading reports...</p>
        ) : filtered.length === 0 ? (
          <p className="mt-4 text-sm text-zinc-500">No reports found yet.</p>
        ) : (
          <div className="mt-4 space-y-3">
            {filtered.map((report) => {
              const meta = STATUS_META[report.status] ?? STATUS_META.pending;
              const post = asPost(report.post_id);
              const reportedUser = asUser(report.reported_user_id);

              return (
                <div key={report._id} className="rounded-xl border border-zinc-200 bg-white p-4">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <p className="text-sm font-semibold text-black">{report.report_id}</p>
                    <span className={`rounded-full px-2 py-1 text-xs font-semibold ${meta.color}`}>
                      {meta.label}
                    </span>
                  </div>

                  <p className="mt-2 text-sm text-zinc-700">{report.description}</p>

                  <div className="mt-3 grid gap-2 text-xs text-zinc-600 md:grid-cols-2">
                    <p>Category: {report.category}</p>
                    <p>Stage: {report.stage}</p>
                    <p>Ride: {post?.post_id ?? '-'}</p>
                    <p>Reported driver: {displayUserName(reportedUser)}</p>
                    <p>Created: {report.created_at ? new Date(report.created_at).toLocaleString() : '-'}</p>
                    <p>Resolved: {report.resolved_at ? new Date(report.resolved_at).toLocaleString() : '-'}</p>
                  </div>

                  <div className="mt-3 rounded-lg border border-zinc-200 bg-zinc-50 p-3 text-xs text-zinc-700">
                    <p className="font-semibold text-zinc-900">Next step</p>
                    <p className="mt-1">{meta.guidance}</p>
                    {report.resolution_notes ? (
                      <p className="mt-2 border-t border-zinc-200 pt-2">
                        Moderator notes: {report.resolution_notes}
                      </p>
                    ) : null}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </Card>
    </div>
  );
}
