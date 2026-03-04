'use client';

import { useEffect, useMemo, useState } from 'react';
import { io } from 'socket.io-client';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import axios, { SOCKET_BASE_URL } from '@/lib/axios';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import {
  Pagination,
  PaginationContent,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
} from '@/components/ui/pagination';
import { AdminTablePageSkeleton } from '@/app/components/PageSkeletons';

type UserLite = {
  _id?: string;
  first_name?: string;
  last_name?: string;
  email?: string;
  role?: string;
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
  post_id?: string | PostLite;
  reporter_id?: string | UserLite;
  reported_user_id?: string | UserLite;
  stage: 'ongoing' | 'completed';
  category: string;
  description: string;
  status: 'pending' | 'reviewed' | 'resolved' | 'dismissed';
  created_at?: string;
};

const socket = io(SOCKET_BASE_URL);

const REPORT_STATUSES: Array<ReportRow['status']> = ['pending', 'reviewed', 'resolved', 'dismissed'];
const PAGE_SIZE = 10;

function asUser(value: ReportRow['reporter_id']): UserLite | null {
  if (!value || typeof value === 'string') return null;
  return value;
}

function asPost(value: ReportRow['post_id']): PostLite | null {
  if (!value || typeof value === 'string') return null;
  return value;
}

function userName(user: UserLite | null): string {
  if (!user) return '-';
  return `${user.first_name ?? ''} ${user.last_name ?? ''}`.trim() || user.email || '-';
}

