import React from 'react';
import { HelpCircle, Home } from 'lucide-react';
import { Link } from 'react-router-dom';
import Button from '../components/common/Button';

export default function NotFoundPage() {
  return (
    <div className="min-h-screen bg-navy-950 flex items-center justify-center p-4">
      <div className="max-w-md w-full bg-navy-900 border border-navy-700/80 rounded-lg p-8 text-center shadow-2xl">
        <div className="w-14 h-14 rounded-full bg-navy-800 border border-navy-700 text-slate-400 flex items-center justify-center mx-auto mb-4">
          <HelpCircle className="w-7 h-7" />
        </div>

        <h2 className="text-xl font-bold text-white mb-2">404 - Page Not Found</h2>
        <p className="text-sm text-slate-400 mb-6">
          The security endpoint or page you requested does not exist on this network.
        </p>

        <Link to="/login">
          <Button variant="primary" icon={Home}>
            Go to Portal
          </Button>
        </Link>
      </div>
    </div>
  );
}
