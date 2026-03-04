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

export default function AdminBookingsPage() {
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(1);
  const queryClient = useQueryClient();

  const { data: bookings, isLoading } = useQuery({
    queryKey: ['admin-bookings'],
    queryFn: () => axios.get('/api/v1/admin/bookings').then(res => res.data)
  });

  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: string, status: string }) => 
      axios.patch(`/api/v1/admin/bookings/${id}/status`, { status }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-bookings'] });
    }
  });

  const updateStatus = (id: string, status: string) => {
    statusMutation.mutate({ id, status });
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'confirmed': return 'bg-green-100 text-green-700';
      case 'pending': return 'bg-yellow-100 text-yellow-700';
      case 'cancelled': return 'bg-red-100 text-red-700';
      case 'accepted': return 'bg-blue-100 text-blue-700';
      default: return 'bg-gray-100 text-gray-700';
    }
  };

  const filteredBookings = useMemo(() => {
    const query = search.trim().toLowerCase();
    if (!query) return bookings ?? [];

    return (bookings ?? []).filter((booking: any) => {
      const rowContent = [
        booking.post?.start_location_name,
        booking.post?.end_location_name,
        `${booking.post?.driver_id?.first_name ?? ''} ${booking.post?.driver_id?.last_name ?? ''}`.trim(),
        booking.post?.driver_id?._id,
        `${booking.passenger_id?.first_name ?? ''} ${booking.passenger_id?.last_name ?? ''}`.trim(),
        booking.passenger_id?.email,
        booking.seats_booked,
        booking.status,
        booking.booked_at ? new Date(booking.booked_at).toLocaleDateString() : '',
      ]
        .filter((value) => value !== null && value !== undefined)
        .join(' ')
        .toLowerCase();

      return rowContent.includes(query);
    });
  }, [bookings, search]);

  const totalPages = Math.max(1, Math.ceil(filteredBookings.length / PAGE_SIZE));
  const currentPage = Math.min(page, totalPages);
  const startIndex = (currentPage - 1) * PAGE_SIZE;
  const paginatedBookings = filteredBookings.slice(startIndex, startIndex + PAGE_SIZE);

  const pageNumbers = useMemo(() => {
    if (totalPages <= 1) return [1];
    const start = Math.max(1, currentPage - 2);
    const end = Math.min(totalPages, start + 4);
    return Array.from({ length: end - start + 1 }, (_, idx) => start + idx);
  }, [currentPage, totalPages]);

  if (isLoading) {
    return <AdminTablePageSkeleton columns={6} />;
  }

  return (
    <div className="p-6 space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold">Booking Management</h1>
      </div>

      <Card className="p-4">
        <div className="mb-4">
          <Input
            placeholder="Search bookings..."
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
              <TableHead>Ride / Driver</TableHead>
              <TableHead>Passenger</TableHead>
              <TableHead>Seats</TableHead>
              <TableHead>Booked At</TableHead>
              <TableHead>Status</TableHead>
              <TableHead className="text-right">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {filteredBookings.length === 0 ? (
              <TableRow>
                <TableCell colSpan={6} className="text-center py-8">No bookings found.</TableCell>
              </TableRow>
            ) : (
              paginatedBookings.map((booking: any) => (
                <TableRow key={booking._id}>
                  <TableCell>
                    <div className="text-sm">
                      <div className="font-medium">{booking.post?.start_location_name ?? '-'} → {booking.post?.end_location_name ?? '-'}</div>
                      <div className="text-gray-500">
                        Driver: {booking.post?.driver_id?.first_name ?? '-'} {booking.post?.driver_id?.last_name ?? ''}
                      </div>
                      <div className="text-gray-500">Driver ID: {booking.post?.driver_id?._id ?? '-'}</div>
                    </div>
                  </TableCell>
                  <TableCell>
                    <div className="text-sm">
                      <div className="font-medium">{booking.passenger_id?.first_name} {booking.passenger_id?.last_name}</div>
                      <div className="text-gray-500">{booking.passenger_id?.email}</div>
                    </div>
                  </TableCell>
                  <TableCell>{booking.seats_booked}</TableCell>
                  <TableCell>{new Date(booking.booked_at).toLocaleDateString()}</TableCell>
                  <TableCell>
                    <span className={`px-2 py-1 text-xs rounded-full capitalize ${getStatusColor(booking.status)}`}>
                      {booking.status}
                    </span>
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="flex justify-end gap-2">
                      {booking.status === 'pending' && (
                        <>
                          <Button 
                            variant="outline" 
                            size="sm"
                            className="text-green-600 border-green-200 hover:bg-green-50"
                            onClick={() => updateStatus(booking._id, 'confirmed')}
                          >
                            Confirm
                          </Button>
                          <Button 
                            variant="outline" 
                            size="sm"
                            className="text-red-600 border-red-200 hover:bg-red-50"
                            onClick={() => updateStatus(booking._id, 'cancelled')}
                          >
                            Cancel
                          </Button>
                        </>
                      )}
                      {booking.status !== 'cancelled' && booking.status !== 'pending' && (
                         <Button 
                            variant="outline" 
                            size="sm"
                            onClick={() => updateStatus(booking._id, 'cancelled')}
                          >
                            Cancel
                          </Button>
                      )}
                    </div>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>

        {filteredBookings.length > PAGE_SIZE ? (
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
