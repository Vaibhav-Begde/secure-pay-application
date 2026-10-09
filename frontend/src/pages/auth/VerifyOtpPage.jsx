import React, { useState } from 'react';
import { useLocation, useNavigate, Link } from 'react-router-dom';
import { ShieldAlert, KeyRound, ArrowRight, RefreshCw, CheckCircle2 } from 'lucide-react';
import { authService } from '../../services/authService';
import { useToast } from '../../context/ToastContext';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import Toast from '../../components/common/Toast';

export default function VerifyOtpPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const { success: toastSuccess, error: toastError } = useToast();

  const queryParams = new URLSearchParams(location.search);
  const initialRefCode = location.state?.referenceCode || queryParams.get('ref') || '';

  const [referenceCode, setReferenceCode] = useState(initialRefCode);
  const [otpCode, setOtpCode] = useState('');
  const [loading, setLoading] = useState(false);
  const [resending, setResending] = useState(false);
  const [formError, setFormError] = useState('');
  const [verifiedSuccess, setVerifiedSuccess] = useState(false);

  const handleVerify = async (e) => {
    e.preventDefault();
    setFormError('');

    if (!referenceCode.trim() || !otpCode.trim()) {
      setFormError('Please provide both the transaction reference code and the 6-digit OTP.');
      return;
    }

    setLoading(true);
    try {
      await authService.verifyOtp({ referenceCode, otpCode });
      setVerifiedSuccess(true);
      toastSuccess('Step-Up OTP verified! Transaction authorized.');
      setTimeout(() => {
        navigate('/customer/transactions');
      }, 1500);
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'OTP verification failed. Check code or request a new one.';
      setFormError(msg);
      toastError(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleResend = async () => {
    if (!referenceCode.trim()) {
      setFormError('Transaction reference code is required to generate OTP.');
      return;
    }
    setResending(true);
    setFormError('');
    try {
      await authService.sendOtp({ referenceCode });
      toastSuccess('A new verification code was sent to your registered email.');
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Failed to resend OTP.';
      setFormError(msg);
      toastError(msg);
    } finally {
      setResending(false);
    }
  };

  return (
    <div className="min-h-screen bg-navy-950 flex flex-col justify-center py-12 px-4 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md text-center">
        <div className="w-12 h-12 rounded-xl bg-amber-500/20 border border-amber-500/40 text-amber-400 flex items-center justify-center mx-auto mb-4 shadow-lg">
          <ShieldAlert className="w-7 h-7" />
        </div>
        <h2 className="text-2xl font-bold tracking-tight text-white">
          Step-Up Verification
        </h2>
        <p className="mt-1.5 text-xs text-slate-400 uppercase tracking-wider font-mono">
          Multi-Factor Challenge For High-Risk Transaction
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-navy-900 border border-navy-700/90 py-8 px-6 shadow-2xl rounded-xl sm:px-10">
          {verifiedSuccess ? (
            <div className="text-center py-6">
              <CheckCircle2 className="w-12 h-12 text-emerald-400 mx-auto mb-3 animate-bounce" />
              <h3 className="text-lg font-bold text-white mb-1">Transfer Authorized!</h3>
              <p className="text-xs text-slate-400 mb-4">Redirecting to transaction history...</p>
            </div>
          ) : (
            <>
              {formError && (
                <div className="mb-5">
                  <Toast type="error" message={formError} />
                </div>
              )}

              <div className="mb-5 p-3 rounded-lg bg-navy-850 border border-navy-750 text-xs text-slate-300">
                <span className="font-semibold text-primary-400">Security Requirement:</span>{' '}
                Our realtime fraud engine flagged this transfer for elevated risk. Please enter the one-time code sent to your registered email.
              </div>

              <form onSubmit={handleVerify} className="space-y-4">
                <Input
                  id="otp-ref"
                  label="Transaction Reference"
                  type="text"
                  placeholder="e.g. TXN-1726678912"
                  value={referenceCode}
                  onChange={(e) => setReferenceCode(e.target.value)}
                  required
                />

                <Input
                  id="otp-code"
                  label="6-Digit Verification Code"
                  type="text"
                  maxLength={6}
                  placeholder="Enter code from email"
                  icon={KeyRound}
                  value={otpCode}
                  onChange={(e) => setOtpCode(e.target.value.trim())}
                  required
                />

                <div className="pt-2">
                  <Button
                    type="submit"
                    variant="primary"
                    size="lg"
                    loading={loading}
                    icon={ArrowRight}
                    className="w-full justify-center"
                  >
                    Confirm & Release Transfer
                  </Button>
                </div>
              </form>

              <div className="mt-5 flex items-center justify-between pt-4 border-t border-navy-750 text-xs">
                <button
                  type="button"
                  onClick={handleResend}
                  disabled={resending}
                  className="text-primary-400 hover:text-primary-300 flex items-center gap-1.5 transition-colors disabled:opacity-50"
                >
                  <RefreshCw className={`w-3.5 h-3.5 ${resending ? 'animate-spin' : ''}`} />
                  <span>Resend code by email</span>
                </button>

                <Link
                  to="/customer/dashboard"
                  className="text-slate-400 hover:text-slate-200"
                >
                  Cancel Transfer
                </Link>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
