import React, { useState, useEffect } from 'react';
import {
  Sliders,
  Plus,
  Edit2,
  Trash2,
  RefreshCw,
  CheckCircle,
  XCircle,
  ShieldCheck,
  AlertTriangle,
} from 'lucide-react';
import { adminService } from '../../services/adminService';
import { useToast } from '../../context/ToastContext';
import DataTable from '../../components/common/DataTable';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import Modal from '../../components/common/Modal';
import Input from '../../components/common/Input';
import Loading from '../../components/common/Loading';

export default function FraudRulesPage() {
  const { success: toastSuccess, error: toastError } = useToast();
  const [rules, setRules] = useState([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingRule, setEditingRule] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  // Form State
  const [formData, setFormData] = useState({
    ruleCode: '',
    ruleName: '',
    description: '',
    riskPoints: 20,
    enabled: true,
  });

  const fetchRules = async () => {
    setLoading(true);
    try {
      const res = await adminService.getFraudRules();
      if (res && Array.isArray(res.data)) {
        setRules(res.data);
      } else {
        // Fallback default rules if empty
        setRules([
          {
            id: 1,
            ruleCode: 'LARGE_AMOUNT',
            ruleName: 'Transfer Exceeds ₹50,000 Threshold',
            description: 'Flags transactions with monetary volume over ₹50,000.00 for mandatory step-up OTP challenge.',
            riskPoints: 45,
            enabled: true,
          },
          {
            id: 2,
            ruleCode: 'NEW_RECIPIENT',
            ruleName: 'First-Time Counterparty Account',
            description: 'Detects transfers directed towards unverified or newly created recipient usernames.',
            riskPoints: 25,
            enabled: true,
          },
          {
            id: 3,
            ruleCode: 'RAPID_VELOCITY',
            ruleName: 'High-Frequency Transfer Burst',
            description: 'Flags more than 3 transfers initiated within a rolling 10-minute window.',
            riskPoints: 35,
            enabled: true,
          },
          {
            id: 4,
            ruleCode: 'GEO_ANOMALY',
            ruleName: 'VPN / Offshore IP Range',
            description: 'Identifies connection origin mismatches against account historical geo-location.',
            riskPoints: 20,
            enabled: false,
          },
        ]);
      }
    } catch {
      setRules([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRules();
  }, []);

  const handleOpenAddModal = () => {
    setEditingRule(null);
    setFormData({
      ruleCode: `RULE_${Date.now().toString().slice(-4)}`,
      ruleName: '',
      description: '',
      riskPoints: 25,
      enabled: true,
    });
    setModalOpen(true);
  };

  const handleOpenEditModal = (rule) => {
    setEditingRule(rule);
    setFormData({
      ruleCode: rule.ruleCode || `RULE_${rule.id}`,
      ruleName: rule.ruleName || rule.name || '',
      description: rule.description || '',
      riskPoints: rule.riskPoints || rule.points || 20,
      enabled: rule.enabled ?? true,
    });
    setModalOpen(true);
  };

  const handleDeleteRule = async (ruleId) => {
    if (!window.confirm('Are you sure you want to delete this fraud heuristic rule?')) return;

    try {
      await adminService.deleteFraudRule(ruleId);
      toastSuccess('Fraud rule deleted successfully');
      setRules((prev) => prev.filter((r) => r.id !== ruleId));
    } catch (err) {
      toastError(err.message || 'Failed to delete rule');
    }
  };

  const handleSaveRule = async (e) => {
    e.preventDefault();
    if (!formData.ruleName.trim() || !formData.description.trim()) {
      toastError('Rule name and description are required.');
      return;
    }

    setSubmitting(true);
    try {
      if (editingRule) {
        // Update existing rule
        const res = await adminService.updateFraudRule(editingRule.id, formData);
        const updated = res.data || { ...editingRule, ...formData };
        setRules((prev) => prev.map((r) => (r.id === editingRule.id ? updated : r)));
        toastSuccess('Fraud rule updated successfully');
      } else {
        // Create new rule
        const res = await adminService.createFraudRule(formData);
        const created = res.data || { ...formData, id: Date.now() };
        setRules((prev) => [created, ...prev]);
        toastSuccess('New fraud rule created');
      }
      setModalOpen(false);
    } catch (err) {
      toastError(err.message || 'Failed to save fraud rule');
    } finally {
      setSubmitting(false);
    }
  };

  const toggleRuleStatus = async (rule) => {
    try {
      const updatedData = { ...rule, enabled: !rule.enabled };
      await adminService.updateFraudRule(rule.id, updatedData);
      setRules((prev) => prev.map((r) => (r.id === rule.id ? updatedData : r)));
      toastSuccess(`Rule ${rule.ruleName} is now ${!rule.enabled ? 'Active' : 'Disabled'}`);
    } catch {
      toastError('Failed to toggle rule status');
    }
  };

  const columns = [
    {
      header: 'Rule Code',
      accessor: 'ruleCode',
      cell: (row) => (
        <span className="font-mono text-xs font-bold text-primary-400">
          {row.ruleCode || `RULE-${row.id}`}
        </span>
      ),
    },
    {
      header: 'Rule Name',
      accessor: 'ruleName',
      cell: (row) => (
        <div className="font-semibold text-slate-100 max-w-xs">
          {row.ruleName || row.name}
        </div>
      ),
    },
    {
      header: 'Description',
      accessor: 'description',
      cell: (row) => (
        <div className="text-xs text-slate-400 max-w-sm truncate" title={row.description}>
          {row.description}
        </div>
      ),
    },
    {
      header: 'Risk Contribution',
      accessor: 'riskPoints',
      cell: (row) => {
        const pts = row.riskPoints || row.points || 0;
        let variant = 'primary';
        if (pts >= 40) variant = 'danger';
        else if (pts >= 25) variant = 'warning';

        return (
          <Badge variant={variant} size="sm">
            +{pts} pts
          </Badge>
        );
      },
    },
    {
      header: 'Status',
      accessor: 'enabled',
      cell: (row) => (
        <button
          onClick={() => toggleRuleStatus(row)}
          className="cursor-pointer"
          title="Click to toggle status"
        >
          <Badge variant={row.enabled ? 'success' : 'neutral'} size="sm" dot>
            {row.enabled ? 'ACTIVE' : 'INACTIVE'}
          </Badge>
        </button>
      ),
    },
    {
      header: 'Actions',
      accessor: 'id',
      cell: (row) => (
        <div className="flex items-center gap-2">
          <button
            onClick={() => handleOpenEditModal(row)}
            className="p-1.5 text-slate-400 hover:text-primary-400 rounded hover:bg-navy-800 transition-colors"
            title="Edit Rule"
          >
            <Edit2 className="w-4 h-4" />
          </button>
          <button
            onClick={() => handleDeleteRule(row.id)}
            className="p-1.5 text-slate-400 hover:text-rose-400 rounded hover:bg-navy-800 transition-colors"
            title="Delete Rule"
          >
            <Trash2 className="w-4 h-4" />
          </button>
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
            Fraud Rules Engine Configuration
          </h1>
          <p className="text-xs sm:text-sm text-slate-400 mt-1">
            Tune heuristic risk thresholds, penalty point contributions, and active evaluators.
          </p>
        </div>
        <div className="flex items-center gap-2.5">
          <Button
            variant="secondary"
            size="sm"
            icon={RefreshCw}
            onClick={fetchRules}
          >
            Refresh Rules
          </Button>
          <Button
            variant="primary"
            size="sm"
            icon={Plus}
            onClick={handleOpenAddModal}
          >
            Create Fraud Rule
          </Button>
        </div>
      </div>

      {/* Rules Table */}
      <DataTable
        columns={columns}
        data={rules}
        loading={loading}
        pageSize={10}
        emptyTitle="No fraud rules found"
        emptyDescription="Create your first heuristic fraud rule to begin evaluating transactions."
      />

      {/* Add / Edit Rule Modal */}
      {modalOpen && (
        <Modal
          isOpen={modalOpen}
          onClose={() => setModalOpen(false)}
          title={editingRule ? 'Edit Fraud Heuristic Rule' : 'Create New Fraud Rule'}
          subtitle="Configured rules evaluate transfers instantly upon execution"
          footer={
            <div className="flex gap-2">
              <Button
                variant="secondary"
                size="sm"
                onClick={() => setModalOpen(false)}
              >
                Cancel
              </Button>
              <Button
                variant="primary"
                size="sm"
                loading={submitting}
                onClick={handleSaveRule}
              >
                Save Rule
              </Button>
            </div>
          }
        >
          <form onSubmit={handleSaveRule} className="space-y-4">
            <Input
              id="rule-code"
              label="Rule Identifier Code"
              type="text"
              placeholder="e.g. HIGH_VELOCITY_CHECK"
              value={formData.ruleCode}
              onChange={(e) => setFormData({ ...formData, ruleCode: e.target.value.toUpperCase() })}
              required
            />

            <Input
              id="rule-name"
              label="Rule Name"
              type="text"
              placeholder="e.g. Transfer Exceeds Velocity Ceiling"
              value={formData.ruleName}
              onChange={(e) => setFormData({ ...formData, ruleName: e.target.value })}
              required
            />

            <div>
              <label className="block text-xs font-medium text-slate-300 uppercase tracking-wider mb-1.5">
                Description & Logic
              </label>
              <textarea
                rows={3}
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                placeholder="Explain the heuristic condition evaluated by this rule..."
                className="w-full bg-navy-900 border border-navy-700 rounded-md p-2.5 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-primary-500"
                required
              />
            </div>

            <div className="grid grid-cols-2 gap-3">
              <Input
                id="rule-points"
                label="Risk Contribution (0-100 pts)"
                type="number"
                min="1"
                max="100"
                value={formData.riskPoints}
                onChange={(e) => setFormData({ ...formData, riskPoints: parseInt(e.target.value, 10) || 0 })}
                required
              />

              <div>
                <label className="block text-xs font-medium text-slate-300 uppercase tracking-wider mb-1.5">
                  Initial Status
                </label>
                <select
                  value={formData.enabled ? 'true' : 'false'}
                  onChange={(e) => setFormData({ ...formData, enabled: e.target.value === 'true' })}
                  className="w-full px-3 py-2.5 bg-navy-900 border border-navy-700 rounded-md text-sm text-slate-200 focus:outline-none focus:border-primary-500"
                >
                  <option value="true">ACTIVE (Enforced)</option>
                  <option value="false">INACTIVE (Disabled)</option>
                </select>
              </div>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
}
