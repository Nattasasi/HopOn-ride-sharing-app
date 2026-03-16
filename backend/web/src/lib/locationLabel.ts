const GENERIC_LOCATION_LABELS = new Set([
  'current location',
  'your live location',
  'choose meetup location',
]);

const hasFiniteNumber = (value: unknown): value is number =>
  typeof value === 'number' && Number.isFinite(value);

const isGenericLocationLabel = (value?: string | null): boolean => {
  const normalized = value?.trim().toLowerCase();
  return Boolean(normalized && GENERIC_LOCATION_LABELS.has(normalized));
};

const formatCoordinates = (lat?: number | null, lng?: number | null): string | null => {
  if (!hasFiniteNumber(lat) || !hasFiniteNumber(lng)) return null;
  return `${lat.toFixed(5)}, ${lng.toFixed(5)}`;
};

export const formatLocationLabel = (
  label?: string | null,
  lat?: number | null,
  lng?: number | null,
  fallback = 'Live location'
): string => {
  if (!isGenericLocationLabel(label)) {
    const cleaned = label?.trim();
    if (cleaned) return cleaned;
  }

  const coordinates = formatCoordinates(lat, lng);
  if (coordinates) return `Live location (${coordinates})`;
  return fallback;
};

export const formatRouteLabel = (
  startLabel?: string | null,
  endLabel?: string | null,
  startLat?: number | null,
  startLng?: number | null,
  fallback = 'Unknown route'
): string => {
  const start = formatLocationLabel(startLabel, startLat, startLng);
  const end = endLabel?.trim();
  if (!start && !end) return fallback;
  if (!end) return start;
  return `${start} to ${end}`;
};
