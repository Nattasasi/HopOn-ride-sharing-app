'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import axios from '@/lib/axios';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';

export default function AdminBookingsPage() {
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

  return (
    <div className="p-6 space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold">Booking Management</h1>
      </div>

      <Card className="p-4">
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
            {isLoading ? (
              <TableRow>
                <TableCell colSpan={6} className="text-center py-8">Loading bookings...</TableCell>
              </TableRow>
            ) : bookings?.length === 0 ? (
              <TableRow>
                <TableCell colSpan={6} className="text-center py-8">No bookings found.</TableCell>
              </TableRow>
            ) : (
              bookings?.map((booking: any) => (
                <TableRow key={booking._id}>
                  <TableCell>
                    <div className="text-sm">
                      <div className="font-medium">{booking.post_id?.start_location_name} → {booking.post_id?.end_location_name}</div>
                      <div className="text-gray-500">Driver: {booking.post_id?.driver_id?.first_name} {booking.post_id?.driver_id?.last_name}</div>
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
      </Card>
    </div>
  );
}