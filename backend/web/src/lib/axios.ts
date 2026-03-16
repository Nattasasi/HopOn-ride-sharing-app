// web/src/lib/axios.ts
import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';

const normalizeApiBaseUrl = (rawValue: string): string => {
  const trimmed = rawValue.trim().replace(/\/+$/, '');
  return trimmed.replace(/\/api\/v1$/i, '');
};

const rawApiBaseUrl = process.env.NEXT_PUBLIC_API_URL;

if (!rawApiBaseUrl) {
  throw new Error('Missing NEXT_PUBLIC_API_URL. Define it in backend/web/.env.');
}

const API_BASE_URL = normalizeApiBaseUrl(rawApiBaseUrl);
const REFRESH_ENDPOINT = '/api/v1/auth/refresh';

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

const isBrowser = () => typeof window !== 'undefined';

const getStoredAccessToken = (): string | null => {
  if (!isBrowser()) return null;
  return localStorage.getItem('token');
};

const getStoredRefreshToken = (): string | null => {
  if (!isBrowser()) return null;
  return localStorage.getItem('refreshToken');
};

const setStoredAccessToken = (token: string) => {
  if (!isBrowser()) return;
  localStorage.setItem('token', token);
};

const clearStoredAuth = () => {
  if (!isBrowser()) return;
  localStorage.removeItem('token');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('userId');
};

const redirectToLogin = () => {
  if (!isBrowser()) return;
  if (window.location.pathname !== '/') {
    window.location.href = '/';
  }
};

const isPublicAuthPath = (url: string): boolean => {
  return url.includes('/auth/login') || url.includes('/auth/register') || url.includes('/auth/verify');
};

const isRefreshPath = (url: string): boolean => {
  return url.includes('/auth/refresh');
};

type RetriableRequestConfig = InternalAxiosRequestConfig & {
  _retry?: boolean;
};

let refreshRequest: Promise<string> | null = null;

const refreshAccessToken = async (): Promise<string> => {
  const refreshToken = getStoredRefreshToken();
  if (!refreshToken) {
    throw new Error('No refresh token available');
  }

  const response = await axios.post(
    `${API_BASE_URL}${REFRESH_ENDPOINT}`,
    { refreshToken },
    {
      headers: { 'Content-Type': 'application/json' }
    }
  );

  const newToken = response?.data?.token;
  if (!newToken) {
    throw new Error('Refresh endpoint did not return an access token');
  }

  setStoredAccessToken(newToken);
  return newToken;
};

// Add a request interceptor to include the JWT token
instance.interceptors.request.use(
  (config) => {
    config.baseURL = config.baseURL || API_BASE_URL;
    config.url = ensureApiPrefix(config.url);

    const token = getStoredAccessToken();
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
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
  async (error: AxiosError) => {
    const status = error?.response?.status;
    const originalRequest = error?.config as RetriableRequestConfig | undefined;
    const requestUrl = String(originalRequest?.url || '');

    if (!isBrowser() || status !== 401 || !originalRequest) {
      return Promise.reject(error);
    }

    if (isPublicAuthPath(requestUrl) || isRefreshPath(requestUrl) || originalRequest._retry) {
      return Promise.reject(error);
    }

    try {
      if (!refreshRequest) {
        refreshRequest = refreshAccessToken().finally(() => {
          refreshRequest = null;
        });
      }

      const newToken = await refreshRequest;
      originalRequest._retry = true;
      originalRequest.headers = originalRequest.headers || {};
      originalRequest.headers.Authorization = `Bearer ${newToken}`;
      return instance(originalRequest);
    } catch (refreshError) {
      clearStoredAuth();
      redirectToLogin();
      return Promise.reject(refreshError);
    }
  }
);

export default instance;
