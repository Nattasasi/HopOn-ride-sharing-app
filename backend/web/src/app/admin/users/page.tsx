'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import axios from '@/lib/axios';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';

export default function UsersPage() {
  const [search, setSearch] = useState('');
  const queryClient = useQueryClient();

  const { data: users, isLoading } = useQuery({
    queryKey: ['admin-users', search],
    queryFn: () => {
      console.log('Fetching users, token in localStorage:', !!localStorage.getItem('token'));
      return axios.get(`/api/v1/admin/users?search=${search}`).then(res => res.data);
    }
  });

  const banMutation = useMutation({
    mutationFn: ({ id, is_banned }: { id: string, is_banned: boolean }) => 
      axios.patch(`/api/v1/admin/users/${id}/ban`, { is_banned }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-users'] });
    }
  });

  const toggleBan = (user: any) => {
    banMutation.mutate({ id: user._id, is_banned: !user.is_banned });
  };

  return (
    <div className="p-6 space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold">User Management</h1>
      </div>

      <Card className="p-4">
        <div className="mb-4">
          <Input 
            placeholder="Search by name or email..." 
            value={search}
            onChange={e => setSearch(e.target.value)}
            className="max-w-sm"
          />
        </div>

        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Name</TableHead>
              <TableHead>Email</TableHead>
              <TableHead>Role</TableHead>
              <TableHead>Status</TableHead>
              <TableHead className="text-right">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              <TableRow>
                <TableCell colSpan={5} className="text-center py-8">Loading users...</TableCell>
              </TableRow>
            ) : users?.length === 0 ? (
              <TableRow>
                <TableCell colSpan={5} className="text-center py-8">No users found.</TableCell>
              </TableRow>
            ) : (
              users?.map((user: any) => (
                <TableRow key={user._id}>
                  <TableCell className="font-medium">{user.first_name} {user.last_name}</TableCell>
                  <TableCell>{user.email}</TableCell>
                  <TableCell className="capitalize">{user.role}</TableCell>
                  <TableCell>
                    {user.is_banned ? (
                      <span className="px-2 py-1 text-xs bg-red-100 text-red-700 rounded-full">Banned</span>
                    ) : (
                      <span className="px-2 py-1 text-xs bg-green-100 text-green-700 rounded-full">Active</span>
                    )}
                  </TableCell>
                  <TableCell className="text-right">
                    <Button 
                      variant={user.is_banned ? "outline" : "destructive"} 
                      size="sm"
                      onClick={() => toggleBan(user)}
                    >
                      {user.is_banned ? 'Unban' : 'Ban'}
                    </Button>
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