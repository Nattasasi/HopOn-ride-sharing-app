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
  verification_status?: string;
  is_verified?: boolean;
};

type VerificationRow = {
  _id: string;
  request_id: string;
  user_id?: UserLite;
  verification_type: 'national_id' | 'student_id';
  verification_doc_url: string;
  verification_notes?: string | null;
  status: 'pending' | 'approved' | 'rejected';
  reviewed_at?: string | null;
  created_at?: string;
};

const socket = io(SOCKET_BASE_URL);
const PAGE_SIZE = 10;

function userName(user?: UserLite): string {
  if (!user) return '-';
  return `${user.first_name ?? ''} ${user.last_name ?? ''}`.trim() || user.email || '-';
}

function resolveDocumentSrc(raw: string): string {
  const value = (raw || '').trim();
  if (!value) return '';
  if (/^https?:\/\//i.test(value)) return value;
  if (/^data:image\//i.test(value)) return value;
  return `data:image/jpeg;base64,${value}`;
}

function statusBadgeClass(status: VerificationRow['status']): string {
  if (status === 'approved') {
    return 'inline-flex rounded-full border border-green-200 bg-green-50 px-2.5 py-1 text-xs font-semibold text-green-700';
  }
  if (status === 'rejected') {
    return 'inline-flex rounded-full border border-red-200 bg-red-50 px-2.5 py-1 text-xs font-semibold text-red-700';
  }
  return 'inline-flex rounded-full border border-amber-200 bg-amber-50 px-2.5 py-1 text-xs font-semibold text-amber-700';
}

export default function AdminVerificationsPage() {
  const queryClient = useQueryClient();
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(1);
  const [socketRows, setSocketRows] = useState<Record<string, VerificationRow>>({});
  const [previewSrc, setPreviewSrc] = useState<string | null>(null);

  const { data: rows, isLoading } = useQuery({
    queryKey: ['admin-verifications'],
    queryFn: () => axios.get('/api/v1/admin/verifications').then((res) => res.data as VerificationRow[]),
  });

  const approveMutation = useMutation({
    mutationFn: (id: string) => axios.patch(`/api/v1/admin/verifications/${id}/approve`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin-verifications'] }),
  });

  const rejectMutation = useMutation({
    mutationFn: (id: string) => axios.patch(`/api/v1/admin/verifications/${id}/reject`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin-verifications'] }),
  });

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (token) socket.emit('join_admin', token);

    const onCreated = (incoming: VerificationRow) => {
      setSocketRows((prev) => ({ ...prev, [incoming._id]: incoming }));
    };
    const onUpdated = (incoming: VerificationRow) => {
      setSocketRows((prev) => ({ ...prev, [incoming._id]: incoming }));
    };

    socket.on('verification_created', onCreated);
    socket.on('verification_updated', onUpdated);
    return () => {
      socket.off('verification_created', onCreated);
      socket.off('verification_updated', onUpdated);
    };
  }, []);

  const allRows = useMemo(() => {
    const base = rows ?? [];
    if (!Object.keys(socketRows).length) return base;
    const merged = base.map((item) => socketRows[item._id] ?? item);
    const baseIds = new Set(base.map((item) => item._id));
    const onlyLive = Object.values(socketRows).filter((item) => !baseIds.has(item._id));
    return [...onlyLive, ...merged];
  }, [rows, socketRows]);

  const filteredRows = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return allRows;
    return allRows.filter((item) => {
      const rowText = [
        item.request_id,
        item.verification_type,
        item.status,
        userName(item.user_id),
        item.user_id?.email,
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase();
      return rowText.includes(q);
    });
  }, [allRows, search]);

  const totalPages = Math.max(1, Math.ceil(filteredRows.length / PAGE_SIZE));
  const currentPage = Math.min(page, totalPages);
  const startIndex = (currentPage - 1) * PAGE_SIZE;
  const paginatedRows = filteredRows.slice(startIndex, startIndex + PAGE_SIZE);

  const pageNumbers = useMemo(() => {
    if (totalPages <= 1) return [1];
    const start = Math.max(1, currentPage - 2);
    const end = Math.min(totalPages, start + 4);
    return Array.from({ length: end - start + 1 }, (_, idx) => start + idx);
  }, [currentPage, totalPages]);

  if (isLoading) {
    return <AdminTablePageSkeleton columns={7} />;
  }

  return (
    <div className="space-y-6 p-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Verification Requests</h1>
      </div>

      <Card className="rounded-xl border border-gray-200 p-5 shadow-sm">
        <div className="mb-5">
          <Input
            placeholder="Search verification requests..."
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(1);
            }}
            className="w-full max-w-sm rounded-lg border border-gray-300 bg-white px-3 py-2 text-gray-900 placeholder:text-gray-500 transition focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
          />
        </div>

        <div className="overflow-hidden rounded-xl border border-gray-200">
          <Table>
          <TableHeader>
            <TableRow className="bg-gray-50">
              <TableHead className="px-4 py-3">Request</TableHead>
              <TableHead className="px-4 py-3">User</TableHead>
              <TableHead className="px-4 py-3">Type</TableHead>
              <TableHead className="px-4 py-3">Document</TableHead>
              <TableHead className="px-4 py-3">Status</TableHead>
              <TableHead className="px-4 py-3">Created</TableHead>
              <TableHead className="px-4 py-3 text-right">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {filteredRows.length === 0 ? (
              <TableRow>
                <TableCell colSpan={7} className="py-8 text-center">
                  No verification requests found.
                </TableCell>
              </TableRow>
            ) : (
              paginatedRows.map((item) => {
                const createdAt = item.created_at ? new Date(item.created_at).toLocaleString() : '-';
                const busy = approveMutation.isPending || rejectMutation.isPending;
                return (
                  <TableRow
                    key={item._id}
                    className="border-t border-gray-200 transition-colors hover:bg-gray-50"
                  >
                    <TableCell className="px-4 py-3 font-medium">{item.request_id}</TableCell>
                    <TableCell className="px-4 py-3">
                      <div className="text-sm">
                        <div>{userName(item.user_id)}</div>
                        <div className="text-gray-500">{item.user_id?.email ?? '-'}</div>
                      </div>
                    </TableCell>
                    <TableCell className="px-4 py-3 capitalize">{item.verification_type.replace('_', ' ')}</TableCell>
                    <TableCell className="px-4 py-3">
                      <button
                        type="button"
                        className="rounded-md border border-blue-200 bg-blue-50 px-2.5 py-1 text-sm font-medium text-blue-700 transition hover:bg-blue-100"
                        onClick={() => setPreviewSrc(resolveDocumentSrc(item.verification_doc_url))}
                      >
                        Preview
                      </button>
                    </TableCell>
                    <TableCell className="px-4 py-3">
                      <span className={statusBadgeClass(item.status)}>{item.status}</span>
                    </TableCell>
                    <TableCell className="px-4 py-3">{createdAt}</TableCell>
                    <TableCell className="px-4 py-3 text-right">
                      <div className="flex justify-end gap-2">
                        <Button
                          size="sm"
                          variant="outline"
                          disabled={busy || item.status !== 'pending'}
                          className="border-green-300 bg-green-50 text-green-700 transition hover:bg-green-100 disabled:cursor-not-allowed disabled:border-gray-200 disabled:bg-gray-100 disabled:text-gray-400"
                          onClick={() => approveMutation.mutate(item._id)}
                        >
                          Approve
                        </Button>
                        <Button
                          size="sm"
                          variant="outline"
                          disabled={busy || item.status !== 'pending'}
                          className="border-red-300 bg-red-50 text-red-700 transition hover:bg-red-100 disabled:cursor-not-allowed disabled:border-gray-200 disabled:bg-gray-100 disabled:text-gray-400"
                          onClick={() => rejectMutation.mutate(item._id)}
                        >
                          Reject
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                );
              })
            )}
          </TableBody>
        </Table>
        </div>

        {filteredRows.length > PAGE_SIZE ? (
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
      {previewSrc && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/65 p-6">
          <div className="w-full max-w-4xl rounded-lg bg-white p-4">
            <div className="mb-3 flex items-center justify-between">
              <h2 className="text-lg font-semibold">Verification Document</h2>
              <Button size="sm" variant="outline" onClick={() => setPreviewSrc(null)}>
                Close
              </Button>
            </div>
            <div className="max-h-[75vh] overflow-auto rounded border p-2">
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img src={previewSrc} alt="Verification document" className="mx-auto h-auto max-w-full" />
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
