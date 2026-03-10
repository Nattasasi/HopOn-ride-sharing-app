// web/src/lib/axios.ts
import axios from 'axios';

const normalizeApiBaseUrl = (rawValue: string): string => {
  const trimmed = rawValue.trim().replace(/\/+$/, '');
  return trimmed.replace(/\/api\/v1$/i, '');
};

const API_BASE_URL = normalizeApiBaseUrl(
  process.env.NEXT_PUBLIC_API_URL || 'http://localhost:5000'
);

export const SOCKET_BASE_URL = API_BASE_URL;

const instance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

const ensureApiPrefix = (url?: string): string | undefined => {
  if (!url) return url;
  if (/^https?:\/\//i.test(url)) return url;

  const normalizedUrl = url.startsWith('/') ? url : `/${url}`;
  if (normalizedUrl === '/api/v1' || normalizedUrl.startsWith('/api/v1/')) return normalizedUrl;
  return `/api/v1${normalizedUrl}`;
};

// Add a request interceptor to include the JWT token
instance.interceptors.request.use(
  (config) => {
    config.baseURL = config.baseURL || API_BASE_URL;
    config.url = ensureApiPrefix(config.url);

    if (typeof window !== 'undefined') {
      const token = localStorage.getItem('token');
      if (token && config.headers) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    }
    return config;
  },
  (error) => {
    console.error('Axios Interceptor - Error Response:', error.response?.status, error.response?.data);
    return Promise.reject(error);
  }
);

instance.interceptors.response.use(
  (response) => response,
  (error) => {
    if (
      typeof window !== 'undefined' &&
      error?.response?.status === 401
    ) {
      const requestUrl = String(error?.config?.url || '');
      const isAuthEndpoint = requestUrl.includes('/auth/login') || requestUrl.includes('/auth/register');

      if (!isAuthEndpoint) {
        localStorage.removeItem('token');
        localStorage.removeItem('userId');
        window.location.href = '/';
      }
    }
    return Promise.reject(error);
  }
);

export default instance;
