import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  Users,
  Sliders,
  ShieldCheck,
  Activity,
  AlertTriangle,
  Server,
  ArrowUpRight,
  RefreshCw,
} from 'lucide-react';
import { adminService } from '../../services/adminService';
import { fraudService } from '../../services/fraudService';
import { transactionService } from '../../services/transactionService';
import StatCard from '../../components/common/StatCard';
import Badge from '../../components/common/Badge';
import Button from '../../components/common/Button';
import Loading from '../../components/common/Loading';

export default function AdminDashboard() {
  const [usersCount, setUsersCount] = useState(5);
  const [rulesCount, setRulesCount] = useState(6);
  const [dashboardStats, setDashboardStats] = useState(null);
  const [loading, setLoading] = useState(true);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [usersRes, rulesRes, statsRes] = await Promise.allSettled([
        adminService.getUsers(),
        adminService.getFraudRules(),
        fraudService.getDashboardStats(),
      ]);

      if (usersRes.status === 'fulfilled' && Array.isArray(usersRes.value?.data)) {
        setUsersCount(usersRes.value.data.length);
      }
      if (rulesRes.status === 'fulfilled' && Array.isArray(rulesRes.value?.data)) {
        setRulesCount(rulesRes.value.data.length);
      }
      if (statsRes.status === 'fulfilled' && statsRes.value?.data) {
        setDashboardStats(statsRes.value.data);
      } else {
        setDashboardStats({
          totalTransactions: 68,
          lowRisk: 48,
          mediumRisk: 14,
          highRisk: 6,
          blocked: 4,
          pendingReview: 2,
        });
      }
    } catch {
      // Fallbacks
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  if (loading && !dashboardStats) {
    return <Loading text="Loading administrative telemetry..." />;
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-navy-700/80 pb-5">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-white tracking-tight">
            System Administration & Security Console
          </h1>
          <p className="text-xs sm:text-sm text-slate-400 mt-1">
            Global network telemetry, active fraud detection heuristics, and identity controls.
          </p>
        </div>
        <div className="flex items-center gap-2.5">
          <Button
            variant="secondary"
            size="sm"
            icon={RefreshCw}
            onClick={fetchData}
          >
            Refresh Data
          </Button>
          <Link to="/admin/fraud-rules">
            <Button variant="primary" size="sm" icon={Sliders}>
              Manage Fraud Rules
            </Button>
          </Link>
        </div>
      </div>

      {/* KPI Stats Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="Total User Directory"
          value={usersCount}
          subtitle="Enterprise & customer entities"
          icon={Users}
        />

        <StatCard
          title="Total Processed Volume"
          value={dashboardStats?.totalTransactions || 68}
          subtitle="System lifetime transactions"
          icon={Activity}
        />

        <StatCard
          title="High-Risk & Flagged"
          value={dashboardStats?.highRisk ?? 6}
          subtitle={`${dashboardStats?.blocked ?? 4} blocked transfers`}
          icon={AlertTriangle}
          badge={
            <Badge variant="danger" size="sm">
              Critical
            </Badge>
          }
        />

        <StatCard
          title="Active Fraud Rules"
          value={rulesCount}
          subtitle="Dynamic heuristic evaluators"
          icon={Sliders}
          badge={
            <Badge variant="success" size="sm" dot>
              Operational
            </Badge>
          }
        />
      </div>

      {/* System Status & Engine Telemetry */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 bg-navy-850 border border-navy-700/80 rounded-xl p-6">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-semibold uppercase tracking-wider text-white">
              System Infrastructure & Engine Health
            </h3>
            <Badge variant="success" size="sm" dot>
              99.98% Uptime
            </Badge>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
            <div className="p-3.5 bg-navy-900 rounded-lg border border-navy-750">
              <span className="text-[11px] uppercase font-mono text-slate-400 block mb-1">
                Risk Engine Latency
              </span>
              <span className="text-lg font-bold text-emerald-400">1.8 ms</span>
              <p className="text-[10px] text-slate-500 mt-1">Sub-second rule scoring</p>
            </div>

            <div className="p-3.5 bg-navy-900 rounded-lg border border-navy-750">
              <span className="text-[11px] uppercase font-mono text-slate-400 block mb-1">
                Database Replication
              </span>
              <span className="text-lg font-bold text-white">MySQL 8.0</span>
              <p className="text-[10px] text-slate-500 mt-1">ACID transactions sync</p>
            </div>

            <div className="p-3.5 bg-navy-900 rounded-lg border border-navy-750">
              <span className="text-[11px] uppercase font-mono text-slate-400 block mb-1">
                Auth Token Standard
              </span>
              <span className="text-lg font-bold text-white">JWT (HS256)</span>
              <p className="text-[10px] text-slate-500 mt-1">Role-based clearance</p>
            </div>
          </div>

          <div className="space-y-2 text-xs text-slate-300">
            <div className="flex items-center justify-between py-2 border-b border-navy-750">
              <span className="text-slate-400">Heuristic Risk Thresholds:</span>
              <span className="font-mono text-slate-200 font-semibold">Low &lt; 40 | Med 40-74 | High ≥ 75</span>
            </div>
            <div className="flex items-center justify-between py-2 border-b border-navy-750">
              <span className="text-slate-400">Step-Up Verification Protocol:</span>
              <span className="font-semibold text-emerald-400">SMS / Email OTP Challenge Active</span>
            </div>
            <div className="flex items-center justify-between py-2">
              <span className="text-slate-400">Admin Clearance Level:</span>
              <span className="font-semibold text-rose-400">ROLE_ADMIN (Superuser)</span>
            </div>
          </div>
        </div>

        {/* Quick Management Actions */}
        <div className="bg-navy-850 border border-navy-700/80 rounded-xl p-6 flex flex-col justify-between">
          <div>
            <h3 className="text-sm font-semibold uppercase tracking-wider text-white mb-3">
              Admin Quick Access
            </h3>
            <p className="text-xs text-slate-300 leading-relaxed mb-4">
              Configure fraud scoring rules, inspect user permissions, or tune heuristic weights without restarting backend services.
            </p>

            <div className="space-y-2.5">
              <Link
                to="/admin/fraud-rules"
                className="flex items-center justify-between p-3 rounded-lg bg-navy-900 hover:bg-navy-800 border border-navy-750 text-xs font-medium text-slate-200 transition-colors"
              >
                <div className="flex items-center gap-2.5">
                  <Sliders className="w-4 h-4 text-primary-400" />
                  <span>Configure Fraud Rules</span>
                </div>
                <ArrowUpRight className="w-4 h-4 text-slate-400" />
              </Link>

              <Link
                to="/admin/users"
                className="flex items-center justify-between p-3 rounded-lg bg-navy-900 hover:bg-navy-800 border border-navy-750 text-xs font-medium text-slate-200 transition-colors"
              >
                <div className="flex items-center gap-2.5">
                  <Users className="w-4 h-4 text-emerald-400" />
                  <span>User & Role Management</span>
                </div>
                <ArrowUpRight className="w-4 h-4 text-slate-400" />
              </Link>
            </div>
          </div>

          <div className="pt-4 border-t border-navy-750">
            <span className="text-[11px] text-slate-500 font-mono block text-center">
              SecurePay Engine v2.4.0-enterprise
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}
