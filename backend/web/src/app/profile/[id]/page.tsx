'use client';

import { useParams } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import axios from '@/lib/axios';
import { ProfilePageSkeleton } from '@/app/components/PageSkeletons';

export default function Profile() {
  const { id } = useParams();

  const { data: user, isLoading: isUserLoading } = useQuery({
    queryKey: ['user', id],
    queryFn: () => axios.get(`/api/v1/users/${id}`).then(res => res.data)
  });

  const { data: feedback, isLoading: isFeedbackLoading } = useQuery({
    queryKey: ['feedback', id],
    queryFn: () => axios.get(`/api/v1/users/${id}/feedback`).then(res => res.data)
  });

  if (isUserLoading || isFeedbackLoading) {
    return <ProfilePageSkeleton />;
  }

  return (
    <div>
      <h1>{user?.first_name}</h1>
      <p>Rating: {user?.average_rating}</p>
      <ul>{feedback?.map(f => <li key={f.feedback_id}>{f.rating} - {f.comment}</li>)}</ul>
    </div>
  );
}
