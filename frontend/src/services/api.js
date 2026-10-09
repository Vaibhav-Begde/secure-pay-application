import axios from 'axios';

const api = axios.create({
  // Use a public backend URL for Vercel deployments; keep /api for local/proxy use.
  baseURL: import.meta.env.VITE_API_URL || '/api',
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000,
});

// Request interceptor to attach JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('securepay_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor to handle 401 / 403 auth failures
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    // 401 = unauthorized (token expired or missing) -> clear session & redirect to login
    if (status === 401) {
      localStorage.removeItem('securepay_token');
      localStorage.removeItem('securepay_user');
      if (window.location.pathname !== '/login' && window.location.pathname !== '/register') {
        window.location.href = '/login?session_expired=true';
      }
    }
    // 403 = forbidden / access denied -> reject error so caller UI displays warning without logging user out
    return Promise.reject(error);
  }
);

export default api;
