// web/src/app/components/RouteDisplay.tsx
'use client';

import React, { useEffect, useState } from 'react';

interface RouteDisplayProps {
  map: google.maps.Map | null;
  maps: typeof google.maps | null;
  startLat: number;
  startLng: number;
  endLat: number;
  endLng: number;
}

const RouteDisplay: React.FC<RouteDisplayProps> = ({ map, maps, startLat, startLng, endLat, endLng }) => {
  const [directionsRenderer, setDirectionsRenderer] = useState<google.maps.DirectionsRenderer | null>(null);

  useEffect(() => {
    console.log('RouteDisplay useEffect running.', { map, maps, startLat, startLng, endLat, endLng }); // Log all props
    if (!map || !maps) {
        console.log('RouteDisplay: Map or maps object is null, skipping directions.');
        return;
    }

    // Create a new DirectionsRenderer only if one doesn't exist for this instance
    const currentRenderer = directionsRenderer || new maps.DirectionsRenderer({ map: map, suppressMarkers: false });
    if (!directionsRenderer) {
        setDirectionsRenderer(currentRenderer);
    }

    const directionsService = new maps.DirectionsService();

    directionsService.route(
      {
        origin: { lat: startLat, lng: startLng },
        destination: { lat: endLat, lng: endLng },
        travelMode: maps.TravelMode.DRIVING,
      },
      (result, status) => {
        console.log(`DirectionsService response for (${startLat},${startLng}) to (${endLat},${endLng}):`, status, result); // Log directions service response
        if (status === maps.DirectionsStatus.OK && result) {
          currentRenderer.setDirections(result);
        } else {
          console.error(`Error fetching directions for (${startLat},${startLng}) to (${endLat},${endLng}): ${status}`);
        }
      }
    );

    // Cleanup function
    return () => {
      if (currentRenderer) { // Use the local renderer reference for cleanup
        currentRenderer.setMap(null); // Remove from map
        console.log('RouteDisplay cleanup: Removed renderer from map.');
      }
    };
  }, [map, maps, startLat, startLng, endLat, endLng]); // Removed directionsRenderer from deps to avoid infinite loop

  return null; // This component doesn't render anything itself, just draws on the map
};

export default RouteDisplay;
