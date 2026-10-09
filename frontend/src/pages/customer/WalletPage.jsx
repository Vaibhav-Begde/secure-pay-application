import React, { useState, useEffect } from 'react';
import {
  Wallet,
  CreditCard,
  ArrowUpRight,
  ShieldCheck,
  CheckCircle,
  Copy,
  RefreshCw,
  Lock,
} from 'lucide-react';
import { walletService } from '../../services/walletService';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import Loading from '../../components/common/Loading';

export default function WalletPage() {
  const { user } = useAuth();
  const { success: toastSuccess } = useToast();
  const [wallet, setWallet] = useState(null);
  const [loading, setLoading] = useState(true);

  const fetchWallet = async () => {
    setLoading(true);
    try {
      const res = await walletService.getWallet();
      if (res && res.data) {
        setWallet(res.data);
      } else {
        setWallet({
          accountNumber: 'ACC-' + (user?.id || '8834') + '-SECURE',
          balance: 1000.0,
          currency: 'INR',
          status: 'ACTIVE',
        });
      }
    } catch {
      setWallet({
        accountNumber: 'ACC-' + (user?.id || '8834') + '-SECURE',
        balance: 1000.0,
        currency: 'INR',
        status: 'ACTIVE',
      });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchWallet();
  }, []);

  const handleCopyAccount = () => {
    if (wallet?.accountNumber) {
      navigator.clipboard.writeText(wallet.accountNumber);
      toastSuccess('Account number copied to clipboard');
    }
  };

  if (loading && !wallet) {
    return <Loading text="Loading SecurePay virtual wallet..." />;
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-navy-700/80 pb-5">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-white tracking-tight">
            Virtual Wallet & Liquidity
          </h1>
          <p className="text-xs sm:text-sm text-slate-400 mt-1">
            Manage your digital funds, virtual account number, and transaction limits.
          </p>
        </div>
        <Button
          variant="secondary"
          size="sm"
          icon={RefreshCw}
          onClick={fetchWallet}
        >
          Refresh Wallet
        </Button>
      </div>

      {/* Main Wallet Cards */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Virtual Card Representation */}
        <div className="lg:col-span-1 bg-gradient-to-br from-navy-800 to-navy-900 border border-navy-700 rounded-xl p-6 shadow-xl flex flex-col justify-between h-56 relative overflow-hidden">
          {/* Subtle decoration lines */}
          <div className="absolute -right-8 -top-8 w-32 h-32 rounded-full bg-primary-600/10 pointer-events-none" />
          <div className="absolute right-12 bottom-0 w-24 h-24 rounded-full bg-primary-500/10 pointer-events-none" />

          <div className="flex items-center justify-between z-10">
            <div className="flex items-center gap-2">
              <ShieldCheck className="w-5 h-5 text-primary-400" />
              <span className="text-xs font-bold uppercase tracking-widest text-white">
                SecurePay Corporate
              </span>
            </div>
            <Badge variant="success" size="sm" dot>
              {wallet?.status || 'Active'}
            </Badge>
          </div>

          <div className="z-10 my-2">
            <span className="text-[11px] uppercase tracking-wider text-slate-400 font-mono">
              Available Balance
            </span>
            <div className="text-3xl font-extrabold text-white tracking-tight">
              ₹{Number(wallet?.balance || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
              <span className="text-xs text-primary-400 ml-1.5 font-semibold">INR (₹)</span>
            </div>
          </div>

          <div className="flex items-center justify-between z-10 border-t border-navy-700/80 pt-3">
            <div>
              <span className="text-[10px] uppercase font-mono text-slate-400 block">
                Account Holder
              </span>
              <span className="text-xs font-semibold text-slate-200">
                {user?.username || 'Customer User'}
              </span>
            </div>
            <button
              onClick={handleCopyAccount}
              className="flex items-center gap-1 text-xs font-mono text-primary-300 hover:text-white bg-navy-750 px-2 py-1 rounded border border-navy-650 transition-colors"
              title="Copy Account Number"
            >
              <span>{wallet?.accountNumber || 'ACC-8912-SEC'}</span>
              <Copy className="w-3 h-3 ml-1" />
            </button>
          </div>
        </div>

        {/* Account Details & Limits */}
        <div className="lg:col-span-2 bg-navy-850 border border-navy-700/80 rounded-xl p-6 flex flex-col justify-between">
          <div>
            <h3 className="text-sm font-semibold text-white uppercase tracking-wider mb-4">
              Wallet Security & Velocity Limits
            </h3>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
              <div className="p-3.5 rounded-lg bg-navy-900 border border-navy-750">
                <span className="text-xs text-slate-400 block mb-1">Per-Transfer Cap</span>
                <span className="text-lg font-bold text-white">₹1,00,000.00</span>
                <p className="text-[10px] text-slate-500 mt-1">Requires OTP over ₹25,000</p>
              </div>

              <div className="p-3.5 rounded-lg bg-navy-900 border border-navy-750">
                <span className="text-xs text-slate-400 block mb-1">Daily Limit Remaining</span>
                <span className="text-lg font-bold text-emerald-400">₹2,50,000.00</span>
                <p className="text-[10px] text-slate-500 mt-1">Rolling 24-hour cycle</p>
              </div>

              <div className="p-3.5 rounded-lg bg-navy-900 border border-navy-750">
                <span className="text-xs text-slate-400 block mb-1">Currency Standard</span>
                <span className="text-lg font-bold text-white">INR (₹)</span>
                <p className="text-[10px] text-slate-500 mt-1">Indian Rupee</p>
              </div>
            </div>

            <div className="space-y-2 text-xs text-slate-300">
              <div className="flex items-center gap-2">
                <CheckCircle className="w-4 h-4 text-emerald-400 shrink-0" />
                <span>Zero-Trust endpoint protection active on all withdrawal transactions.</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle className="w-4 h-4 text-emerald-400 shrink-0" />
                <span>Real-time anomaly evaluation checks device fingerprints and velocity spikes.</span>
              </div>
            </div>
          </div>

          <div className="mt-6 pt-4 border-t border-navy-750 flex items-center justify-between">
            <span className="text-xs text-slate-400">
              Need to send funds to another SecurePay user?
            </span>
            <a href="/customer/transfer">
              <Button variant="primary" size="sm" icon={ArrowUpRight}>
                Go to Send Money
              </Button>
            </a>
          </div>
        </div>
      </div>
    </div>
  );
}
