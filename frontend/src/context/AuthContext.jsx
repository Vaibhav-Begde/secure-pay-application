import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { authService } from '../services/authService';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem('securepay_token') || null);
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem('securepay_user');
    try {
      return saved ? JSON.parse(saved) : null;
    } catch {
      return null;
    }
  });
  const [loading, setLoading] = useState(true);

  // Sync token into state and localStorage
  const saveAuth = useCallback((tokenValue, userData) => {
    setToken(tokenValue);
    setUser(userData);
    if (tokenValue) {
      localStorage.setItem('securepay_token', tokenValue);
    } else {
      localStorage.removeItem('securepay_token');
    }
    if (userData) {
      localStorage.setItem('securepay_user', JSON.stringify(userData));
    } else {
      localStorage.removeItem('securepay_user');
    }
  }, []);

  // Validate token on mount — clears stale/invalid tokens (401 or 403 from backend)
  useEffect(() => {
    let isMounted = true;
    async function verifyAuth() {
      const storedToken = localStorage.getItem('securepay_token');
      if (storedToken) {
        try {
          const res = await authService.getCurrentUser();
          if (isMounted && res && res.data) {
            setUser((prev) => ({ ...prev, ...res.data }));
            localStorage.setItem('securepay_user', JSON.stringify(res.data));
          }
        } catch (err) {
          // If backend returns 401 or 403, the stored token is invalid — clear it
          const status = err?.response?.status;
          if (status === 401 || status === 403) {
            localStorage.removeItem('securepay_token');
            localStorage.removeItem('securepay_user');
            if (isMounted) {
              setToken(null);
              setUser(null);
            }
          }
        }
      }
      if (isMounted) setLoading(false);
    }

    verifyAuth();
    return () => {
      isMounted = false;
    };
  }, []);

  const login = async (usernameOrEmail, password) => {
    const res = await authService.login({ usernameOrEmail, password });
    if (res && res.data) {
      const { token: jwtToken, id, username, email, role, transactionPinSet } = res.data;
      const userData = { id, username, email, role, transactionPinSet };
      saveAuth(jwtToken, userData);
      return userData;
    }
    throw new Error(res?.message || 'Login failed');
  };

  const register = async (registerData) => {
    const res = await authService.register(registerData);
    return res;
  };

  const refreshCurrentUser = async () => {
    const res = await authService.getCurrentUser();
    if (res?.data) {
      setUser((prev) => ({ ...prev, ...res.data }));
      localStorage.setItem('securepay_user', JSON.stringify(res.data));
      return res.data;
    }
    throw new Error('Unable to refresh account details.');
  };

  const logout = () => {
    saveAuth(null, null);
  };

  // Check if current user has any of the requested roles
  const hasRole = (roles) => {
    if (!user || !user.role) return false;
    if (Array.isArray(roles)) {
      return roles.includes(user.role);
    }
    return user.role === roles;
  };

  // NOTE: switchDemoRole removed — fake tokens cause 403 from Spring Security.
  // Use real credentials: customer1/password123, analyst1/password123, admin1/password123

  return (
    <AuthContext.Provider
      value={{
        token,
        user,
        role: user?.role || null,
        isAuthenticated: !!token && !!user,
        loading,
        login,
        register,
        refreshCurrentUser,
        logout,
        hasRole,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
