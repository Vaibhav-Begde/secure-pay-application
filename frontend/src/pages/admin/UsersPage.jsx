import React, { useState, useEffect } from 'react';
import {
  Users,
  Search,
  Shield,
  UserCheck,
  UserX,
  Edit,
  RefreshCw,
  Eye,
} from 'lucide-react';
import { adminService } from '../../services/adminService';
import { useToast } from '../../context/ToastContext';
import DataTable from '../../components/common/DataTable';
import Badge from '../../components/common/Badge';
import Button from '../../components/common/Button';
import Modal from '../../components/common/Modal';
import Loading from '../../components/common/Loading';

export default function UsersPage() {
  const { success: toastSuccess, error: toastError } = useToast();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [editingUser, setEditingUser] = useState(null);
  const [selectedRole, setSelectedRole] = useState('CUSTOMER');
  const [selectedStatus, setSelectedStatus] = useState('ACTIVE');
  const [saving, setSaving] = useState(false);

  const fetchUsers = async () => {
    setLoading(true);
    try {
      const res = await adminService.getUsers();
      if (res && Array.isArray(res.data)) {
        setUsers(res.data);
      }
    } catch {
      toastError('Failed to fetch user directory');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  const handleOpenEdit = (user) => {
    setEditingUser(user);
    setSelectedRole(user.role);
    setSelectedStatus(user.status);
  };

  const handleSaveUser = async () => {
    if (!editingUser) return;
    setSaving(true);
    try {
      await adminService.updateUser(editingUser.id, {
        role: selectedRole,
        status: selectedStatus,
      });
      toastSuccess(`Updated profile for ${editingUser.username}`);
      setUsers((prev) =>
        prev.map((u) =>
          u.id === editingUser.id
            ? { ...u, role: selectedRole, status: selectedStatus }
            : u
        )
      );
      setEditingUser(null);
    } catch {
      toastError('Failed to update user status');
    } finally {
      setSaving(false);
    }
  };

  const handleToggleStatus = async (user) => {
    const newStatus = user.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE';
    try {
      await adminService.updateUser(user.id, { status: newStatus });
      toastSuccess(`User ${user.username} is now ${newStatus}`);
      setUsers((prev) =>
        prev.map((u) => (u.id === user.id ? { ...u, status: newStatus } : u))
      );
    } catch {
      toastError('Failed to toggle status');
    }
  };

  const filteredUsers = users.filter((u) => {
    const query = searchTerm.toLowerCase();
    return (
      !query ||
      u.username.toLowerCase().includes(query) ||
      u.email.toLowerCase().includes(query) ||
      u.role.toLowerCase().includes(query)
    );
  });

  const columns = [
    {
      header: 'User Entity',
      accessor: 'username',
      cell: (row) => (
        <div className="flex items-center gap-2.5">
          <div className="w-7 h-7 rounded-full bg-navy-800 border border-navy-700 flex items-center justify-center font-bold text-xs text-primary-300">
            {row.username.substring(0, 2).toUpperCase()}
          </div>
          <div>
            <span className="font-semibold text-slate-100 block">{row.username}</span>
            <span className="text-[10px] text-slate-500 font-mono">ID #{row.id}</span>
          </div>
        </div>
      ),
    },
    {
      header: 'Corporate Email',
      accessor: 'email',
      cell: (row) => <span className="text-slate-300 text-xs">{row.email}</span>,
    },
    {
      header: 'Assigned Role',
      accessor: 'role',
      cell: (row) => {
        let variant = 'primary';
        if (row.role === 'ADMIN') variant = 'danger';
        else if (row.role === 'FRAUD_ANALYST') variant = 'warning';

        return (
          <Badge variant={variant} size="sm">
            {row.role}
          </Badge>
        );
      },
    },
    {
      header: 'Account Status',
      accessor: 'status',
      cell: (row) => (
        <Badge
          variant={row.status === 'ACTIVE' ? 'success' : 'danger'}
          size="sm"
          dot
        >
          {row.status}
        </Badge>
      ),
    },
    {
      header: 'Created Date',
      accessor: 'createdAt',
      cell: (row) => (
        <span className="text-xs text-slate-400">
          {new Date(row.createdAt).toLocaleDateString()}
        </span>
      ),
    },
    {
      header: 'Actions',
      accessor: 'id',
      cell: (row) => (
        <div className="flex items-center gap-1.5">
          <button
            onClick={() => handleOpenEdit(row)}
            className="p-1.5 text-slate-400 hover:text-primary-300 rounded hover:bg-navy-800 transition-colors"
            title="Edit Role / Status"
          >
            <Edit className="w-3.5 h-3.5" />
          </button>
          <button
            onClick={() => handleToggleStatus(row)}
            className={`p-1.5 rounded hover:bg-navy-800 transition-colors ${
              row.status === 'ACTIVE'
                ? 'text-slate-400 hover:text-rose-400'
                : 'text-slate-400 hover:text-emerald-400'
            }`}
            title={row.status === 'ACTIVE' ? 'Suspend Account' : 'Activate Account'}
          >
            {row.status === 'ACTIVE' ? (
              <UserX className="w-3.5 h-3.5" />
            ) : (
              <UserCheck className="w-3.5 h-3.5" />
            )}
          </button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-navy-700/80 pb-5">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-white tracking-tight">
            User Directory & Access Controls
          </h1>
          <p className="text-xs sm:text-sm text-slate-400 mt-1">
            Manage enterprise role assignments, MFA requirements, and security account statuses.
          </p>
        </div>
        <Button
          variant="secondary"
          size="sm"
          icon={RefreshCw}
          onClick={fetchUsers}
        >
          Refresh Directory
        </Button>
      </div>

      {/* Search Input */}
      <div className="bg-navy-850 border border-navy-700/80 rounded-lg p-3 max-w-md">
        <div className="relative">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
          <input
            type="text"
            placeholder="Search users by name, email, or role..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-9 pr-3 py-1.5 bg-navy-900 border border-navy-700 rounded-md text-xs sm:text-sm text-white placeholder-slate-500 focus:outline-none focus:border-primary-500"
          />
        </div>
      </div>

      {/* Users Data Table */}
      <DataTable
        columns={columns}
        data={filteredUsers}
        loading={loading}
        pageSize={10}
        emptyTitle="No users found"
        emptyDescription="No registered users match your search criteria."
      />

      {/* Edit Role / Status Modal */}
      {editingUser && (
        <Modal
          isOpen={!!editingUser}
          onClose={() => setEditingUser(null)}
          title={`Edit Permissions: ${editingUser.username}`}
          subtitle={`User ID #${editingUser.id} • ${editingUser.email}`}
          footer={
            <div className="flex gap-2">
              <Button variant="secondary" size="sm" onClick={() => setEditingUser(null)}>
                Cancel
              </Button>
              <Button variant="primary" size="sm" loading={saving} onClick={handleSaveUser}>
                Save Changes
              </Button>
            </div>
          }
        >
          <div className="space-y-4 text-xs">
            <div>
              <label className="block text-slate-300 font-medium uppercase tracking-wider mb-1.5">
                Security Role Clearance
              </label>
              <select
                value={selectedRole}
                onChange={(e) => setSelectedRole(e.target.value)}
                className="w-full px-3 py-2.5 bg-navy-900 border border-navy-700 rounded-md text-sm text-slate-200 focus:outline-none focus:border-primary-500"
              >
                <option value="CUSTOMER">CUSTOMER (Standard User)</option>
                <option value="FRAUD_ANALYST">FRAUD_ANALYST (Risk Monitoring)</option>
                <option value="ADMIN">ADMIN (Superuser / Security Controller)</option>
              </select>
            </div>

            <div>
              <label className="block text-slate-300 font-medium uppercase tracking-wider mb-1.5">
                Account Status
              </label>
              <select
                value={selectedStatus}
                onChange={(e) => setSelectedStatus(e.target.value)}
                className="w-full px-3 py-2.5 bg-navy-900 border border-navy-700 rounded-md text-sm text-slate-200 focus:outline-none focus:border-primary-500"
              >
                <option value="ACTIVE">ACTIVE (Normal Access)</option>
                <option value="SUSPENDED">SUSPENDED (Transfer Freezes Enforced)</option>
              </select>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}
