import React, { useState, useEffect, useMemo } from 'react';
import {
  Search,
  Filter,
  RefreshCw,
  Eye,
  Calendar,
  ShieldCheck,
  ShieldAlert,
  Download,
  ArrowDownLeft,
  ArrowUpRight,
} from 'lucide-react';
import { transactionService } from '../../services/transactionService';
import { useAuth } from '../../context/AuthContext';
import DataTable from '../../components/common/DataTable';
import Badge from '../../components/common/Badge';
import RiskBadge from '../../components/common/RiskBadge';
import RiskScore from '../../components/common/RiskScore';
import Button from '../../components/common/Button';
import Modal from '../../components/common/Modal';
import Loading from '../../components/common/Loading';

export default function TransactionsPage() {
  const { user } = useAuth();
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [riskFilter, setRiskFilter] = useState('ALL');
  const [selectedTxn, setSelectedTxn] = useState(null);

  const fetchTransactions = async () => {
    setLoading(true);
    try {
      const res = await transactionService.getHistory();
      if (res && Array.isArray(res.data)) {
        const currentName = (user?.username || '').trim().toLowerCase();
        const visibleTxns = res.data.filter((tx) => {
          const sender = (tx.senderUsername || '').trim().toLowerCase();
          // Sender always sees their transactions (completed, blocked, failed, etc.)
          if (!currentName || sender === currentName) {
            return true;
          }
          // Receiver only sees completed / successful transactions
          const st = (tx.status || '').toUpperCase();
          return st === 'COMPLETED' || st === 'SUCCESS' || st === 'APPROVED';
        });
        setTransactions(visibleTxns);
      } else {
        setTransactions([]);
      }
    } catch {
      setTransactions([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTransactions();
  }, []);

  // Filtering
  const filteredData = useMemo(() => {
    return transactions.filter((item) => {
      // Search term matching
      const query = searchTerm.toLowerCase();
      const ref = (item.referenceCode || `TXN-${item.id}`).toLowerCase();
      const rec = (item.receiverUsername || '').toLowerCase();
      const send = (item.senderUsername || '').toLowerCase();
      const matchesSearch = !query || ref.includes(query) || rec.includes(query) || send.includes(query);

      // Status matching
      const status = (item.status || '').toUpperCase();
      const matchesStatus = statusFilter === 'ALL' || status === statusFilter;

      // Risk matching
      const risk = (item.riskLevel || '').toUpperCase();
      const matchesRisk = riskFilter === 'ALL' || risk === riskFilter;

      return matchesSearch && matchesStatus && matchesRisk;
    });
  }, [transactions, searchTerm, statusFilter, riskFilter]);

  const isCurrentCustomer = (username) =>
    (username || '').trim().toLowerCase() === (user?.username || '').trim().toLowerCase();

  const displayParty = (username, fallback) =>
    isCurrentCustomer(username) ? 'You' : (username || fallback);

  const columns = [
    {
      header: 'Reference Code',
      accessor: 'referenceCode',
      cell: (row) => (
        <span className="font-mono text-xs text-primary-400 font-semibold">
          {row.referenceCode || `TXN-${row.id}`}
        </span>
      ),
    },
    {
      header: 'Type',
      accessor: 'senderUsername',
      cell: (row) => {
        const currentUsername = (user?.username || '').trim().toLowerCase();
        const isDebit = (row.senderUsername || '').trim().toLowerCase() === currentUsername;
        const Icon = isDebit ? ArrowUpRight : ArrowDownLeft;
        return (
          <div className={`inline-flex items-center gap-1.5 text-xs font-semibold ${isDebit ? 'text-rose-400' : 'text-emerald-400'}`}>
            <span className={`flex h-6 w-6 items-center justify-center rounded-full ${isDebit ? 'bg-rose-500/15' : 'bg-emerald-500/15'}`}>
              <Icon className="h-3.5 w-3.5" />
            </span>
            {isDebit ? 'Debit' : 'Credit'}
          </div>
        );
      },
    },
    {
      header: 'Counterparty',
      accessor: 'receiverUsername',
      cell: (row) => {
        const isDebit = isCurrentCustomer(row.senderUsername);
        const counterparty = isDebit ? row.receiverUsername : row.senderUsername;
        return (
          <div>
            <span className="block text-[10px] uppercase tracking-wider text-slate-500">
              {isDebit ? 'To' : 'From'}
            </span>
            <span className="font-medium text-slate-200">
              {displayParty(counterparty, 'External Account')}
            </span>
          </div>
        );
      },
    },
    {
      header: 'Amount',
      accessor: 'amount',
      cell: (row) => (
        <span className="font-semibold text-white">
          ₹{Number(row.amount || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
        </span>
      ),
    },
    {
      header: 'Status',
      accessor: 'status',
      cell: (row) => {
        const s = (row.status || '').toUpperCase();
        let variant = 'neutral';
        if (s === 'SUCCESS' || s === 'COMPLETED' || s === 'APPROVED') variant = 'success';
        else if (s === 'PENDING' || s === 'OTP_REQUIRED') variant = 'warning';
        else if (s === 'BLOCKED' || s === 'REJECTED') variant = 'danger';

        return (
          <Badge variant={variant} size="sm" dot>
            {s || 'PROCESSED'}
          </Badge>
        );
      },
    },
    {
      header: 'Risk Level',
      accessor: 'riskLevel',
      cell: (row) => (
        <RiskBadge level={row.riskLevel} score={row.riskScore} size="sm" />
      ),
    },
    {
      header: 'Date & Time',
      accessor: 'createdAt',
      cell: (row) => (
        <span className="text-xs text-slate-400">
          {row.createdAt ? new Date(row.createdAt).toLocaleString() : 'Just now'}
        </span>
      ),
    },
    {
      header: 'Action',
      accessor: 'id',
      cell: (row) => (
        <button
          onClick={(e) => {
            e.stopPropagation();
            setSelectedTxn(row);
          }}
          className="p-1 text-slate-400 hover:text-primary-400 transition-colors"
          title="Inspect Details"
        >
          <Eye className="w-4 h-4" />
        </button>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-navy-700/80 pb-5">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-white tracking-tight">
            Transaction Ledger & History
          </h1>
          <p className="text-xs sm:text-sm text-slate-400 mt-1">
            Search, filter, and audit all incoming and outgoing financial transfers.
          </p>
        </div>
        <Button
          variant="secondary"
          size="sm"
          icon={RefreshCw}
          onClick={fetchTransactions}
        >
          Refresh List
        </Button>
      </div>

      {/* Filter and Search Bar */}
      <div className="bg-navy-850 border border-navy-700/80 rounded-lg p-4 grid grid-cols-1 sm:grid-cols-3 gap-3">
        {/* Search input */}
        <div className="relative sm:col-span-1">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
          <input
            type="text"
            placeholder="Search by ID, sender, receiver..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-9 pr-3 py-2 bg-navy-900 border border-navy-700 rounded-md text-xs sm:text-sm text-white placeholder-slate-500 focus:outline-none focus:border-primary-500"
          />
        </div>

        {/* Status Filter */}
        <div>
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="w-full px-3 py-2 bg-navy-900 border border-navy-700 rounded-md text-xs sm:text-sm text-slate-200 focus:outline-none focus:border-primary-500"
          >
            <option value="ALL">All Statuses</option>
            <option value="SUCCESS">SUCCESS / COMPLETED</option>
            <option value="PENDING">PENDING</option>
            <option value="OTP_REQUIRED">OTP REQUIRED</option>
            <option value="BLOCKED">BLOCKED</option>
          </select>
        </div>

        {/* Risk Level Filter */}
        <div>
          <select
            value={riskFilter}
            onChange={(e) => setRiskFilter(e.target.value)}
            className="w-full px-3 py-2 bg-navy-900 border border-navy-700 rounded-md text-xs sm:text-sm text-slate-200 focus:outline-none focus:border-primary-500"
          >
            <option value="ALL">All Risk Tiers</option>
            <option value="LOW">LOW Risk</option>
            <option value="MEDIUM">MEDIUM Risk</option>
            <option value="HIGH">HIGH Risk</option>
          </select>
        </div>
      </div>

      {/* Transactions Data Table */}
      <DataTable
        columns={columns}
        data={filteredData}
        loading={loading}
        pageSize={10}
        onRowClick={(row) => setSelectedTxn(row)}
        emptyTitle="No transactions match query"
        emptyDescription="Try adjusting your search terms or filter selections above."
      />

      {/* Transaction Details Modal */}
      {selectedTxn && (
        <Modal
          isOpen={!!selectedTxn}
          onClose={() => setSelectedTxn(null)}
          title="Transaction Audit Record"
          subtitle={`Reference ID: ${selectedTxn.referenceCode || `TXN-${selectedTxn.id}`}`}
          footer={
            <Button variant="secondary" size="sm" onClick={() => setSelectedTxn(null)}>
              Close
            </Button>
          }
        >
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-3 p-3.5 bg-navy-900 rounded-lg border border-navy-750 text-xs">
              <div>
                <span className="text-slate-400 block mb-0.5">Sender Username:</span>
                <span className="font-semibold text-white">{selectedTxn.senderUsername || user?.username}</span>
              </div>
              <div>
                <span className="text-slate-400 block mb-0.5">Receiver Username:</span>
                <span className="font-semibold text-white">{selectedTxn.receiverUsername}</span>
              </div>
              <div>
                <span className="text-slate-400 block mb-0.5">Gross Transfer Amount:</span>
                <span className="font-bold text-primary-400 text-sm">
                  ₹{Number(selectedTxn.amount || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })} INR
                </span>
              </div>
              <div>
                <span className="text-slate-400 block mb-0.5">Transaction Type:</span>
                <span className={`font-semibold ${
                  (selectedTxn.senderUsername || '').trim().toLowerCase() === (user?.username || '').trim().toLowerCase()
                    ? 'text-rose-300'
                    : 'text-emerald-300'
                }`}>
                  {(selectedTxn.senderUsername || '').trim().toLowerCase() === (user?.username || '').trim().toLowerCase() ? 'Debit ↑' : 'Credit ↓'}
                </span>
              </div>
              <div>
                <span className="text-slate-400 block mb-0.5">Transaction Status:</span>
                <Badge variant={selectedTxn.status === 'SUCCESS' ? 'success' : 'warning'} size="sm">
                  {selectedTxn.status}
                </Badge>
              </div>
              <div>
                <span className="text-slate-400 block mb-0.5">Device Fingerprint:</span>
                <span className="font-mono text-slate-300">{selectedTxn.deviceId || 'CLIENT-VERIFIED'}</span>
              </div>
              <div>
                <span className="text-slate-400 block mb-0.5">Client IP Address:</span>
                <span className="font-mono text-slate-300">{selectedTxn.ipAddress || '127.0.0.1'}</span>
              </div>
            </div>

            <div className="p-3.5 bg-navy-900 rounded-lg border border-navy-750">
              <span className="text-xs font-semibold uppercase tracking-wider text-slate-400 block mb-2">
                Fraud Engine Score Breakdown
              </span>
              <div className="flex items-center justify-between mb-2">
                <RiskScore score={selectedTxn.riskScore || 12} compact />
                <RiskBadge level={selectedTxn.riskLevel} score={selectedTxn.riskScore} />
              </div>
              <p className="text-[11px] text-slate-400">
                Evaluation conducted via rule-based anomaly engine with multi-factor risk weighting.
              </p>
            </div>

            {selectedTxn.description && (
              <div className="p-3 bg-navy-900 rounded-lg border border-navy-750 text-xs">
                <span className="text-slate-400 block mb-1">Transfer Memo / Note:</span>
                <p className="text-slate-200">{selectedTxn.description}</p>
              </div>
            )}
          </div>
        </Modal>
      )}
    </div>
  );
}
