'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
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

type BookingResponse = {
  _id: string;
  status?: string;
  /** ISO timestamp: free-cancel window end (booking_at + FREE_CANCEL_WINDOW_MINUTES). Present on active bookings. */
  cancellation_cutoff_at?: string;
  seconds_until_free_cancel?: number;
};

type MyBooking = {
  _id: string;
  post_id: {
    _id: string;
    post_id: string;
    departure_time: string;
    status: string;
    start_location_name: string;
    end_location_name: string;
  } | null;
  status: string;
};

const CONFLICT_WINDOW_MS = 2 * 60 * 60 * 1000; // ±2 hours

export default function Posts() {
  const { isLoggedIn } = useAuth();
  const router = useRouter();
  const [location, setLocation] = useState<{ lat: number; lng: number } | null>(null);
  const [mounted, setMounted] = useState(false);
  const [map, setMap] = useState<google.maps.Map | null>(null); // State for map object
  const [maps, setMaps] = useState<typeof google.maps | null>(null); // State for maps object
  const [selectedPostId, setSelectedPostId] = useState<string | null>(null);
  const [bookingMessage, setBookingMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  const [bookingInFlightPostId, setBookingInFlightPostId] = useState<string | null>(null);
  const [requestedPostIds, setRequestedPostIds] = useState<Set<string>>(new Set());
  // Maps post._id → ISO cutoff string for free-cancel countdown
  const [cutoffByPostId, setCutoffByPostId] = useState<Record<string, string>>({});
  // Wall-clock ticker so countdowns update every second
  const [now, setNow] = useState(() => Date.now());
  const [conflictWarning, setConflictWarning] = useState<{ target: Post; conflicting: MyBooking } | null>(null);

  useEffect(() => {
    const id = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(id);
  }, []);

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

  const { data: myBookings = [] } = useQuery<MyBooking[]>({
    queryKey: ['my-bookings'],
    queryFn: () => axios.get<MyBooking[]>('/api/v1/bookings/me').then(res => res.data),
    enabled: isLoggedIn
  });

  const { data: posts = [], isLoading, error: queryError, refetch } = useQuery<Post[]>({ // Added queryError
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
      const response = await axios.post<BookingResponse>('/api/v1/bookings', { post_id: post.post_id });
      return { post, data: response.data };
    },
    onSuccess: ({ post, data }) => {
      setRequestedPostIds((prev) => {
        const next = new Set(prev);
        next.add(post._id);
        return next;
      });
      if (data.cancellation_cutoff_at) {
        setCutoffByPostId((prev) => ({ ...prev, [post._id]: data.cancellation_cutoff_at! }));
      }
      setBookingMessage({ type: 'success', text: 'Booking request sent.' });
    },
    onError: (error) => {
      const code = isAxiosError(error) ? (error.response?.data?.code as string | undefined) : undefined;
      const fallback = 'Failed to send booking request.';
      const message = code === 'ACTIVE_BOOKING_CONFLICT'
        ? 'You already have an overlapping active booking. Cancel it first or use the Replace option.'
        : isAxiosError(error) ? ((error.response?.data?.message as string | undefined) ?? fallback) : fallback;
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

  const queryStatus = (() => {
    if (queryError) {
      if (isAxiosError(queryError) && [401, 403].includes(queryError.response?.status ?? 0)) {
        return {
          title: 'Session expired',
          message: 'Sign in again to keep browsing live routes and sending requests.',
          actionLabel: 'Sign in again',
          action: () => router.push('/')
        };
      }
      return {
        title: 'Couldn’t load routes',
        message: 'We couldn’t reach the ride service. Check your connection and try again.',
        actionLabel: 'Retry routes',
        action: () => refetch()
      };
    }
    if (!isLoading && posts.length === 0) {
      return {
        title: 'No nearby rides yet',
        message: 'There are no active rides available right now. Refresh to check again in a moment.',
        actionLabel: 'Refresh routes',
        action: () => refetch()
      };
    }
    return null;
  })();

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

  const buildJoinConfirmationMessage = (post: Post) => {
    const seatsAvailable = Math.max(0, post.available_seats);
    const pricePerSeat = Number.isFinite(post.price_per_seat) ? `฿${post.price_per_seat}` : 'Not set';
    const total = Number.isFinite(post.price_per_seat) ? `฿${post.price_per_seat}` : 'Not set';

    const departureEpoch = new Date(post.departure_time).getTime();
    const cancelCutoffEpoch = departureEpoch - 30 * 60 * 1000;
    const cancelPolicy = Number.isFinite(cancelCutoffEpoch) && cancelCutoffEpoch > Date.now()
      ? `Free cancellation until ${new Date(cancelCutoffEpoch).toLocaleString()}.`
      : 'Cancellation window may already be closed for this ride.';

    return [
      'Confirm booking request?',
      `Seats available: ${seatsAvailable}`,
      `Price per seat: ${pricePerSeat}`,
      `Total for 1 seat: ${total}`,
      '',
      cancelPolicy,
    ].join('\n');
  };

  const findConflictingBooking = (targetPost: Post): MyBooking | null => {
    const targetDep = new Date(targetPost.departure_time).getTime();
    if (isNaN(targetDep)) return null;
    return myBookings.find(b => {
      if (!['pending', 'confirmed', 'accepted'].includes(b.status)) return false;
      const postStatus = b.post_id?.status;
      if (postStatus !== 'active' && postStatus !== 'in_progress') return false;
      if (b.post_id?.post_id === targetPost.post_id) return false;
      const existingDep = b.post_id?.departure_time
        ? new Date(b.post_id.departure_time).getTime() : NaN;
      if (isNaN(existingDep)) return false;
      return Math.abs(existingDep - targetDep) < CONFLICT_WINDOW_MS;
    }) ?? null;
  };

  const handleJoinRequest = (post: Post) => {
    const conflict = findConflictingBooking(post);
    if (conflict) {
      setConflictWarning({ target: post, conflicting: conflict });
      return;
    }
    const ok = window.confirm(buildJoinConfirmationMessage(post));
    if (!ok) return;
    bookRideMutation.mutate(post);
  };

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

            {conflictWarning && (
              <div className="mb-3 rounded-md border border-yellow-400 bg-yellow-50 p-4">
                <p className="text-sm font-semibold text-yellow-800">Overlapping ride time</p>
                <p className="mt-1 text-sm text-yellow-700">
                  You already have a booking for{' '}
                  <strong>{conflictWarning.conflicting.post_id?.start_location_name}</strong>
                  {' \u2192 '}
                  <strong>{conflictWarning.conflicting.post_id?.end_location_name}</strong>
                  {' at '}
                  {conflictWarning.conflicting.post_id?.departure_time
                    ? new Date(conflictWarning.conflicting.post_id.departure_time).toLocaleString()
                    : 'unknown time'}
                  {' — within 2 hours of this ride.'}
                </p>
                <div className="mt-3 flex flex-wrap gap-2">
                  <Button
                    size="sm"
                    variant="outline"
                    type="button"
                    onClick={() => setConflictWarning(null)}
                  >
                    Cancel
                  </Button>
                  <Button
                    size="sm"
                    variant="outline"
                    type="button"
                    onClick={() => {
                      const target = conflictWarning.target;
                      setConflictWarning(null);
                      bookRideMutation.mutate(target);
                    }}
                  >
                    Keep Both
                  </Button>
                  <Button
                    size="sm"
                    type="button"
                    onClick={async () => {
                      const oldId = conflictWarning.conflicting._id;
                      const target = conflictWarning.target;
                      setConflictWarning(null);
                      try {
                        await axios.patch(`/api/v1/bookings/${oldId}/cancel`);
                        bookRideMutation.mutate(target);
                      } catch {
                        setBookingMessage({ type: 'error', text: 'Failed to replace booking. Please try again.' });
                      }
                    }}
                  >
                    Replace
                  </Button>
                </div>
              </div>
            )}

            <div className="max-h-[360px] space-y-3 overflow-y-auto pr-1">
              {queryStatus ? (
                <div className="rounded-xl border border-zinc-200 bg-zinc-50 p-4 text-sm">
                  <p className="text-base font-semibold text-black">{queryStatus.title}</p>
                  <p className="mt-2 text-zinc-600">{queryStatus.message}</p>
                  <Button
                    className="mt-4 rounded-md bg-black px-3 py-1 text-xs text-white"
                    type="button"
                    onClick={queryStatus.action}
                  >
                    {queryStatus.actionLabel}
                  </Button>
                </div>
              ) : posts.map((post) => {
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
                          onClick={() => handleJoinRequest(post)}
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
                    {isRequested && cutoffByPostId[post._id] && (() => {
                      const secsLeft = Math.max(
                        0,
                        Math.floor((new Date(cutoffByPostId[post._id]).getTime() - now) / 1000)
                      );
                      if (secsLeft <= 0) return null;
                      const m = Math.floor(secsLeft / 60);
                      const s = secsLeft % 60;
                      return (
                        <p className="mt-1 text-xs text-emerald-600">
                          Free cancel in {m > 0 ? `${m}m ` : ''}{s}s
                        </p>
                      );
                    })()}
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