export default function AdminReportsPage() {
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(1);
  const [socketReports, setSocketReports] = useState<Record<string, ReportRow>>({});
  const queryClient = useQueryClient();

  const { data: reports, isLoading } = useQuery({
    queryKey: ['admin-reports'],
    queryFn: () => axios.get('/api/v1/admin/reports').then((res) => res.data as ReportRow[]),
  });

  const updateStatusMutation = useMutation({
    mutationFn: ({ id, status }: { id: string; status: ReportRow['status'] }) =>
      axios.patch(`/api/v1/admin/reports/${id}/status`, { status }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-reports'] });
    },
  });

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (token) {
      socket.emit('join_admin', token);
    }

    const onCreated = (incoming: ReportRow) => {
      setSocketReports((prev) => ({ ...prev, [incoming._id]: incoming }));
    };

    const onUpdated = (incoming: ReportRow) => {
      setSocketReports((prev) => ({ ...prev, [incoming._id]: incoming }));
    };

    socket.on('report_created', onCreated);
    socket.on('report_updated', onUpdated);

    return () => {
      socket.off('report_created', onCreated);
      socket.off('report_updated', onUpdated);
    };
  }, []);

  const allReports = useMemo(() => {
    const base = reports ?? [];
    if (!Object.keys(socketReports).length) return base;

    const merged = base.map((item) => socketReports[item._id] ?? item);
    const baseIds = new Set(base.map((item) => item._id));
    const onlyLive = Object.values(socketReports).filter((item) => !baseIds.has(item._id));
    return [...onlyLive, ...merged];
  }, [reports, socketReports]);

  const filteredReports = useMemo(() => {
    const all = allReports;
    const query = search.trim().toLowerCase();
    if (!query) return all;

    return all.filter((report) => {
      const reporter = asUser(report.reporter_id);
      const reported = asUser(report.reported_user_id);
      const post = asPost(report.post_id);
      const rowText = [
        report.report_id,
        report.stage,
        report.category,
        report.description,
        report.status,
        userName(reporter),
        userName(reported),
        post?.post_id,
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase();

      return rowText.includes(query);
    });
  }, [allReports, search]);

  const totalPages = Math.max(1, Math.ceil(filteredReports.length / PAGE_SIZE));
  const currentPage = Math.min(page, totalPages);
  const startIndex = (currentPage - 1) * PAGE_SIZE;
  const paginatedReports = filteredReports.slice(startIndex, startIndex + PAGE_SIZE);

  const pageNumbers = useMemo(() => {
    if (totalPages <= 1) return [1];
    const start = Math.max(1, currentPage - 2);
    const end = Math.min(totalPages, start + 4);
    return Array.from({ length: end - start + 1 }, (_, idx) => start + idx);
  }, [currentPage, totalPages]);

  if (isLoading) {
    return <AdminTablePageSkeleton columns={8} />;
  }

  return (
    <div className="p-6 space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Report Moderation</h1>
      </div>

      <Card className="p-4">
        <div className="mb-4">
          <Input
            placeholder="Search reports..."
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(1);
            }}
            className="w-full max-w-sm rounded-md border border-gray-300 bg-white px-3 py-2 text-gray-900 placeholder:text-gray-500 focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>

        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Report</TableHead>
              <TableHead>Reporter</TableHead>
              <TableHead>Driver</TableHead>
              <TableHead>Stage</TableHead>
              <TableHead>Category</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Created</TableHead>
              <TableHead className="text-right">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {filteredReports.length === 0 ? (
              <TableRow>
                <TableCell colSpan={8} className="py-8 text-center">
                  No reports found.
                </TableCell>
              </TableRow>
            ) : (
              paginatedReports.map((report) => {
                const reporter = asUser(report.reporter_id);
                const reported = asUser(report.reported_user_id);
                const createdAt = report.created_at ? new Date(report.created_at).toLocaleString() : '-';

                return (
                  <TableRow key={report._id}>
                    <TableCell>
                      <div className="text-sm">
                        <div className="font-medium">{report.report_id}</div>
                        <div className="text-gray-500 line-clamp-2">{report.description}</div>
                      </div>
                    </TableCell>
                    <TableCell>
                      <div className="text-sm">
                        <div>{userName(reporter)}</div>
                        <div className="text-gray-500">{reporter?.email ?? '-'}</div>
                      </div>
                    </TableCell>
                    <TableCell>
                      <div className="text-sm">
                        <div>{userName(reported)}</div>
                        <div className="text-gray-500">{reported?.email ?? '-'}</div>
                      </div>
                    </TableCell>
                    <TableCell className="capitalize">{report.stage}</TableCell>
                    <TableCell>{report.category}</TableCell>
                    <TableCell className="capitalize">{report.status}</TableCell>
                    <TableCell>{createdAt}</TableCell>
                    <TableCell className="text-right">
                      <div className="flex justify-end gap-2">
                        {REPORT_STATUSES.filter((status) => status !== report.status).map((status) => (
                          <Button
                            key={status}
                            size="sm"
                            variant="outline"
                            disabled={updateStatusMutation.isPending}
                            onClick={() => updateStatusMutation.mutate({ id: report._id, status })}
                          >
                            {status}
                          </Button>
                        ))}
                      </div>
                    </TableCell>
                  </TableRow>
                );
              })
            )}
          </TableBody>
        </Table>

        {filteredReports.length > PAGE_SIZE ? (
          <Pagination className="mt-4">
            <PaginationContent>
              <PaginationItem>
                <PaginationPrevious
                  href="#"
                  onClick={(e) => {
                    e.preventDefault();
                    setPage((prev) => Math.max(1, prev - 1));
                  }}
                  aria-disabled={currentPage <= 1}
                  className={currentPage <= 1 ? 'pointer-events-none opacity-50' : ''}
                />
              </PaginationItem>
              {pageNumbers.map((num) => (
                <PaginationItem key={num}>
                  <PaginationLink
                    href="#"
                    isActive={num === currentPage}
                    onClick={(e) => {
                      e.preventDefault();
                      setPage(num);
                    }}
                  >
                    {num}
                  </PaginationLink>
                </PaginationItem>
              ))}
              <PaginationItem>
                <PaginationNext
                  href="#"
                  onClick={(e) => {
                    e.preventDefault();
                    setPage((prev) => Math.min(totalPages, prev + 1));
                  }}
                  aria-disabled={currentPage >= totalPages}
                  className={currentPage >= totalPages ? 'pointer-events-none opacity-50' : ''}
                />
              </PaginationItem>
            </PaginationContent>
          </Pagination>
        ) : null}
      </Card>
    </div>
  );
}
