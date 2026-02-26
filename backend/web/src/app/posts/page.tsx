'use client';

import { useState, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import axios from '@/lib/axios';
import GoogleMapReact from 'google-map-react';
import { Button } from '@/components/ui/button';
import RouteDisplay from '../components/RouteDisplay'; // Import RouteDisplay component

export default function Posts() {
  const [location, setLocation] = useState<{ lat: number; lng: number } | null>(null);
  const [mounted, setMounted] = useState(false);
  const [map, setMap] = useState<google.maps.Map | null>(null); // State for map object
  const [maps, setMaps] = useState<typeof google.maps | null>(null); // State for maps object

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

  const { data: posts, isLoading, error: queryError } = useQuery({ // Added queryError
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
    return <div>Loading application...</div>;
  }

  // Display error message from query if any
  if (queryError) {
    return <div className="p-8 text-center text-red-500">Error loading posts: {queryError.message || 'Unknown error'}</div>;
  }

  return (
    <div>
      {isLoading || !location ? (
        <div>Loading Map and Posts...</div>
      ) : (
        <>
          {location && (
            <div style={{ height: '500px', width: '100%' }}>
              <GoogleMapReact
                bootstrapURLKeys={{ key: 'AIzaSyB6y-549HrO6No2H4yELrxw-phFYRHo5I0' }}
                center={location}
                zoom={11}
                yesIWantToUseGoogleMapApiInternals
                onGoogleApiLoaded={handleApiLoaded} // Pass callback to get map and maps objects
              >
                {/* Render routes using RouteDisplay component */}
                {map && maps && posts?.map(post => {
                  console.log('Rendering RouteDisplay for post:', post._id, 'Coords:', post.start_lat, post.start_lng, post.end_lat, post.end_lng); // Log RouteDisplay rendering
                  return (
                  <RouteDisplay
                    key={`route-${post._id}`}
                    map={map}
                    maps={maps}
                    startLat={post.start_lat}
                    startLng={post.start_lng}
                    endLat={post.end_lat}
                    endLng={post.end_lng}
                  />
                )})}
              </GoogleMapReact>
            </div>
          )}
          <ul>
            {posts?.map(post => (
              <li key={post._id}>
                {post.end_location_name} - {post.distance} km - Seats: {post.total_seats - post.available_seats}/{post.total_seats}
                <Button>Join</Button>
              </li>
            ))}
          </ul>
        </>
      )}
    </div>
  );
}