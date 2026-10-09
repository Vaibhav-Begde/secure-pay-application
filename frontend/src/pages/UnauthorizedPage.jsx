import React from 'react';
import { ShieldAlert, ArrowLeft, Home } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import Button from '../components/common/Button';

export default function UnauthorizedPage() {
  const { role } = useAuth();

  const getHomePath = () => {
    if (role === 'ADMIN') return '/admin/dashboard';
    if (role === 'FRAUD_ANALYST') return '/analyst/dashboard';
    return '/customer/dashboard';
  };

  return (
    <div className="min-h-screen bg-navy-950 flex items-center justify-center p-4">
      <div className="max-w-md w-full bg-navy-900 border border-navy-700/80 rounded-lg p-8 text-center shadow-2xl">
        <div className="w-14 h-14 rounded-full bg-rose-950/80 border border-rose-800/80 text-rose-400 flex items-center justify-center mx-auto mb-4">
          <ShieldAlert className="w-7 h-7" />
        </div>

        <h2 className="text-xl font-bold text-white mb-2">Access Denied (403)</h2>
        <p className="text-sm text-slate-400 mb-6 leading-relaxed">
          Your current security clearance level (<span className="text-primary-400 font-semibold">{role || 'GUEST'}</span>) does not allow access to this restricted enterprise resource.
        </p>

        <div className="flex flex-col sm:flex-row items-center justify-center gap-3">
          <Link to={getHomePath()} className="w-full sm:w-auto">
            <Button variant="primary" icon={Home} className="w-full">
              Return to My Dashboard
            </Button>
          </Link>
          <Link to="/login" className="w-full sm:w-auto">
            <Button variant="secondary" icon={ArrowLeft} className="w-full">
              Switch Account
            </Button>
          </Link>
        </div>
      </div>
    </div>
  );
}
