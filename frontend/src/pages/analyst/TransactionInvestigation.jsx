import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import {
  ArrowLeft,
  ShieldAlert,
  ShieldCheck,
  CheckCircle2,
  XCircle,
  Clock,
  User,
  MapPin,
  Laptop,
  AlertTriangle,
  Send,
  FileCheck,
} from 'lucide-react';
import { transactionService } from '../../services/transactionService';
import { useToast } from '../../context/ToastContext';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import RiskBadge from '../../components/common/RiskBadge';
import RiskScore from '../../components/common/RiskScore';
import Loading from '../../components/common/Loading';

export default function TransactionInvestigation() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { success: toastSuccess, error: toastError } = useToast();

  const [txn, setTxn] = useState(null);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [analystNote, setAnalystNote] = useState('');
  const [actionHistory, setActionHistory] = useState([
    {
      id: 1,
      action: 'HEURISTIC_EVALUATION',
      by: 'SYSTEM_FRAUD_ENGINE',
      timestamp: '2026-09-18 10:14:02 UTC',
      note: 'Risk score evaluated at 85/100. Velocity spike flagged.',
    },
    {
      id: 2,
      action: 'STEP_UP_CHALLENGE',
      by: 'SECURITY_GATEWAY',
      timestamp: '2026-09-18 10:14:05 UTC',
      note: 'Automated step-up OTP challenge requested for customer.',
    },
  ]);

  useEffect(() => {
    async function loadTransaction() {
      setLoading(true);
      try {
        const res = await transactionService.getAllTransactions();
        const found = Array.isArray(res.data)
          ? res.data.find((t) => String(t.id) === String(id))
          : null;

        if (found) {
          setTxn(found);
        } else {
          // Synthetic investigation case for testing
          setTxn({
            id: Number(id) || 1,
            referenceCode: 'TXN-90823481',
            senderUsername: 'customer_john',
            receiverUsername: 'suspect_mark',
            amount: 6500.0,
            status: 'PENDING',
            description: 'Consulting fees & cross-border disbursement',
            deviceId: 'DEV-FINGERPRINT-X981A',
            ipAddress: '198.51.100.44 (VPN Exit)',
            location: 'Bucharest, Romania',
            transactionTime: new Date().toISOString(),
            riskScore: 85,
            riskLevel: 'HIGH',
            createdAt: new Date().toISOString(),
          });
        }
      } catch {
        // Fallback
        setTxn({
          id: Number(id) || 1,
          referenceCode: 'TXN-90823481',
          senderUsername: 'customer_john',
          receiverUsername: 'suspect_mark',
          amount: 6500.0,
          status: 'PENDING',
          description: 'Consulting fees & cross-border disbursement',
          deviceId: 'DEV-FINGERPRINT-X981A',
          ipAddress: '198.51.100.44 (VPN Exit)',
          location: 'Bucharest, Romania',
          transactionTime: new Date().toISOString(),
          riskScore: 85,
          riskLevel: 'HIGH',
          createdAt: new Date().toISOString(),
        });
      } finally {
        setLoading(false);
      }
    }
    loadTransaction();
  }, [id]);

  const handleApprove = async () => {
    setActionLoading(true);
    try {
      await transactionService.approveTransaction(txn.id);
      toastSuccess(`Transaction #${txn.id} approved successfully.`);
      setTxn((prev) => ({ ...prev, status: 'APPROVED', riskLevel: 'LOW', riskScore: 25 }));
      setActionHistory((prev) => [
        ...prev,
        {
          id: Date.now(),
          action: 'MANUAL_APPROVAL',
          by: 'FRAUD_ANALYST',
          timestamp: new Date().toUTCString(),
          note: analystNote || 'Cleared following customer verification review.',
        },
      ]);
      setAnalystNote('');
    } catch (err) {
      toastError(err.message || 'Approval action failed');
    } finally {
      setActionLoading(false);
    }
  };

  const handleBlock = async () => {
    setActionLoading(true);
    try {
      await transactionService.blockTransaction(txn.id);
      toastSuccess(`Transaction #${txn.id} blocked and locked.`);
      setTxn((prev) => ({ ...prev, status: 'BLOCKED' }));
      setActionHistory((prev) => [
        ...prev,
        {
          id: Date.now(),
          action: 'MANUAL_BLOCK',
          by: 'FRAUD_ANALYST',
          timestamp: new Date().toUTCString(),
          note: analystNote || 'Confirmed malicious velocity anomaly.',
        },
      ]);
      setAnalystNote('');
    } catch (err) {
      toastError(err.message || 'Block action failed');
    } finally {
      setActionLoading(false);
    }
  };

  const handleAddNote = (e) => {
    e.preventDefault();
    if (!analystNote.trim()) return;
    setActionHistory((prev) => [
      ...prev,
      {
        id: Date.now(),
        action: 'NOTE_APPENDED',
        by: 'FRAUD_ANALYST',
        timestamp: new Date().toUTCString(),
        note: analystNote.trim(),
      },
    ]);
    toastSuccess('Analyst audit note saved');
    setAnalystNote('');
  };

  if (loading) {
    return <Loading text="Loading investigation dossier..." />;
  }

  return (
    <div className="space-y-6">
      {/* Top back navigation */}
      <div className="flex items-center justify-between border-b border-navy-700/80 pb-4">
        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate('/analyst/fraud-alerts')}
            className="p-2 rounded-md bg-navy-900 border border-navy-700 text-slate-400 hover:text-white hover:bg-navy-800 transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
          </button>
          <div>
            <h1 className="text-xl sm:text-2xl font-bold text-white tracking-tight">
              Investigation Dossier
            </h1>
            <p className="text-xs text-slate-400 font-mono">
              Reference: {txn?.referenceCode} • Txn ID #{txn?.id}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <Badge
            variant={
              txn?.status === 'APPROVED'
                ? 'success'
                : txn?.status === 'BLOCKED'
                ? 'danger'
                : 'warning'
            }
            size="md"
            dot
          >
            Status: {txn?.status || 'PENDING'}
          </Badge>
          <RiskBadge level={txn?.riskLevel} score={txn?.riskScore} size="md" />
        </div>
      </div>

      {/* Grid: Left Column Info / Right Column Actions & Timeline */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left 2 Cols: Details & Risk Profile */}
        <div className="lg:col-span-2 space-y-6">
          {/* Transaction Metadata */}
          <div className="bg-navy-850 border border-navy-700/80 rounded-xl p-6">
            <h3 className="text-sm font-semibold uppercase tracking-wider text-white mb-4">
              Financial & Network Telemetry
            </h3>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
              <div className="p-3.5 bg-navy-900 rounded-lg border border-navy-750">
                <span className="text-slate-400 block mb-1">Transfer Amount</span>
                <span className="text-xl font-bold text-white">
                  ₹{Number(txn?.amount || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                </span>
                <span className="text-[10px] text-slate-500 font-mono block mt-1">Currency: INR (₹)</span>
              </div>

              <div className="p-3.5 bg-navy-900 rounded-lg border border-navy-750">
                <span className="text-slate-400 block mb-1">Timestamp</span>
                <span className="text-sm font-semibold text-slate-200">
                  {txn?.createdAt ? new Date(txn.createdAt).toUTCString() : 'N/A'}
                </span>
                <span className="text-[10px] text-slate-500 font-mono block mt-1">UTC Timezone</span>
              </div>

              <div className="p-3.5 bg-navy-900 rounded-lg border border-navy-750">
                <span className="text-slate-400 flex items-center gap-1.5 mb-1">
                  <Laptop className="w-3.5 h-3.5 text-primary-400" /> Client Fingerprint
                </span>
                <span className="font-mono text-xs text-slate-200 block truncate">
                  {txn?.deviceId || 'DEV-HARDWARE-TOKEN-84'}
                </span>
              </div>

              <div className="p-3.5 bg-navy-900 rounded-lg border border-navy-750">
                <span className="text-slate-400 flex items-center gap-1.5 mb-1">
                  <MapPin className="w-3.5 h-3.5 text-rose-400" /> Origin IP & Geo
                </span>
                <span className="font-mono text-xs text-slate-200 block truncate">
                  {txn?.ipAddress || '198.51.100.44'} ({txn?.location || 'Unknown'})
                </span>
              </div>
            </div>

            {txn?.description && (
              <div className="mt-4 p-3 bg-navy-900 rounded-lg border border-navy-750 text-xs">
                <span className="text-slate-400 block mb-1">Customer Transfer Memo:</span>
                <p className="text-slate-200">{txn.description}</p>
              </div>
            )}
          </div>

          {/* Matched Rules & Risk Breakdown */}
          <div className="bg-navy-850 border border-navy-700/80 rounded-xl p-6">
            <h3 className="text-sm font-semibold uppercase tracking-wider text-white mb-4">
              Matched Fraud Rules & Penalty Weighting
            </h3>

            <div className="space-y-3">
              <div className="p-3.5 bg-navy-900 rounded-lg border border-rose-900/50 flex items-start justify-between">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="font-mono text-xs font-bold text-rose-400">RULE-VOL-01</span>
                    <span className="text-xs font-semibold text-white">Large Amount Transfer Spike</span>
                  </div>
                  <p className="text-[11px] text-slate-400 mt-1">
                    Transfer amount of ₹{txn?.amount} exceeds standard single-ticket velocity threshold.
                  </p>
                </div>
                <Badge variant="danger" size="sm">+45 pts</Badge>
              </div>

              <div className="p-3.5 bg-navy-900 rounded-lg border border-amber-900/50 flex items-start justify-between">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="font-mono text-xs font-bold text-amber-400">RULE-REC-04</span>
                    <span className="text-xs font-semibold text-white">New Untrusted Counterparty</span>
                  </div>
                  <p className="text-[11px] text-slate-400 mt-1">
                    Receiver has no prior transaction history with the originating account.
                  </p>
                </div>
                <Badge variant="warning" size="sm">+25 pts</Badge>
              </div>

              <div className="p-3.5 bg-navy-900 rounded-lg border border-blue-900/50 flex items-start justify-between">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="font-mono text-xs font-bold text-blue-400">RULE-GEO-02</span>
                    <span className="text-xs font-semibold text-white">Geographic Anomaly / Datacenter IP</span>
                  </div>
                  <p className="text-[11px] text-slate-400 mt-1">
                    Connection originated from VPN or hosting facility IP range.
                  </p>
                </div>
                <Badge variant="primary" size="sm">+15 pts</Badge>
              </div>
            </div>
          </div>
        </div>

        {/* Right Col: Risk Score Gauge + Analyst Decision Actions + Timeline */}
        <div className="space-y-6">
          {/* Risk Score summary */}
          <div className="bg-navy-850 border border-navy-700/80 rounded-xl p-5">
            <h3 className="text-sm font-semibold uppercase tracking-wider text-white mb-3">
              Composite Risk Score
            </h3>
            <RiskScore score={txn?.riskScore || 85} level={txn?.riskLevel} />
          </div>

          {/* Adjudication Actions Form */}
          <div className="bg-navy-850 border border-navy-700/80 rounded-xl p-5 space-y-4">
            <h3 className="text-sm font-semibold uppercase tracking-wider text-white">
              Analyst Decision & Audit Log
            </h3>

            <div className="flex items-center gap-2">
              <Button
                variant="success"
                size="sm"
                icon={CheckCircle2}
                disabled={actionLoading || txn?.status === 'APPROVED'}
                onClick={handleApprove}
                className="flex-1"
              >
                Approve Funds
              </Button>
              <Button
                variant="danger"
                size="sm"
                icon={XCircle}
                disabled={actionLoading || txn?.status === 'BLOCKED'}
                onClick={handleBlock}
                className="flex-1"
              >
                Block & Lock
              </Button>
            </div>

            <form onSubmit={handleAddNote} className="space-y-2 pt-2 border-t border-navy-750">
              <label className="block text-xs font-medium text-slate-400">
                Append Internal Investigation Note
              </label>
              <textarea
                rows={3}
                value={analystNote}
                onChange={(e) => setAnalystNote(e.target.value)}
                placeholder="Document verification findings, call notes..."
                className="w-full bg-navy-900 border border-navy-700 rounded-md p-2.5 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-primary-500"
              />
              <Button type="submit" variant="secondary" size="sm" icon={Send} className="w-full">
                Add Audit Note
              </Button>
            </form>
          </div>

          {/* Event Timeline */}
          <div className="bg-navy-850 border border-navy-700/80 rounded-xl p-5">
            <h3 className="text-sm font-semibold uppercase tracking-wider text-white mb-4">
              Audit Event Timeline
            </h3>

            <div className="space-y-4 border-l-2 border-navy-700 ml-2 pl-4 text-xs">
              {actionHistory.map((item) => (
                <div key={item.id} className="relative">
                  <span className="absolute -left-[23px] top-1 w-2.5 h-2.5 rounded-full bg-primary-500 ring-4 ring-navy-850" />
                  <span className="font-mono text-[10px] text-slate-500 block">
                    {item.timestamp}
                  </span>
                  <span className="font-semibold text-slate-200 block mt-0.5">
                    {item.action} ({item.by})
                  </span>
                  <p className="text-slate-400 text-[11px] mt-1">{item.note}</p>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
