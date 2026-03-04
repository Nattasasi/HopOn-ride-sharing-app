'use client';

import { useMemo, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import axios from '@/lib/axios';
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

const PAGE_SIZE = 10;

export default function AdminPostsPage() {
  const [status, setStatus] = useState('');
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(1);
  const queryClient = useQueryClient();
  const statusOptions = ['', 'active', 'in_progress', 'completed', 'cancelled'];

  const { data: posts, isLoading } = useQuery({
    queryKey: ['admin-posts', status],
    queryFn: () => axios.get(`/api/v1/admin/posts?status=${status}`).then(res => res.data)
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => axios.delete(`/api/v1/admin/posts/${id}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-posts'] });
    }
  });

  const handleDelete = (id: string) => {
    if (confirm('Are you sure you want to delete this ride? This will also cancel all bookings for this ride.')) {
      deleteMutation.mutate(id);
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'active': return 'bg-green-100 text-green-700';
      case 'in_progress': return 'bg-blue-100 text-blue-700';
      case 'completed': return 'bg-gray-100 text-gray-700';
      case 'cancelled': return 'bg-red-100 text-red-700';
      default: return 'bg-gray-100 text-gray-700';
    }
  };

  const filteredPosts = useMemo(() => {
    const query = search.trim().toLowerCase();
    if (!query) return posts ?? [];

    return (posts ?? []).filter((post: any) => {
      const rowContent = [
        post.start_location_name,
        post.end_location_name,
        `${post.driver_id?.first_name ?? ''} ${post.driver_id?.last_name ?? ''}`.trim(),
        post.driver_id?.email,
        post.status,
        post.available_seats,
        post.total_seats,
        post.departure_time ? new Date(post.departure_time).toLocaleString() : '',
      ]
        .filter((value) => value !== null && value !== undefined)
        .join(' ')
        .toLowerCase();

      return rowContent.includes(query);
    });
  }, [posts, search]);

  const totalPages = Math.max(1, Math.ceil(filteredPosts.length / PAGE_SIZE));
  const currentPage = Math.min(page, totalPages);
  const startIndex = (currentPage - 1) * PAGE_SIZE;
  const paginatedPosts = filteredPosts.slice(startIndex, startIndex + PAGE_SIZE);

  const pageNumbers = useMemo(() => {
    if (totalPages <= 1) return [1];
    const start = Math.max(1, currentPage - 2);
    const end = Math.min(totalPages, start + 4);
    return Array.from({ length: end - start + 1 }, (_, idx) => start + idx);
  }, [currentPage, totalPages]);

  if (isLoading) {
    return <AdminTablePageSkeleton hasFilterPills columns={6} />;
  }

  return (
    <div className="p-6 space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Ride Post Management</h1>
      </div>

      <Card className="p-4">
        <div className="mb-4 flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
          <Input
            placeholder="Search rides..."
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(1);
            }}
            className="w-full max-w-sm rounded-md border border-gray-300 bg-white px-3 py-2 text-gray-900 placeholder:text-gray-500 focus:outline-none focus:ring-2 focus:ring-blue-500"
          />

          <div className="flex flex-wrap gap-2">
            {statusOptions.map((s) => {
              const selected = status === s;
              return (
                <button
                  key={s}
                  type="button"
                  onClick={() => {
                    setStatus(s);
                    setPage(1);
                  }}
                  className={[
                    'rounded-md border border-gray-300 px-3 py-2 text-sm font-medium capitalize transition-colors',
                    selected
                      ? 'bg-blue-100 text-blue-700'
                      : 'bg-white text-gray-700 hover:bg-gray-50',
                  ].join(' ')}
                >
                  {s || 'All'}
                </button>
              );
            })}
          </div>
        </div>

        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Route</TableHead>
              <TableHead>Driver</TableHead>
              <TableHead>Departure</TableHead>
              <TableHead>Seats</TableHead>
              <TableHead>Status</TableHead>
              <TableHead className="text-right">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {filteredPosts.length === 0 ? (
              <TableRow>
                <TableCell colSpan={6} className="text-center py-8">No rides found.</TableCell>
              </TableRow>
            ) : (
              paginatedPosts.map((post: any) => (
                <TableRow key={post._id}>
                  <TableCell>
                    <div className="text-sm">
                      <div className="font-medium">{post.start_location_name}</div>
                      <div className="text-gray-500">to {post.end_location_name}</div>
                    </div>
                  </TableCell>
                  <TableCell>
                    <div className="text-sm">
                      <div>{post.driver_id?.first_name} {post.driver_id?.last_name}</div>
                      <div className="text-gray-500">{post.driver_id?.email}</div>
                    </div>
                  </TableCell>
                  <TableCell>{new Date(post.departure_time).toLocaleString()}</TableCell>
                  <TableCell>{post.available_seats} / {post.total_seats}</TableCell>
                  <TableCell>
                    <span className={`px-2 py-1 text-xs rounded-full capitalize ${getStatusColor(post.status)}`}>
                      {post.status}
                    </span>
                  </TableCell>
                  <TableCell className="text-right">
                    <Button 
                      variant="destructive" 
                      size="sm"
                      onClick={() => handleDelete(post._id)}
                    >
                      Delete
                    </Button>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>

        {filteredPosts.length > PAGE_SIZE ? (
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
