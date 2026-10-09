import React from 'react';
import { Menu, LogOut } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

export default function Navbar({ onMenuToggle }) {
  const { user, logout } = useAuth();

  return (
    <header className="h-16 bg-navy-900 border-b border-navy-700/80 sticky top-0 z-30 flex items-center justify-between px-4 sm:px-6">
      {/* Left section: Hamburger toggle and system badge */}
      <div className="flex items-center gap-3">
        <button
          onClick={onMenuToggle}
          className="lg:hidden p-2 rounded-md text-slate-400 hover:text-white hover:bg-navy-800 transition-colors"
          aria-label="Open menu"
        >
          <Menu className="w-5 h-5" />
        </button>

        <div className="hidden sm:flex items-center gap-2 px-2.5 py-1 rounded-full bg-navy-950 border border-navy-750 text-xs text-slate-300">
          <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
          <span className="font-mono text-[11px] text-slate-300">
            Realtime Fraud Engine <span className="text-emerald-400 font-semibold">Active</span>
          </span>
        </div>
      </div>

      {/* Right section: Account details and logout */}
      <div className="flex items-center gap-3">

        {/* User Account Info */}
        <div className="flex items-center gap-3">
          <div className="hidden sm:block text-right">
            <p className="text-xs font-semibold text-white">{user?.username || 'User'}</p>
            <p className="text-[10px] font-mono text-slate-400">{user?.role || 'CUSTOMER'}</p>
          </div>
          <button
            onClick={logout}
            className="p-2 rounded-md text-slate-400 hover:text-rose-400 hover:bg-navy-800 transition-colors"
            title="Logout"
          >
            <LogOut className="w-4 h-4" />
          </button>
        </div>
      </div>
    </header>
  );
}
