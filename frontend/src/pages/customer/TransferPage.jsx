import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Send,
  User,
  IndianRupee,
  FileText,
  ShieldAlert,
  CheckCircle2,
  ArrowRight,
  ArrowLeft,
  KeyRound,
  Eye,
  EyeOff,
  AlertTriangle,
  RefreshCw,
} from 'lucide-react';
import { transactionService } from '../../services/transactionService';
import { authService } from '../../services/authService';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import RiskScore from '../../components/common/RiskScore';
import RiskBadge from '../../components/common/RiskBadge';
import Toast from '../../components/common/Toast';

export default function TransferPage() {
  const { user, refreshCurrentUser } = useAuth();
  const { success: toastSuccess, error: toastError } = useToast();

  // Multi-step states: 1: Enter Details, 2: Review, 3: Fraud Risk Check, 4: OTP Challenge (if required), 5: Success
  const [step, setStep] = useState(1);

  // Form Fields
  const [receiver, setReceiver] = useState('');
  const [amount, setAmount] = useState('');
  const [note, setNote] = useState('');
  const [transactionPin, setTransactionPin] = useState('');
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPin, setNewPin] = useState('');
  const [confirmPin, setConfirmPin] = useState('');
  const [showCurrentPassword, setShowCurrentPassword] = useState(false);
  const [showNewPin, setShowNewPin] = useState(false);
  const [showConfirmPin, setShowConfirmPin] = useState(false);
  const [showTransferPin, setShowTransferPin] = useState(false);
  const [savingPin, setSavingPin] = useState(false);

  // Risk evaluation state
  const [evaluatingRisk, setEvaluatingRisk] = useState(false);
  const [riskAssessment, setRiskAssessment] = useState(null);

  // Transfer execution state
  const [submitting, setSubmitting] = useState(false);
  const [transferResult, setTransferResult] = useState(null);

  // OTP step state
  const [otpCode, setOtpCode] = useState('');
  const [verifyingOtp, setVerifyingOtp] = useState(false);
  const [resendingOtp, setResendingOtp] = useState(false);
  const [stepError, setStepError] = useState('');

  const handleSetPin = async (e) => {
    e.preventDefault();
    setStepError('');
    if (newPin !== confirmPin) {
      setStepError('The transaction PIN entries do not match.');
      return;
    }
    setSavingPin(true);
    try {
      await authService.setTransactionPin({ currentPassword, transactionPin: newPin });
      await refreshCurrentUser();
      setCurrentPassword('');
      setNewPin('');
      setConfirmPin('');
      setShowCurrentPassword(false);
      setShowNewPin(false);
      setShowConfirmPin(false);
      toastSuccess('Transaction PIN saved. You will need it for every transfer.');
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Could not save the transaction PIN.';
      setStepError(msg);
      toastError(msg);
    } finally {
      setSavingPin(false);
    }
  };

  // Step 1 Validation & Proceed to Review
  const handleProceedToReview = (e) => {
    e.preventDefault();
    setStepError('');

    const numAmount = parseFloat(amount);
    if (!receiver.trim()) {
      setStepError('Please enter a recipient username or email.');
      return;
    }
    if (isNaN(numAmount) || numAmount <= 0) {
      setStepError('Transfer amount must be greater than ₹0.00.');
      return;
    }
    if (receiver.trim().toLowerCase() === (user?.username || '').toLowerCase()) {
      setStepError('You cannot transfer funds to your own username.');
      return;
    }

    setStep(2);
  };

  // Step 2 to Step 3: Run Realtime Fraud Risk Check
  const handleRunRiskCheck = async () => {
    setStep(3);
    setEvaluatingRisk(true);
    setStepError('');

    const numAmount = parseFloat(amount);
    try {
      const payload = {
        receiverUsernameOrEmail: receiver.trim(),
        amount: numAmount,
        description: note.trim() || 'Direct Transfer via SecurePay',
        deviceId: 'Web-Browser-Chrome',
        ipAddress: '127.0.0.1',
        location: 'New York, USA',
      };

      const res = await transactionService.evaluate(payload);
      const evalData = res?.data;
      if (evalData) {
        setRiskAssessment({
          score: evalData.riskScore,
          level: evalData.riskLevel,
          factors: evalData.riskReasons && evalData.riskReasons.length > 0
            ? evalData.riskReasons
            : ['Standard velocity profile', 'Verified sender account'],
          decision: evalData.decision,
        });
      }
    } catch (err) {
      console.warn('Real-time risk evaluation API error, using local heuristic:', err);
      let score = 15;
      let level = 'LOW';
      let factors = ['Standard velocity profile', 'Verified sender account'];

      if (numAmount >= 50000) {
        score = 85;
        level = 'HIGH';
        factors = [
          'High transfer amount exceeds ₹50,000.00 safety threshold',
          'Multi-factor OTP verification step-up required before release',
        ];
      } else if (numAmount >= 10000) {
        score = 52;
        level = 'MEDIUM';
        factors = [
          'Transfer amount exceeds ₹10,000.00 tier threshold',
          'Step-up OTP verification required',
        ];
      }
      setRiskAssessment({ score, level, factors });
    } finally {
      setEvaluatingRisk(false);
    }
  };

  // Step 3: Execute Transfer via Backend API
  const handleExecuteTransfer = async () => {
    if (!/^\d{6}$/.test(transactionPin)) {
      setStepError('Enter your 6-digit transaction PIN to continue.');
      return;
    }
    setSubmitting(true);
    setStepError('');

    try {
      const payload = {
        receiverUsernameOrEmail: receiver.trim(),
        amount: parseFloat(amount),
        transactionPin,
        description: note.trim() || 'Direct Transfer via SecurePay',
        deviceId: 'Web-Browser-Chrome',
        ipAddress: '127.0.0.1',
        location: 'New York, USA',
      };

      const res = await transactionService.transfer(payload);
      const data = res?.data;
      if (!data?.referenceCode || !data?.status) {
        throw new Error('The transfer service returned an incomplete response. No transfer status is available.');
      }

      setTransferResult(data);
      setTransactionPin('');
      setShowTransferPin(false);

      // Backend returns VERIFICATION_REQUIRED or OTP_REQUIRED for MEDIUM risk,
      // and BLOCKED / VERIFICATION_REQUIRED for HIGH risk (with OTP generated).
      // COMPLETED means LOW risk — transfer went through immediately without OTP.
      if (
        data.status === 'VERIFICATION_REQUIRED' ||
        data.status === 'OTP_REQUIRED' ||
        data.status === 'PENDING' ||
        data.status === 'BLOCKED' ||
        data.riskLevel === 'HIGH' ||
        data.riskLevel === 'MEDIUM'
      ) {
        setStep(4);
        toastSuccess('A verification code was sent to your registered email.');
      } else {
        // COMPLETED — LOW risk, balance already updated on backend
        setStep(5);
        toastSuccess('Transfer completed successfully! Balance has been updated.');
      }
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Transfer failed. Please try again.';
      setStepError(msg);
      toastError(msg);
    } finally {
      setSubmitting(false);
      setTransactionPin('');
    }
  };

  // Step 4: Verify Step-Up OTP
  const handleVerifyOtp = async (e) => {
    e.preventDefault();
    setStepError('');

    if (!otpCode.trim() || otpCode.trim().length < 4) {
      setStepError('Please enter a valid OTP code.');
      return;
    }

    setVerifyingOtp(true);
    try {
      const res = await authService.verifyOtp({
        referenceCode: transferResult?.referenceCode || '',
        otpCode: otpCode.trim(),
      });
      const updatedTx = res?.data;
      if (updatedTx) {
        setTransferResult(updatedTx);
      }
      toastSuccess('Step-Up OTP verified successfully!');
      
      setStep(5);
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'OTP verification failed.';
      setStepError(msg);
      toastError(msg);
    } finally {
      setVerifyingOtp(false);
    }
  };

  // Resend OTP in Step 4
  const handleResendOtp = async () => {
    if (!transferResult?.referenceCode) return;
    setResendingOtp(true);
    try {
      await authService.sendOtp({ referenceCode: transferResult.referenceCode });
      toastSuccess('A new verification code was sent to your registered email.');
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Failed to resend the verification email.';
      setStepError(msg);
      toastError(msg);
    } finally {
      setResendingOtp(false);
    }
  };

  const getFactorPointInfo = (factor) => {
    if (!factor) return { label: '', points: null };
    const match = factor.match(/\(\+(\d+)\s*pts?\)/i);
    if (match) {
      const clean = factor.replace(/\s*\(\+\d+\s*pts?\)/i, '').trim();
      return { label: clean, points: parseInt(match[1], 10) };
    }
    const lower = factor.toLowerCase();
    if (lower.includes('50,000') || lower.includes('safety threshold')) {
      return { label: factor, points: 65 };
    }
    if (lower.includes('10,000') || lower.includes('tier threshold')) {
      return { label: factor, points: 40 };
    }
    if (lower.includes('unusually high') || lower.includes('higher than average')) {
      return { label: factor, points: 20 };
    }
    if (lower.includes('previous fraud') || lower.includes('fraud history')) {
      return { label: factor, points: 30 };
    }
    if (lower.includes('rapid') || lower.includes('velocity')) {
      return { label: factor, points: 20 };
    }
    if (lower.includes('new device') || lower.includes('unrecognized device')) {
      return { label: factor, points: 20 };
    }
    if (lower.includes('new receiver') || lower.includes('first-time')) {
      return { label: factor, points: 15 };
    }
    if (lower.includes('new location') || lower.includes('geographical')) {
      return { label: factor, points: 15 };
    }
    if (lower.includes('unusual') && (lower.includes('time') || lower.includes('hour'))) {
      return { label: factor, points: 10 };
    }
    if (lower.includes('standard velocity') || lower.includes('verified sender')) {
      return { label: factor, points: 0 };
    }
    return { label: factor, points: null };
  };

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      {/* Header */}
      <div className="border-b border-navy-700/80 pb-4">
        <h1 className="text-xl sm:text-2xl font-bold text-white tracking-tight">
          Send Money
        </h1>
        <p className="text-xs sm:text-sm text-slate-400 mt-1">
          End-to-end encrypted transfer with real-time heuristic fraud detection.
        </p>
      </div>

      {!user?.transactionPinSet ? (
        <div className="bg-navy-850 border border-navy-700/80 rounded-xl p-6 shadow-sm">
          <h2 className="text-lg font-semibold text-white">Set a transaction PIN</h2>
          <p className="mt-1 mb-5 text-sm text-slate-400">
            Create a 6-digit PIN. You’ll enter it for every transfer. Confirm your account password to set or reset it.
          </p>
          {stepError && <Toast type="error" message={stepError} />}
          <form onSubmit={handleSetPin} className="space-y-4">
            <Input
              id="transaction-pin-account-password"
              label="Current Account Password"
              type={showCurrentPassword ? 'text' : 'password'}
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
              endAdornment={
                <button
                  type="button"
                  onClick={() => setShowCurrentPassword((visible) => !visible)}
                  aria-label={showCurrentPassword ? 'Hide current account password' : 'Show current account password'}
                  className="text-slate-400 hover:text-slate-200 p-1"
                >
                  {showCurrentPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              }
              required
            />
            <Input
              id="transaction-pin-new"
              label="New 6-Digit Transaction PIN"
              type={showNewPin ? 'text' : 'password'}
              inputMode="numeric"
              maxLength={6}
              value={newPin}
              onChange={(e) => setNewPin(e.target.value.replace(/\D/g, '').slice(0, 6))}
              endAdornment={
                <button
                  type="button"
                  onClick={() => setShowNewPin((visible) => !visible)}
                  aria-label={showNewPin ? 'Hide new transaction PIN' : 'Show new transaction PIN'}
                  className="text-slate-400 hover:text-slate-200 p-1"
                >
                  {showNewPin ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              }
              required
            />
            <Input
              id="transaction-pin-confirm"
              label="Confirm Transaction PIN"
              type={showConfirmPin ? 'text' : 'password'}
              inputMode="numeric"
              maxLength={6}
              value={confirmPin}
              onChange={(e) => setConfirmPin(e.target.value.replace(/\D/g, '').slice(0, 6))}
              endAdornment={
                <button
                  type="button"
                  onClick={() => setShowConfirmPin((visible) => !visible)}
                  aria-label={showConfirmPin ? 'Hide confirmation PIN' : 'Show confirmation PIN'}
                  className="text-slate-400 hover:text-slate-200 p-1"
                >
                  {showConfirmPin ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              }
              required
            />
            <Button type="submit" variant="primary" loading={savingPin}>
              Save Transaction PIN
            </Button>
          </form>
        </div>
      ) : (
        <>
      {/* Stepper Bar */}
      <div className="flex items-center justify-between px-2 py-3 bg-navy-900 border border-navy-750 rounded-lg text-xs font-medium">
        <span className={step >= 1 ? 'text-primary-400 font-bold' : 'text-slate-500'}>
          1. Details
        </span>
        <span className="text-slate-600">→</span>
        <span className={step >= 2 ? 'text-primary-400 font-bold' : 'text-slate-500'}>
          2. Review
        </span>
        <span className="text-slate-600">→</span>
        <span className={step >= 3 ? 'text-primary-400 font-bold' : 'text-slate-500'}>
          3. Risk Check
        </span>
        <span className="text-slate-600">→</span>
        <span className={step >= 4 ? 'text-primary-400 font-bold' : 'text-slate-500'}>
          4. Confirm
        </span>
      </div>

      {stepError && <Toast type="error" message={stepError} />}

      {/* STEP 1: Enter Details */}
      {step === 1 && (
        <div className="bg-navy-850 border border-navy-700/80 rounded-xl p-6 shadow-sm">
          <form onSubmit={handleProceedToReview} className="space-y-4">
            <Input
              id="transfer-recipient"
              label="Recipient Username or Email"
              placeholder="e.g. alice_wonder or colleague@work.com"
              icon={User}
              value={receiver}
              onChange={(e) => setReceiver(e.target.value)}
              required
            />

            <Input
              id="transfer-amount"
              label="Transfer Amount (INR)"
              type="number"
              step="1"
              min="1"
              placeholder="0.00"
              icon={IndianRupee}
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              required
              helperText="Transfers exceeding ₹50,000.00 automatically trigger Step-Up OTP challenge."
            />

            <Input
              id="transfer-note"
              label="Transfer Note / Description"
              placeholder="e.g. Invoice settlement, Project milestone"
              icon={FileText}
              value={note}
              onChange={(e) => setNote(e.target.value)}
            />

            <div className="pt-3 flex justify-end">
              <Button type="submit" variant="primary" icon={ArrowRight}>
                Proceed to Review
              </Button>
            </div>
          </form>
        </div>
      )}

      {/* STEP 2: Review Details */}
      {step === 2 && (
        <div className="bg-navy-850 border border-navy-700/80 rounded-xl p-6 shadow-sm space-y-5">
          <h3 className="text-sm font-semibold uppercase tracking-wider text-white">
            Review Transfer Summary
          </h3>

          <div className="bg-navy-900 rounded-lg border border-navy-750 p-4 space-y-3 text-xs sm:text-sm">
            <div className="flex justify-between py-1.5 border-b border-navy-750">
              <span className="text-slate-400">Sender Account:</span>
              <span className="font-semibold text-white">{user?.username}</span>
            </div>
            <div className="flex justify-between py-1.5 border-b border-navy-750">
              <span className="text-slate-400">Recipient:</span>
              <span className="font-semibold text-white">{receiver}</span>
            </div>
            <div className="flex justify-between py-1.5 border-b border-navy-750">
              <span className="text-slate-400">Transfer Amount:</span>
              <span className="font-bold text-primary-400 text-base">
                ₹{parseFloat(amount || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })} INR
              </span>
            </div>
            <div className="flex justify-between py-1.5 border-b border-navy-750">
              <span className="text-slate-400">Transfer Fee:</span>
              <span className="font-semibold text-emerald-400">₹0.00 (Zero Fee)</span>
            </div>
            {note && (
              <div className="flex justify-between py-1.5">
                <span className="text-slate-400">Note:</span>
                <span className="text-slate-200">{note}</span>
              </div>
            )}
          </div>

          <div className="flex items-center justify-between pt-2">
            <Button
              variant="secondary"
              icon={ArrowLeft}
              onClick={() => setStep(1)}
            >
              Edit Details
            </Button>
            <Button
              variant="primary"
              icon={ArrowRight}
              onClick={handleRunRiskCheck}
            >
              Analyze Fraud Risk
            </Button>
          </div>

        </div>
      )}

      {/* STEP 3: Fraud Risk Check */}
      {step === 3 && (
        <div className="bg-navy-850 border border-navy-700/80 rounded-xl p-6 shadow-sm space-y-5">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-semibold uppercase tracking-wider text-white">
              Real-time Fraud Risk Evaluation
            </h3>
            {riskAssessment && (
              <RiskBadge level={riskAssessment.level} score={riskAssessment.score} />
            )}
          </div>

          {evaluatingRisk ? (
            <div className="py-8 text-center space-y-3">
              <RefreshCw className="w-8 h-8 text-primary-400 animate-spin mx-auto" />
              <p className="text-sm text-slate-300 font-medium">
                Evaluating behavioral heuristics and velocity rules...
              </p>
            </div>
          ) : (
            riskAssessment && (
              <div className="space-y-4">
                <RiskScore score={riskAssessment.score} level={riskAssessment.level} />

                <div className="bg-navy-900 rounded-lg border border-navy-750 p-4">
                  <div className="flex items-center justify-between mb-3 pb-2 border-b border-navy-800">
                    <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                      Key Risk Factors Analyzed
                    </span>
                    <span className="text-[11px] font-mono text-slate-400">
                      Evaluated Score: <span className="font-bold text-amber-400">{riskAssessment.score}/100</span>
                    </span>
                  </div>
                  <ul className="divide-y divide-navy-800/60 text-xs">
                    {riskAssessment.factors.map((factor, idx) => {
                      const { label, points } = getFactorPointInfo(factor);
                      return (
                        <li key={idx} className="py-2 flex items-center justify-between gap-3 first:pt-0 last:pb-0">
                          <div className="flex items-start gap-2 text-slate-300">
                            <span className="text-primary-400 font-bold">•</span>
                            <span>{label}</span>
                          </div>
                          {points !== null && (
                            <span className={`shrink-0 px-2 py-0.5 rounded text-[11px] font-mono font-semibold border ${
                              points > 0
                                ? 'bg-amber-500/15 text-amber-300 border-amber-500/30'
                                : 'bg-emerald-500/15 text-emerald-300 border-emerald-500/30'
                            }`}>
                              {points > 0 ? `+${points} pts` : '0 pts'}
                            </span>
                          )}
                        </li>
                      );
                    })}
                  </ul>
                </div>

                {riskAssessment.level === 'HIGH' && (
                  <div className="p-3 rounded-md bg-amber-950/50 border border-amber-800/60 text-amber-300 text-xs flex items-center gap-2.5">
                    <AlertTriangle className="w-5 h-5 shrink-0 text-amber-400" />
                    <span>
                      High risk threshold reached. You will be asked for step-up OTP authentication next.
                    </span>
                  </div>
                )}

                <Input
                  id="transaction-pin-transfer"
                  label="6-Digit Transaction PIN"
                  type={showTransferPin ? 'text' : 'password'}
                  inputMode="numeric"
                  maxLength={6}
                  placeholder="Enter your transaction PIN"
                  icon={KeyRound}
                  value={transactionPin}
                  onChange={(e) => setTransactionPin(e.target.value.replace(/\D/g, '').slice(0, 6))}
                  endAdornment={
                    <button
                      type="button"
                      onClick={() => setShowTransferPin((visible) => !visible)}
                      aria-label={showTransferPin ? 'Hide transaction PIN' : 'Show transaction PIN'}
                      className="text-slate-400 hover:text-slate-200 p-1"
                    >
                      {showTransferPin ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                    </button>
                  }
                  required
                />

                <div className="flex items-center justify-between pt-3">
                  <Button
                    variant="secondary"
                    icon={ArrowLeft}
                    onClick={() => {
                      setTransactionPin('');
                      setStep(2);
                    }}
                  >
                    Back to Review
                  </Button>
                  <Button
                    variant="primary"
                    icon={Send}
                    loading={submitting}
                    onClick={handleExecuteTransfer}
                  >
                    Confirm & Send Transfer
                  </Button>
                </div>
              </div>
            )
          )}
        </div>
      )}

      {/* STEP 4: OTP Step-Up Challenge (if triggered) */}
      {step === 4 && (
        <div className="bg-navy-850 border border-navy-700/80 rounded-xl p-6 shadow-sm space-y-5">
          <div className="text-center">
            <div className="w-12 h-12 rounded-xl bg-amber-500/20 border border-amber-500/40 text-amber-400 flex items-center justify-center mx-auto mb-3">
              <ShieldAlert className="w-6 h-6" />
            </div>
            <h3 className="text-lg font-bold text-white">Step-Up Verification Required</h3>
            <p className="text-xs text-slate-400 mt-1">
              Elevated risk detected. Enter the 6-digit OTP code to authorize this transaction.
            </p>
          </div>

          <div className="p-3 bg-navy-900 rounded-lg border border-navy-750 text-xs flex justify-between">
            <span className="text-slate-400">Reference Code:</span>
            <span className="font-mono text-primary-400 font-bold">
              {transferResult?.referenceCode || 'TXN-PENDING'}
            </span>
          </div>

          <div className="p-3 rounded-lg bg-navy-900 border border-navy-750 text-xs text-slate-300">
            The verification code was sent to your registered email address{' '}
            <span className="font-semibold text-primary-400">
              ({user?.email || 'your registered email'})
            </span>. It expires in 5 minutes.
          </div>

          <form onSubmit={handleVerifyOtp} className="space-y-4">
            <Input
              id="otp-transfer-code"
              label="6-Digit Verification Code"
              type="text"
              maxLength={6}
              placeholder="Enter code from email"
              icon={KeyRound}
              value={otpCode}
              onChange={(e) => setOtpCode(e.target.value.trim())}
              required
            />

            <Button
              type="submit"
              variant="primary"
              size="lg"
              loading={verifyingOtp}
              icon={ArrowRight}
              className="w-full justify-center"
            >
              Verify OTP & Complete Transfer
            </Button>
          </form>

          <div className="flex items-center justify-between pt-2 border-t border-navy-750 text-xs">
            <button
              type="button"
              onClick={handleResendOtp}
              disabled={resendingOtp}
              className="text-primary-400 hover:text-primary-300 flex items-center gap-1.5 transition-colors disabled:opacity-50"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${resendingOtp ? 'animate-spin' : ''}`} />
              <span>Resend code by email</span>
            </button>
            <button
              type="button"
              onClick={() => setStep(1)}
              className="text-slate-400 hover:text-slate-200"
            >
              Cancel Transfer
            </button>
          </div>
        </div>
      )}

      {/* STEP 5: Success Receipt or Blocked Notice */}
      {step === 5 && (
        <div className={`bg-navy-850 border ${
          transferResult?.status === 'COMPLETED'
            ? 'border-emerald-500/30'
            : 'border-amber-500/30'
        } rounded-xl p-6 shadow-xl text-center space-y-4`}>
          <div className={`w-14 h-14 rounded-full flex items-center justify-center mx-auto ${
            transferResult?.status === 'COMPLETED'
              ? 'bg-emerald-950/80 border border-emerald-600/60 text-emerald-400'
              : 'bg-amber-950/80 border border-amber-600/60 text-amber-400'
          }`}>
            {transferResult?.status === 'COMPLETED' ? (
              <CheckCircle2 className="w-8 h-8" />
            ) : (
              <ShieldAlert className="w-8 h-8" />
            )}
          </div>

          {transferResult?.status === 'COMPLETED' ? (
            <>
              <h3 className="text-xl font-bold text-white">Transfer Successfully Processed</h3>
              <p className="text-xs text-slate-300">
                Funds have been debited from your virtual wallet and credited to{' '}
                <span className="font-semibold text-white">{receiver}</span>.
              </p>
            </>
          ) : transferResult?.description?.includes('OTP Verified') ? (
            <>
              <h3 className="text-xl font-bold text-white">OTP Verified — Queued For Analyst Review</h3>
              <p className="text-xs text-slate-300">
                Step-up authentication succeeded. Because this transfer was identified with high fraud risk, it has been securely submitted to the Fraud Analyst Queue for final release.
              </p>
            </>
          ) : (
            <>
              <h3 className="text-xl font-bold text-white">Transfer Submitted — Verification Pending</h3>
              <p className="text-xs text-slate-300">
                High fraud risk detected. This transaction is undergoing security review.
              </p>
            </>
          )}

          <div className="bg-navy-900 rounded-lg border border-navy-750 p-4 max-w-md mx-auto text-xs text-left space-y-2">
            <div className="flex justify-between">
              <span className="text-slate-400">Reference Code:</span>
              <span className="font-mono text-primary-400 font-bold">
                {transferResult?.referenceCode || `TXN-${transferResult?.id || '8392'}`}
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-400">Amount:</span>
              <span className="font-bold text-white">₹{parseFloat(amount || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })} INR</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-400">Status:</span>
              <span className={`font-semibold ${
                transferResult?.status === 'COMPLETED' ? 'text-emerald-400' : 'text-amber-400'
              }`}>
                {transferResult?.status || 'VERIFICATION_REQUIRED'}
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-400">Risk Assessment:</span>
              <span className="font-semibold text-slate-300">
                {transferResult?.riskLevel || riskAssessment?.level || 'LOW'} ({transferResult?.riskScore ?? riskAssessment?.score ?? 15}/100)
              </span>
            </div>
          </div>

          <div className="pt-4 flex justify-center gap-3">
            <Button
              variant="secondary"
              onClick={() => {
                setStep(1);
                setReceiver('');
                setAmount('');
                setNote('');
                setTransactionPin('');
                setTransferResult(null);
                setRiskAssessment(null);
                setOtpCode('');
              }}
            >
              {transferResult?.blocked ? 'New Transfer' : 'Send Another Payment'}
            </Button>
            <Link to="/customer/transactions">
              <Button variant="primary" icon={ArrowRight}>
                View in History
              </Button>
            </Link>
          </div>
        </div>
      )}
        </>
      )}
    </div>
  );
}
