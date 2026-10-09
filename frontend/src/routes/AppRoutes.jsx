import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

// Auth Pages
import LoginPage from '../pages/auth/LoginPage';
import RegisterPage from '../pages/auth/RegisterPage';
import VerifyOtpPage from '../pages/auth/VerifyOtpPage';

// Customer Pages
import CustomerDashboard from '../pages/customer/CustomerDashboard';
import WalletPage from '../pages/customer/WalletPage';
import TransferPage from '../pages/customer/TransferPage';
import TransactionsPage from '../pages/customer/TransactionsPage';
import ProfilePage from '../pages/customer/ProfilePage';

// Analyst Pages
import AnalystDashboard from '../pages/analyst/AnalystDashboard';
import FraudAlertsPage from '../pages/analyst/FraudAlertsPage';
import TransactionInvestigation from '../pages/analyst/TransactionInvestigation';

// Admin Pages
import AdminDashboard from '../pages/admin/AdminDashboard';
import FraudRulesPage from '../pages/admin/FraudRulesPage';
import UsersPage from '../pages/admin/UsersPage';

// Errors & Layout
import DashboardLayout from '../components/layout/DashboardLayout';
import ProtectedRoute from './ProtectedRoute';
import UnauthorizedPage from '../pages/UnauthorizedPage';
import NotFoundPage from '../pages/NotFoundPage';

// Root redirector based on authenticated role
function RootRedirect() {
  const { isAuthenticated, role } = useAuth();
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  if (role === 'ADMIN') {
    return <Navigate to="/admin/dashboard" replace />;
  }
  if (role === 'FRAUD_ANALYST') {
    return <Navigate to="/analyst/dashboard" replace />;
  }
  return <Navigate to="/customer/dashboard" replace />;
}

export default function AppRoutes() {
  return (
    <Routes>
      {/* Root redirect */}
      <Route path="/" element={<RootRedirect />} />

      {/* Public / Auth Routes */}
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/verify-otp" element={<VerifyOtpPage />} />

      {/* CUSTOMER Protected Routes */}
      <Route element={<ProtectedRoute allowedRoles={['CUSTOMER', 'ADMIN']} />}>
        <Route element={<DashboardLayout />}>
          <Route path="/customer/dashboard" element={<CustomerDashboard />} />
          <Route path="/customer/wallet" element={<WalletPage />} />
          <Route path="/customer/transfer" element={<TransferPage />} />
          <Route path="/customer/transactions" element={<TransactionsPage />} />
        </Route>
      </Route>

      {/* Profile is available to every authenticated role. */}
      <Route element={<ProtectedRoute allowedRoles={['CUSTOMER', 'FRAUD_ANALYST', 'ADMIN']} />}>
        <Route element={<DashboardLayout />}>
          <Route path="/profile" element={<ProfilePage />} />
        </Route>
      </Route>

      {/* FRAUD ANALYST Protected Routes */}
      <Route element={<ProtectedRoute allowedRoles={['FRAUD_ANALYST', 'ADMIN']} />}>
        <Route element={<DashboardLayout />}>
          <Route path="/analyst/dashboard" element={<AnalystDashboard />} />
          <Route path="/analyst/fraud-alerts" element={<FraudAlertsPage />} />
          <Route path="/analyst/transactions/:id" element={<TransactionInvestigation />} />
        </Route>
      </Route>

      {/* ADMIN Protected Routes */}
      <Route element={<ProtectedRoute allowedRoles={['ADMIN']} />}>
        <Route element={<DashboardLayout />}>
          <Route path="/admin/dashboard" element={<AdminDashboard />} />
          <Route path="/admin/fraud-rules" element={<FraudRulesPage />} />
          <Route path="/admin/users" element={<UsersPage />} />
        </Route>
      </Route>

      {/* Unauthorized & 404 Pages */}
      <Route path="/unauthorized" element={<UnauthorizedPage />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
