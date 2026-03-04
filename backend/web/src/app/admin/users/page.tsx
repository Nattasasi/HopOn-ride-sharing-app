'use client';

import { useMemo, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import axios from '@/lib/axios';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { AdminTablePageSkeleton } from '@/app/components/PageSkeletons';

export default function UsersPage() {
  const [search, setSearch] = useState('');
  const queryClient = useQueryClient();

  const { data: users, isLoading } = useQuery({
    queryKey: ['admin-users'],
    queryFn: () => {
      console.log('Fetching users, token in localStorage:', !!localStorage.getItem('token'));
      return axios.get('/api/v1/admin/users').then(res => res.data);
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

  const filteredUsers = useMemo(() => {
    const query = search.trim().toLowerCase();
    if (!query) return users ?? [];

    return (users ?? []).filter((user: any) => {
      const rowContent = [
        user.first_name,
        user.last_name,
        user.email,
        user.role,
        user.is_banned ? 'banned' : 'active',
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase();

      return rowContent.includes(query);
    });
  }, [users, search]);

  if (isLoading) {
    return <AdminTablePageSkeleton columns={5} />;
  }

  return (
    <div className="p-6 space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold">User Management</h1>
      </div>

      <Card className="p-4">
        <div className="mb-4">
          <Input 
            placeholder="Search users..." 
            value={search}
            onChange={e => setSearch(e.target.value)}
            className="w-full max-w-sm rounded-md border border-gray-300 bg-white px-3 py-2 text-gray-900 placeholder:text-gray-500 focus:outline-none focus:ring-2 focus:ring-blue-500"
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
            {filteredUsers.length === 0 ? (
              <TableRow>
                <TableCell colSpan={5} className="text-center py-8">No users found.</TableCell>
              </TableRow>
            ) : (
              filteredUsers.map((user: any) => (
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
