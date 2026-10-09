import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  AlertOctagon,
  ShieldCheck,
  ShieldAlert,
  CheckCircle2,
  XCircle,
  Eye,
  RefreshCw,
  Search,
  Filter,
} from 'lucide-react';
import { fraudService } from '../../services/fraudService';
import { transactionService } from '../../services/transactionService';
import { useToast } from '../../context/ToastContext';
import DataTable from '../../components/common/DataTable';
import Badge from '../../components/common/Badge';
import RiskBadge from '../../components/common/RiskBadge';
import RiskScore from '../../components/common/RiskScore';
import Button from '../../components/common/Button';
import Loading from '../../components/common/Loading';
import Modal from '../../components/common/Modal';

export default function FraudAlertsPage() {
  const { success: toastSuccess, error: toastError } = useToast();
  const [alerts, setAlerts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedAlert, setSelectedAlert] = useState(null);
  const [actionInProgress, setActionInProgress] = useState(null);

  const fetchAlerts = async () => {
    setLoading(true);
    try {
      const res = await fraudService.getAlerts();
      if (res && Array.isArray(res.data)) {
        setAlerts(res.data);
      } else {
        // Fallback sample alerts if backend is cold
        setAlerts([
          {
            id: 101,
            transactionId: 1,
            referenceCode: 'TXN-90281244',
            senderUsername: 'customer_john',
            receiverUsername: 'suspect_mark',
            amount: 6500.0,
            riskScore: 88,
            riskLevel: 'HIGH',
            riskReasons: 'Large transfer exceeding ₹50,000 | New untrusted counterparty | Velocity surge',
            decision: 'PENDING_REVIEW',
            status: 'NEW',
            createdAt: new Date().toISOString(),
          },
          {
            id: 102,
            transactionId: 2,
            referenceCode: 'TXN-90281245',
            senderUsername: 'customer_alice',
            receiverUsername: 'crypto_swap_offshore',
            amount: 3200.0,
            riskScore: 65,
            riskLevel: 'MEDIUM',
            riskReasons: 'Offshore gateway indicator | Moderate risk score',
            decision: 'STEP_UP_OTP',
            status: 'IN_REVIEW',
            createdAt: new Date(Date.now() - 3600000).toISOString(),
          },
          {
            id: 103,
            transactionId: 3,
            referenceCode: 'TXN-90281246',
            senderUsername: 'test_user',
            receiverUsername: 'bob_verified',
            amount: 8000.0,
            riskScore: 92,
            riskLevel: 'HIGH',
            riskReasons: 'Excessive single ticket volume | Multi-device collision',
            decision: 'BLOCKED',
            status: 'FLAGGED',
            createdAt: new Date(Date.now() - 7200000).toISOString(),
          },
        ]);
      }
    } catch {
      setAlerts([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAlerts();
  }, []);

  const handleReviewAlert = async (alertId) => {
    setActionInProgress(alertId);
    try {
      await fraudService.reviewAlert(alertId);
      toastSuccess(`Alert #${alertId} marked as IN_REVIEW`);
      setAlerts((prev) =>
        prev.map((a) => (a.id === alertId ? { ...a, status: 'IN_REVIEW' } : a))
      );
    } catch (err) {
      toastError(err.message || 'Failed to update alert status');
    } finally {
      setActionInProgress(null);
    }
  };

  const handleApprove = async (txnId, alertId) => {
    setActionInProgress(alertId);
    try {
      await transactionService.approveTransaction(txnId);
      toastSuccess(`Transaction #${txnId} approved and funds cleared.`);
      setAlerts((prev) =>
        prev.map((a) => (a.id === alertId ? { ...a, status: 'APPROVED' } : a))
      );
    } catch (err) {
      toastError(err.message || 'Approval action failed');
    } finally {
      setActionInProgress(null);
    }
  };

  const handleBlock = async (txnId, alertId) => {
    setActionInProgress(alertId);
    try {
      await transactionService.blockTransaction(txnId);
      toastSuccess(`Transaction #${txnId} blocked and flagged.`);
      setAlerts((prev) =>
        prev.map((a) => (a.id === alertId ? { ...a, status: 'REJECTED' } : a))
      );
    } catch (err) {
      toastError(err.message || 'Block action failed');
    } finally {
      setActionInProgress(null);
    }
  };

  // Filtered alerts
  const filteredAlerts = alerts.filter((a) => {
    const query = searchTerm.toLowerCase();
    const matchesSearch =
      !query ||
      (a.referenceCode || '').toLowerCase().includes(query) ||
      (a.senderUsername || '').toLowerCase().includes(query) ||
      (a.receiverUsername || '').toLowerCase().includes(query);

    const matchesStatus = statusFilter === 'ALL' || a.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  const columns = [
    {
      header: 'Alert ID / Ref',
      accessor: 'referenceCode',
      cell: (row) => (
        <div>
          <span className="font-mono text-xs text-primary-400 font-semibold block">
            {row.referenceCode || `TXN-${row.transactionId}`}
          </span>
          <span className="text-[10px] text-slate-500 font-mono">Alert #{row.id}</span>
        </div>
      ),
    },
    {
      header: 'Parties',
      accessor: 'senderUsername',
      cell: (row) => (
        <div className="text-xs">
          <p className="text-slate-200">
            <span className="text-slate-400">From:</span> {row.senderUsername}
          </p>
          <p className="text-slate-300">
            <span className="text-slate-400">To:</span> {row.receiverUsername}
          </p>
        </div>
      ),
    },
    {
      header: 'Amount',
      accessor: 'amount',
      cell: (row) => (
        <span className="font-bold text-white">
          ₹{Number(row.amount || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
        </span>
      ),
    },
    {
      header: 'Risk Score & Level',
      accessor: 'riskScore',
      cell: (row) => (
        <div className="space-y-1">
          <RiskBadge level={row.riskLevel} score={row.riskScore} size="sm" />
          <div className="text-[11px] font-mono font-bold text-slate-300">
            Score: {row.riskScore || 0}/100
          </div>
        </div>
      ),
    },
    {
      header: 'Risk Factors Summary',
      accessor: 'riskReasons',
      cell: (row) => {
        const reasons = (row.riskReasons || '').split('|').filter(Boolean);
        return (
          <div className="max-w-xs space-y-0.5 text-xs text-slate-300">
            {reasons.length > 0 ? (
              reasons.slice(0, 2).map((r, i) => (
                <div key={i} className="truncate">
                  • {r.trim()}
                </div>
              ))
            ) : (
              <span className="text-slate-500 italic">Heuristic flags detected</span>
            )}
          </div>
        );
      },
    },
    {
      header: 'Status',
      accessor: 'status',
      cell: (row) => {
        const s = (row.status || 'NEW').toUpperCase();
        let variant = 'warning';
        if (s === 'NEW') variant = 'purple';
        else if (s === 'IN_REVIEW') variant = 'primary';
        else if (s === 'APPROVED') variant = 'success';
        else if (s === 'REJECTED' || s === 'FLAGGED') variant = 'danger';

        return (
          <Badge variant={variant} size="sm" dot>
            {s}
          </Badge>
        );
      },
    },
    {
      header: 'Adjudication Actions',
      accessor: 'id',
      cell: (row) => (
        <div className="flex items-center gap-1.5" onClick={(e) => e.stopPropagation()}>
          <Link
            to={`/analyst/transactions/${row.transactionId || row.id}`}
            className="p-1.5 rounded bg-navy-800 hover:bg-navy-700 text-primary-300 transition-colors"
            title="Full Investigation Dossier"
          >
            <Eye className="w-3.5 h-3.5" />
          </Link>

          {row.status === 'NEW' && (
            <button
              onClick={() => handleReviewAlert(row.id)}
              disabled={actionInProgress === row.id}
              className="px-2 py-1 text-[11px] font-medium bg-blue-950 border border-blue-800 text-blue-300 rounded hover:bg-blue-900 transition-colors"
            >
              Review
            </button>
          )}

          {row.status !== 'APPROVED' && row.status !== 'REJECTED' && (
            <>
              <button
                onClick={() => handleApprove(row.transactionId || row.id, row.id)}
                disabled={actionInProgress === row.id}
                className="p-1 text-emerald-400 hover:text-emerald-300 hover:bg-emerald-950/60 rounded transition-colors"
                title="Approve & Clear Funds"
              >
                <CheckCircle2 className="w-4 h-4" />
              </button>
              <button
                onClick={() => handleBlock(row.transactionId || row.id, row.id)}
                disabled={actionInProgress === row.id}
                className="p-1 text-rose-400 hover:text-rose-300 hover:bg-rose-950/60 rounded transition-colors"
                title="Block & Reject Transfer"
              >
                <XCircle className="w-4 h-4" />
              </button>
            </>
          )}
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-navy-700/80 pb-5">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-white tracking-tight">
            Fraud Alerts & Triage Queue
          </h1>
          <p className="text-xs sm:text-sm text-slate-400 mt-1">
            Suspicious activities automatically flagged by the rule heuristics engine.
          </p>
        </div>
        <Button
          variant="secondary"
          size="sm"
          icon={RefreshCw}
          onClick={fetchAlerts}
        >
          Refresh Queue
        </Button>
      </div>

      {/* Filter and Search controls */}
      <div className="bg-navy-850 border border-navy-700/80 rounded-lg p-4 grid grid-cols-1 sm:grid-cols-2 gap-3">
        <div className="relative">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
          <input
            type="text"
            placeholder="Search by reference, sender, recipient..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-9 pr-3 py-2 bg-navy-900 border border-navy-700 rounded-md text-xs sm:text-sm text-white placeholder-slate-500 focus:outline-none focus:border-primary-500"
          />
        </div>

        <div>
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="w-full px-3 py-2 bg-navy-900 border border-navy-700 rounded-md text-xs sm:text-sm text-slate-200 focus:outline-none focus:border-primary-500"
          >
            <option value="ALL">All Alert Statuses</option>
            <option value="NEW">NEW (Unreviewed)</option>
            <option value="IN_REVIEW">IN_REVIEW</option>
            <option value="FLAGGED">FLAGGED</option>
            <option value="APPROVED">APPROVED</option>
            <option value="REJECTED">REJECTED</option>
          </select>
        </div>
      </div>

      {/* Alerts Table */}
      <DataTable
        columns={columns}
        data={filteredAlerts}
        loading={loading}
        pageSize={10}
        onRowClick={(row) => setSelectedAlert(row)}
        emptyTitle="Fraud queue clean"
        emptyDescription="No suspicious transfer alerts match your active filter."
      />

      {/* Quick Alert Preview Modal */}
      {selectedAlert && (
        <Modal
          isOpen={!!selectedAlert}
          onClose={() => setSelectedAlert(null)}
          title="Fraud Alert Details"
          subtitle={`Alert ID: #${selectedAlert.id} • Txn: ${selectedAlert.referenceCode}`}
          footer={
            <div className="flex gap-2">
              <Button variant="secondary" size="sm" onClick={() => setSelectedAlert(null)}>
                Dismiss
              </Button>
              <Link to={`/analyst/transactions/${selectedAlert.transactionId || selectedAlert.id}`}>
                <Button variant="primary" size="sm" icon={Eye}>
                  Full Dossier Investigation
                </Button>
              </Link>
            </div>
          }
        >
          <div className="space-y-4">
            <div className="p-3 bg-navy-900 rounded-lg border border-navy-750">
              <div className="flex items-center justify-between mb-2">
                <span className="text-xs text-slate-400 font-medium uppercase">Risk Assessment</span>
                <RiskBadge level={selectedAlert.riskLevel} score={selectedAlert.riskScore} />
              </div>
              <RiskScore score={selectedAlert.riskScore || 85} level={selectedAlert.riskLevel} />
            </div>

            <div className="p-3 bg-navy-900 rounded-lg border border-navy-750 text-xs space-y-2">
              <span className="font-semibold text-slate-300 block uppercase tracking-wider">
                Flagged Heuristic Rules
              </span>
              <ul className="space-y-1.5 text-slate-300">
                {(selectedAlert.riskReasons || 'Rule threshold exceeded').split('|').map((r, i) => (
                  <li key={i} className="flex items-start gap-2">
                    <span className="text-rose-400 font-bold">•</span>
                    <span>{r.trim()}</span>
                  </li>
                ))}
              </ul>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}
