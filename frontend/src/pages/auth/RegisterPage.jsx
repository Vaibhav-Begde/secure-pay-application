import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Shield, Lock, User, Mail, UserCheck, ArrowRight } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import Toast from '../../components/common/Toast';

export default function RegisterPage() {
  const { register } = useAuth();
  const { success: toastSuccess, error: toastError } = useToast();
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: '',
    role: 'CUSTOMER',
  });
  const [loading, setLoading] = useState(false);
  const [formError, setFormError] = useState('');

  const handleChange = (e) => {
    setFormData((prev) => ({
      ...prev,
      [e.target.name]: e.target.value,
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setFormError('');

    if (formData.password.length < 6) {
      setFormError('Password must be at least 6 characters long.');
      return;
    }

    setLoading(true);
    try {
      await register(formData);
      toastSuccess('Account created successfully! Please sign in.');
      navigate('/login', { state: { registered: true } });
    } catch (err) {
      const msg =
        err.response?.data?.message ||
        err.message ||
        'Registration failed. Username or email may already exist.';
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
          Create Secure<span className="text-primary-400">Pay</span> Account
        </h2>
        <p className="mt-1.5 text-xs text-slate-400 uppercase tracking-wider font-mono">
          Virtual Wallet Provisioning with Risk Analytics
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-navy-900 border border-navy-700/90 py-8 px-6 shadow-2xl rounded-xl sm:px-10">
          {formError && (
            <div className="mb-5">
              <Toast type="error" message={formError} />
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <Input
              id="reg-username"
              name="username"
              label="Username"
              type="text"
              placeholder="e.g. alex_stone"
              icon={User}
              value={formData.username}
              onChange={handleChange}
              required
            />

            <Input
              id="reg-email"
              name="email"
              label="Email Address"
              type="email"
              placeholder="alex@company.com"
              icon={Mail}
              value={formData.email}
              onChange={handleChange}
              required
            />

            <Input
              id="reg-password"
              name="password"
              label="Password (min 6 chars)"
              type="password"
              placeholder="••••••••••••"
              icon={Lock}
              value={formData.password}
              onChange={handleChange}
              required
            />

            <div className="rounded-lg bg-navy-850 border border-navy-750 p-3 text-xs text-slate-400">
              <span className="font-semibold text-slate-200">Customer Account:</span> Includes a virtual wallet credited with an initial ₹1,000 balance and real-time fraud protection.
            </div>

            <div className="pt-2">
              <Button
                type="submit"
                variant="primary"
                size="lg"
                loading={loading}
                icon={UserCheck}
                className="w-full justify-center"
              >
                Complete Registration
              </Button>
            </div>
          </form>

          <div className="mt-6 text-center text-xs text-slate-400">
            Already registered?{' '}
            <Link
              to="/login"
              className="text-primary-400 hover:text-primary-300 font-semibold underline underline-offset-4"
            >
              Sign in to your account
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
