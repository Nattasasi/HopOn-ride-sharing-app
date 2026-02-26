// web/src/lib/axios.ts
import axios from 'axios';

const instance = axios.create({
  baseURL: 'http://localhost:5000',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add a request interceptor to include the JWT token
instance.interceptors.request.use(
  (config) => {
    if (typeof window !== 'undefined') {
      const token = localStorage.getItem('token');
      console.log('Axios Interceptor - Request URL:', config.url, 'Token found:', !!token);
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

export default instance;
