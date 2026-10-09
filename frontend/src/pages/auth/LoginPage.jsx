import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { Shield, Lock, User, Eye, EyeOff, ArrowRight } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import Toast from '../../components/common/Toast';

export default function LoginPage() {
  const { login } = useAuth();
  const { error: toastError, success: toastSuccess } = useToast();
  const navigate = useNavigate();
  const location = useLocation();

  const [usernameOrEmail, setUsernameOrEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [formError, setFormError] = useState('');

  const registeredNotice = location.state?.registered;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setFormError('');
    if (!usernameOrEmail.trim() || !password) {
      setFormError('Please enter your username/email and password.');
      return;
    }

    setLoading(true);
    try {
      const user = await login(usernameOrEmail, password);
      toastSuccess(`Welcome back, ${user.username}!`);
      
      // Navigate to respective role dashboard
      if (user.role === 'ADMIN') {
        navigate('/admin/dashboard', { replace: true });
      } else if (user.role === 'FRAUD_ANALYST') {
        navigate('/analyst/dashboard', { replace: true });
      } else {
        navigate('/customer/dashboard', { replace: true });
      }
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Authentication failed. Please check credentials.';
      setFormError(msg);
      toastError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-navy-950 flex flex-col justify-center py-12 px-4 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md text-center">
        <div className="w-12 h-12 rounded-xl bg-primary-600/20 border border-primary-500/40 text-primary-400 flex items-center justify-center mx-auto mb-4 shadow-lg">
          <Shield className="w-7 h-7" />
        </div>
        <h2 className="text-2xl font-bold tracking-tight text-white">
          Secure<span className="text-primary-400">Pay</span> Enterprise Portal
        </h2>
        <p className="mt-1.5 text-xs text-slate-400 uppercase tracking-wider font-mono">
          Zero-Trust Authentication & Realtime Risk Gateway
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-navy-900 border border-navy-700/90 py-8 px-6 shadow-2xl rounded-xl sm:px-10">
          {registeredNotice && (
            <div className="mb-5">
              <Toast
                type="success"
                title="Account Created"
                message="Registration successful. Please log in with your credentials."
              />
            </div>
          )}

          {formError && (
            <div className="mb-5">
              <Toast type="error" message={formError} />
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <Input
              id="login-username"
              label="Username or Corporate Email"
              type="text"
              placeholder="e.g. john_doe or analyst@bank.com"
              icon={User}
              value={usernameOrEmail}
              onChange={(e) => setUsernameOrEmail(e.target.value)}
              required
            />

            <Input
              id="login-password"
              label="Password"
              type={showPassword ? 'text' : 'password'}
              placeholder="••••••••••••"
              icon={Lock}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              endAdornment={
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="text-slate-400 hover:text-slate-200 p-1"
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              }
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
                Authenticate Session
              </Button>
            </div>
          </form>

          <div className="mt-6 text-center text-xs text-slate-400">
            Don't have an enterprise account?{' '}
            <Link
              to="/register"
              className="text-primary-400 hover:text-primary-300 font-semibold underline underline-offset-4"
            >
              Register now
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
