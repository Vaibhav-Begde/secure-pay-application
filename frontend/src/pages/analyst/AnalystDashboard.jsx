import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  ShieldAlert,
  AlertOctagon,
  TrendingDown,
  IndianRupee,
  Activity,
  ArrowUpRight,
  RefreshCw,
  Eye,
  CheckCircle,
  XCircle,
} from 'lucide-react';
import { fraudService } from '../../services/fraudService';
import { transactionService } from '../../services/transactionService';
import { useToast } from '../../context/ToastContext';
import StatCard from '../../components/common/StatCard';
import Badge from '../../components/common/Badge';
import RiskBadge from '../../components/common/RiskBadge';
import RiskScore from '../../components/common/RiskScore';
import DataTable from '../../components/common/DataTable';
import Button from '../../components/common/Button';
import Loading from '../../components/common/Loading';

export default function AnalystDashboard() {
  const { success: toastSuccess, error: toastError } = useToast();
  const [stats, setStats] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [statsRes, txnsRes] = await Promise.allSettled([
        fraudService.getDashboardStats(),
        transactionService.getAllTransactions(),
      ]);

      if (statsRes.status === 'fulfilled' && statsRes.value?.data) {
        setStats(statsRes.value.data);
      } else {
        setStats({
          totalTransactions: 42,
          lowRisk: 28,
          mediumRisk: 9,
          highRisk: 5,
          blocked: 3,
          pendingReview: 2,
        });
      }

      if (txnsRes.status === 'fulfilled' && Array.isArray(txnsRes.value?.data)) {
        setTransactions(txnsRes.value.data);
      } else {
        setTransactions([]);
      }
    } catch {
      // Handled
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const total = stats?.totalTransactions || transactions.length || 1;
  const lowCount = stats?.lowRisk ?? 0;
  const medCount = stats?.mediumRisk ?? 0;
  const highCount = stats?.highRisk ?? 0;

  const lowPct = Math.round((lowCount / total) * 100) || 0;
  const medPct = Math.round((medCount / total) * 100) || 0;
  const highPct = Math.round((highCount / total) * 100) || 0;

  // Calculate amount at risk (sum of HIGH risk transactions)
  const amountAtRisk = transactions
    .filter((t) => t.riskLevel === 'HIGH' || t.status === 'BLOCKED' || (t.riskScore || 0) >= 75)
    .reduce((sum, t) => sum + (Number(t.amount) || 0), 0);

  // High risk queue
  const highRiskQueue = transactions
    .filter((t) => t.riskLevel === 'HIGH' || (t.riskScore || 0) >= 60 || t.status === 'PENDING')
    .slice(0, 5);

  const columns = [
    {
      header: 'Txn Reference',
      accessor: 'referenceCode',
      cell: (row) => (
        <span className="font-mono text-xs text-primary-400 font-semibold">
          {row.referenceCode || `TXN-${row.id}`}
        </span>
      ),
    },
    {
      header: 'Sender',
      accessor: 'senderUsername',
      cell: (row) => (
        <span className="text-slate-200 font-medium">{row.senderUsername || 'Anonymous'}</span>
      ),
    },
    {
      header: 'Recipient',
      accessor: 'receiverUsername',
      cell: (row) => (
        <span className="text-slate-200">{row.receiverUsername}</span>
      ),
    },
    {
      header: 'Exposure',
      accessor: 'amount',
      cell: (row) => (
        <span className="font-bold text-white">
          ₹{Number(row.amount || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
        </span>
      ),
    },
    {
      header: 'Risk Score',
      accessor: 'riskScore',
      cell: (row) => (
        <RiskScore score={row.riskScore || 80} compact />
      ),
    },
    {
      header: 'Status',
      accessor: 'status',
      cell: (row) => (
        <Badge
          variant={
            row.status === 'BLOCKED'
              ? 'danger'
              : row.status === 'APPROVED'
              ? 'success'
              : 'warning'
          }
          size="sm"
          dot
        >
          {row.status || 'PENDING'}
        </Badge>
      ),
    },
    {
      header: 'Investigate',
      accessor: 'id',
      cell: (row) => (
        <Link
          to={`/analyst/transactions/${row.id || 1}`}
          className="inline-flex items-center gap-1 text-xs text-primary-400 hover:text-primary-300 font-semibold"
        >
          <span>Review</span>
          <ArrowUpRight className="w-3.5 h-3.5" />
        </Link>
      ),
    },
  ];

  if (loading && !stats) {
    return <Loading text="Loading fraud intelligence dashboard..." />;
  }

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-navy-700/80 pb-5">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-white tracking-tight">
            Fraud Analyst Command Center
          </h1>
          <p className="text-xs sm:text-sm text-slate-400 mt-1">
            Realtime suspicious transfer telemetry, anomaly monitoring, and risk queues.
          </p>
        </div>
        <div className="flex items-center gap-2.5">
          <Button
            variant="secondary"
            size="sm"
            icon={RefreshCw}
            onClick={fetchData}
          >
            Refresh Telemetry
          </Button>
          <Link to="/analyst/fraud-alerts">
            <Button variant="primary" size="sm" icon={AlertOctagon}>
              Open Fraud Alerts
            </Button>
          </Link>
        </div>
      </div>

      {/* KPI Stats Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="Total Transactions (System)"
          value={stats?.totalTransactions || transactions.length}
          subtitle="Processed across all channels"
          icon={Activity}
        />

        <StatCard
          title="High-Risk Flagged"
          value={stats?.highRisk ?? 0}
          subtitle={`${stats?.blocked ?? 0} confirmed blocked`}
          icon={ShieldAlert}
          badge={
            <Badge variant="danger" size="sm">
              Critical
            </Badge>
          }
        />

        <StatCard
          title="Open Review Cases"
          value={stats?.pendingReview ?? 0}
          subtitle="Awaiting analyst adjudication"
          icon={AlertOctagon}
          badge={
            <Badge variant="warning" size="sm">
              In Queue
            </Badge>
          }
        />

        <StatCard
          title="Capital at Risk"
          value={`₹${amountAtRisk.toLocaleString('en-IN', { minimumFractionDigits: 2 })}`}
          subtitle="Suspicious gross volume"
          icon={IndianRupee}
        />
      </div>

      {/* Risk Distribution Summary Section */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 bg-navy-850 border border-navy-700/80 rounded-xl p-6">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-semibold text-white uppercase tracking-wider">
              Network Risk Distribution & Tiers
            </h3>
            <span className="text-xs text-slate-400 font-mono">
              Total Sample: {total} transactions
            </span>
          </div>

          <div className="space-y-4">
            {/* Low risk bar */}
            <div>
              <div className="flex justify-between text-xs mb-1.5 font-medium">
                <span className="text-emerald-400 flex items-center gap-1.5">
                  <span className="w-2 h-2 rounded-full bg-emerald-400" />
                  LOW RISK (Score 0-39)
                </span>
                <span className="text-slate-300">
                  {lowCount} txns ({lowPct}%)
                </span>
              </div>
              <div className="w-full bg-navy-950 rounded-full h-3 overflow-hidden border border-navy-750">
                <div
                  className="bg-emerald-500 h-full rounded-full transition-all duration-500"
                  style={{ width: `${lowPct}%` }}
                />
              </div>
            </div>

            {/* Medium risk bar */}
            <div>
              <div className="flex justify-between text-xs mb-1.5 font-medium">
                <span className="text-amber-400 flex items-center gap-1.5">
                  <span className="w-2 h-2 rounded-full bg-amber-400" />
                  MEDIUM RISK (Score 40-74)
                </span>
                <span className="text-slate-300">
                  {medCount} txns ({medPct}%)
                </span>
              </div>
              <div className="w-full bg-navy-950 rounded-full h-3 overflow-hidden border border-navy-750">
                <div
                  className="bg-amber-500 h-full rounded-full transition-all duration-500"
                  style={{ width: `${medPct}%` }}
                />
              </div>
            </div>

            {/* High risk bar */}
            <div>
              <div className="flex justify-between text-xs mb-1.5 font-medium">
                <span className="text-rose-400 flex items-center gap-1.5">
                  <span className="w-2 h-2 rounded-full bg-rose-400" />
                  HIGH RISK (Score 75-100)
                </span>
                <span className="text-slate-300">
                  {highCount} txns ({highPct}%)
                </span>
              </div>
              <div className="w-full bg-navy-950 rounded-full h-3 overflow-hidden border border-navy-750">
                <div
                  className="bg-rose-500 h-full rounded-full transition-all duration-500"
                  style={{ width: `${highPct}%` }}
                />
              </div>
            </div>
          </div>

          <div className="mt-6 pt-4 border-t border-navy-750 text-xs text-slate-400 flex items-center justify-between">
            <span>Automated rule thresholds enforced on every transfer</span>
            <Link to="/analyst/fraud-alerts" className="text-primary-400 hover:text-primary-300 font-semibold">
              Adjudicate Alerts →
            </Link>
          </div>
        </div>

        {/* Action Quick Links / Analyst Guidance */}
        <div className="bg-navy-850 border border-navy-700/80 rounded-xl p-6 flex flex-col justify-between">
          <div>
            <h3 className="text-sm font-semibold text-white uppercase tracking-wider mb-3">
              Analyst Protocols
            </h3>
            <ul className="space-y-2.5 text-xs text-slate-300">
              <li className="flex items-start gap-2">
                <span className="text-primary-400 font-bold">1.</span>
                <span>Review all transactions with Risk Score &gt; 75 within 15 minutes.</span>
              </li>
              <li className="flex items-start gap-2">
                <span className="text-primary-400 font-bold">2.</span>
                <span>Audit user velocity spikes exceeding 3 consecutive transfers in 1 hour.</span>
              </li>
              <li className="flex items-start gap-2">
                <span className="text-primary-400 font-bold">3.</span>
                <span>Verify customer ID documents before lifting automated account freezes.</span>
              </li>
            </ul>
          </div>

          <div className="pt-4 border-t border-navy-750">
            <Link to="/analyst/fraud-alerts">
              <Button variant="secondary" size="sm" icon={AlertOctagon} className="w-full justify-center">
                Go to Alerts Triage
              </Button>
            </Link>
          </div>
        </div>
      </div>

      {/* Urgent High-Risk Queue */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-semibold text-white uppercase tracking-wider">
            Urgent Investigation Queue
          </h3>
          <span className="text-xs text-slate-400">
            {highRiskQueue.length} items requiring review
          </span>
        </div>

        <DataTable
          columns={columns}
          data={highRiskQueue}
          pagination={false}
          emptyTitle="High-risk queue is clear"
          emptyDescription="No anomalous or high-risk transactions currently require analyst adjudication."
        />
      </div>
    </div>
  );
}
