'use client';

import { useState, useEffect } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import axios from '@/lib/axios';
import { isAxiosError } from 'axios';
import GoogleMapReact from 'google-map-react';
import { Button } from '@/components/ui/button';
import RouteDisplay from '../components/RouteDisplay'; // Import RouteDisplay component
import { PostsPageSkeleton } from '@/app/components/PageSkeletons';
import { useAuth } from '../components/AuthContext';

type Post = {
  _id: string;
  post_id: string;
  start_location_name: string;
  end_location_name: string;
  start_lat: number;
  start_lng: number;
  end_lat: number;
  end_lng: number;
  departure_time: string;
  total_seats: number;
  available_seats: number;
  price_per_seat: number;
  distance?: number;
};

export default function Posts() {
  const { isLoggedIn } = useAuth();
  const [location, setLocation] = useState<{ lat: number; lng: number } | null>(null);
  const [mounted, setMounted] = useState(false);
  const [map, setMap] = useState<google.maps.Map | null>(null); // State for map object
  const [maps, setMaps] = useState<typeof google.maps | null>(null); // State for maps object
  const [selectedPostId, setSelectedPostId] = useState<string | null>(null);
  const [bookingMessage, setBookingMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  const [bookingInFlightPostId, setBookingInFlightPostId] = useState<string | null>(null);
  const [requestedPostIds, setRequestedPostIds] = useState<Set<string>>(new Set());

  useEffect(() => {
    if (typeof window !== 'undefined' && navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        pos => {
          setLocation({ lat: pos.coords.latitude, lng: pos.coords.longitude });
          console.log('Geolocation obtained:', pos.coords.latitude, pos.coords.longitude); // Log geolocation
        },
        (error) => {
          console.error("Error getting geolocation:", error);
          setLocation({ lat: 0, lng: 0 }); // Fallback to a default location or handle error display
          console.log('Using fallback location:', 0, 0); // Log fallback
        }
      );
    }
    setMounted(true);
    console.log('Component mounted.'); // Log component mount
  }, []);

  const { data: posts = [], isLoading, error: queryError } = useQuery<Post[]>({ // Added queryError
    queryKey: ['posts', location],
    queryFn: async () => { // Made queryFn async to use try/catch
      try {
        // const url = location?.lat !== 0 ? `/api/v1/posts?lat=${location?.lat}&lng=${location?.lng}&radius=50` : '/api/v1/posts';
        const url = '/api/v1/posts';
        const response = await axios.get(url);
        return response.data;
      } catch (err) {
        console.error('Axios error fetching posts:', err); // Log the Axios error
        throw err; // Re-throw to let useQuery handle it
      }
    },
    enabled: mounted
  });

  const selectedPost = posts.find((post) => post._id === selectedPostId) ?? posts[0] ?? null;

  useEffect(() => {
    if (posts.length > 0 && !selectedPostId) {
      setSelectedPostId(posts[0]._id);
    }
  }, [posts, selectedPostId]);

  const bookRideMutation = useMutation({
    mutationFn: async (post: Post) => {
      setBookingInFlightPostId(post._id);
      setBookingMessage(null);
      const response = await axios.post('/api/v1/bookings', { post_id: post.post_id });
      return { post, data: response.data };
    },
    onSuccess: ({ post }) => {
      setRequestedPostIds((prev) => {
        const next = new Set(prev);
        next.add(post._id);
        return next;
      });
      setBookingMessage({ type: 'success', text: 'Booking request sent.' });
    },
    onError: (error) => {
      const fallback = 'Failed to send booking request.';
      const message = isAxiosError(error)
        ? (error.response?.data?.message as string | undefined) ?? fallback
        : fallback;
      setBookingMessage({ type: 'error', text: message });
    },
    onSettled: () => {
      setBookingInFlightPostId(null);
    }
  });

  useEffect(() => {
    if (posts) {
      console.log('Posts data fetched:', posts); // Log fetched posts data
      console.log('Number of posts:', posts.length); // Log number of posts
    }
    if (isLoading) {
      console.log('Posts data is loading...'); // Log loading state
    }
  }, [posts, isLoading]);

  useEffect(() => {
    if (queryError) {
      console.error('useQuery error fetching posts:', queryError); // Log useQuery specific error
    }
  }, [queryError]);


  const handleApiLoaded = ({ map, maps }: { map: google.maps.Map, maps: typeof google.maps }) => {
    setMap(map);
    setMaps(maps);
    console.log('Google Maps API loaded. Map object:', map, 'Maps object:', maps); // Log API loaded
  };

  if (!mounted) {
    return <PostsPageSkeleton />;
  }

  // Display error message from query if any
  if (queryError) {
    return <div className="p-8 text-center text-red-500">Error loading posts: {queryError.message || 'Unknown error'}</div>;
  }

  const formatDeparture = (isoTime: string) => {
    const date = new Date(isoTime);
    if (Number.isNaN(date.getTime())) return isoTime;
    return date.toLocaleString();
  };

  const distanceLabel = (post: Post) => {
    if (typeof post.distance !== 'number') return 'N/A';
    return `${(post.distance / 1000).toFixed(1)} km`;
  };

  const seatsTaken = (post: Post) => post.total_seats - post.available_seats;

  return (
    <div className="space-y-4">
      {isLoading || !location ? (
        <PostsPageSkeleton />
      ) : (
        <>
          {location && (
            <div className="h-[500px] w-full overflow-hidden rounded-xl border">
              <GoogleMapReact
                bootstrapURLKeys={{ key: 'AIzaSyB6y-549HrO6No2H4yELrxw-phFYRHo5I0' }}
                center={selectedPost ? { lat: selectedPost.end_lat, lng: selectedPost.end_lng } : location}
                zoom={11}
                yesIWantToUseGoogleMapApiInternals
                onGoogleApiLoaded={handleApiLoaded} // Pass callback to get map and maps objects
              >
                {/* Render routes using RouteDisplay component */}
                {map && maps && selectedPost && (
                  <RouteDisplay
                    key={`route-${selectedPost._id}`}
                    map={map}
                    maps={maps}
                    startLat={selectedPost.start_lat}
                    startLng={selectedPost.start_lng}
                    endLat={selectedPost.end_lat}
                    endLng={selectedPost.end_lng}
                  />
                )}
              </GoogleMapReact>
            </div>
          )}

          <div className="rounded-xl border bg-white p-4">
            <div className="mb-3 flex items-center justify-between">
              <h2 className="text-lg font-semibold text-black">Available Routes</h2>
              <span className="text-sm text-zinc-500">{posts.length} found</span>
            </div>

            {bookingMessage && (
              <p
                className={`mb-3 rounded-md px-3 py-2 text-sm ${
                  bookingMessage.type === 'success'
                    ? 'bg-green-50 text-green-700'
                    : 'bg-red-50 text-red-700'
                }`}
              >
                {bookingMessage.text}
              </p>
            )}

            <div className="max-h-[360px] space-y-3 overflow-y-auto pr-1">
              {posts.map((post) => {
                const isSelected = selectedPost?._id === post._id;
                const isRequested = requestedPostIds.has(post._id);
                const isJoining = bookingInFlightPostId === post._id;
                const fullyBooked = post.available_seats <= 0;
                const disableJoin = !isLoggedIn || isRequested || isJoining || fullyBooked;

                return (
                  <div
                    key={post._id}
                    className={`rounded-lg border p-3 transition ${
                      isSelected ? 'border-black bg-zinc-50' : 'border-zinc-200 bg-white'
                    }`}
                  >
                    <div className="mb-2 flex items-start justify-between gap-3">
                      <button
                        className="text-left"
                        type="button"
                        onClick={() => setSelectedPostId(post._id)}
                      >
                        <p className="text-sm text-zinc-500">{post.start_location_name}</p>
                        <p className="font-medium text-black">to {post.end_location_name}</p>
                        <p className="text-xs text-zinc-500">{formatDeparture(post.departure_time)}</p>
                      </button>
                      <div className="text-right text-sm">
                        <p className="font-semibold text-black">฿{post.price_per_seat}</p>
                        <p className="text-zinc-500">{distanceLabel(post)}</p>
                      </div>
                    </div>

                    <div className="flex items-center justify-between">
                      <p className="text-xs text-zinc-600">
                        Seats {seatsTaken(post)}/{post.total_seats}
                      </p>
                      <div className="flex gap-2">
                        <Button
                          className="rounded-md border border-zinc-300 px-3 py-1 text-xs"
                          type="button"
                          onClick={() => setSelectedPostId(post._id)}
                        >
                          Show Route
                        </Button>
                        <Button
                          className="rounded-md bg-black px-3 py-1 text-xs text-white disabled:cursor-not-allowed disabled:bg-zinc-400"
                          type="button"
                          disabled={disableJoin}
                          onClick={() => bookRideMutation.mutate(post)}
                        >
                          {!isLoggedIn
                            ? 'Login to Join'
                            : fullyBooked
                            ? 'Full'
                            : isJoining
                            ? 'Joining...'
                            : isRequested
                            ? 'Requested'
                            : 'Join'}
                        </Button>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </>
      )}
    </div>
  );
}
