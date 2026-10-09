import React from 'react';
import { NavLink } from 'react-router-dom';
import {
  Shield,
  LayoutDashboard,
  Wallet,
  Send,
  History,
  AlertOctagon,
  Sliders,
  Users,
  LogOut,
  X,
  Lock,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import Badge from '../common/Badge';

export default function Sidebar({ isOpen, onClose }) {
  const { user, role, logout } = useAuth();

  const customerLinks = [
    { name: 'Dashboard', path: '/customer/dashboard', icon: LayoutDashboard },
    { name: 'Virtual Wallet', path: '/customer/wallet', icon: Wallet },
    { name: 'Send Money', path: '/customer/transfer', icon: Send },
    { name: 'Transactions', path: '/customer/transactions', icon: History },
  ];

  const analystLinks = [
    { name: 'Risk Analytics', path: '/analyst/dashboard', icon: LayoutDashboard },
    { name: 'Fraud Alerts', path: '/analyst/fraud-alerts', icon: AlertOctagon },
  ];

  const adminLinks = [
    { name: 'System Overview', path: '/admin/dashboard', icon: LayoutDashboard },
    { name: 'Fraud Rules Engine', path: '/admin/fraud-rules', icon: Sliders },
    { name: 'User Directory', path: '/admin/users', icon: Users },
  ];

  let links = [];
  let roleLabel = 'Customer';
  let roleBadgeVariant = 'primary';

  if (role === 'FRAUD_ANALYST') {
    links = analystLinks;
    roleLabel = 'Fraud Analyst';
    roleBadgeVariant = 'warning';
  } else if (role === 'ADMIN') {
    links = adminLinks;
    roleLabel = 'System Admin';
    roleBadgeVariant = 'danger';
  } else {
    links = customerLinks;
    roleLabel = 'Customer';
    roleBadgeVariant = 'primary';
  }

  return (
    <>
      {/* Mobile Backdrop */}
      {isOpen && (
        <div
          className="fixed inset-0 z-40 bg-navy-950/80 backdrop-blur-sm lg:hidden"
          onClick={onClose}
        />
      )}

      {/* Sidebar Container */}
      <aside
        className={`fixed top-0 bottom-0 left-0 z-40 w-64 bg-navy-900 border-r border-navy-700 flex flex-col transition-transform duration-200 ease-in-out lg:translate-x-0 ${
          isOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        {/* Brand Header */}
        <div className="h-16 flex items-center justify-between px-5 border-b border-navy-700/80 bg-navy-950/50">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-primary-600/20 border border-primary-500/40 flex items-center justify-center text-primary-400">
              <Shield className="w-5 h-5" />
            </div>
            <div>
              <span className="font-bold tracking-tight text-white text-base">
                Secure<span className="text-primary-400">Pay</span>
              </span>
              <span className="block text-[9px] uppercase tracking-widest text-slate-400 font-mono">
                Fintech Risk OS
              </span>
            </div>
          </div>
          <button
            onClick={onClose}
            className="text-slate-400 hover:text-white lg:hidden p-1 rounded hover:bg-navy-800"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Role & Workspace Indicator */}
        <div className="px-5 py-3 border-b border-navy-700/60 bg-navy-900/60">
          <div className="flex items-center justify-between">
            <span className="text-[11px] uppercase tracking-wider font-semibold text-slate-400">
              Workspace
            </span>
            <Badge variant={roleBadgeVariant} size="sm" dot>
              {roleLabel}
            </Badge>
          </div>
        </div>

        {/* Navigation Links */}
        <nav className="flex-1 px-3 py-4 space-y-1.5 overflow-y-auto">
          <div className="text-[10px] font-semibold uppercase tracking-wider text-slate-500 px-3 mb-2">
            Navigation Menu
          </div>
          {links.map((link) => {
            const Icon = link.icon;
            return (
              <NavLink
                key={link.path}
                to={link.path}
                onClick={onClose}
                className={({ isActive }) =>
                  `flex items-center gap-3 px-3 py-2.5 rounded-md text-sm font-medium transition-colors ${
                    isActive
                      ? 'bg-primary-600/20 text-primary-300 border border-primary-500/40'
                      : 'text-slate-300 hover:bg-navy-800 hover:text-white'
                  }`
                }
              >
                <Icon className="w-4 h-4 shrink-0" />
                <span>{link.name}</span>
              </NavLink>
            );
          })}
        </nav>

        {/* User profile & Logout footer */}
        <div className="p-3 border-t border-navy-700/80 bg-navy-950/40">
          <div className="flex items-center justify-between p-2 rounded-md bg-navy-850 border border-navy-750">
            <div className="flex items-center gap-2.5 overflow-hidden">
              <div className="w-8 h-8 rounded-full bg-navy-700 flex items-center justify-center text-xs font-bold text-primary-300 shrink-0 border border-navy-600">
                {user?.username ? user.username.substring(0, 2).toUpperCase() : 'SP'}
              </div>
              <div className="overflow-hidden">
                <p className="text-xs font-semibold text-white truncate">
                  {user?.username || 'Authenticated User'}
                </p>
                <p className="text-[10px] text-slate-400 truncate">
                  {user?.email || 'user@securepay.internal'}
                </p>
              </div>
            </div>
            <button
              onClick={logout}
              title="Sign Out"
              className="p-1.5 text-slate-400 hover:text-rose-400 rounded-md hover:bg-navy-800 transition-colors"
            >
              <LogOut className="w-4 h-4" />
            </button>
          </div>
        </div>
      </aside>
    </>
  );
}
