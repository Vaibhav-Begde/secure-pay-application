import React, { useEffect, useState } from 'react';
import { Calendar, CheckCircle2, Globe2, Laptop, Mail, MapPin, Shield, User } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import Badge from '../../components/common/Badge';
import Loading from '../../components/common/Loading';

function formatDate(value) {
  if (!value) return 'Not available yet';
  return new Intl.DateTimeFormat('en-IN', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value));
}

function Detail({ icon: Icon, label, value }) {
  return (
    <div className="rounded-lg border border-navy-750 bg-navy-900/70 p-4">
      <div className="flex items-center gap-2 text-[10px] uppercase tracking-wider text-slate-500">
        <Icon className="h-3.5 w-3.5 text-primary-400" />
        {label}
      </div>
      <p className="mt-2 break-words text-sm font-medium text-slate-100">{value || 'Not available'}</p>
    </div>
  );
}

export default function ProfilePage() {
  const { user, refreshCurrentUser } = useAuth();
  const [profile, setProfile] = useState(user);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    refreshCurrentUser()
      .then((data) => {
        if (active) setProfile(data);
      })
      .catch(() => {
        if (active) setProfile(user);
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => { active = false; };
  }, []);

  if (loading && !profile) return <Loading />;

  const currentProfile = profile || user || {};
  const initials = (currentProfile.username || 'SP').slice(0, 2).toUpperCase();

  return (
    <div className="space-y-6">
      <div>
        <p className="text-xs font-mono uppercase tracking-widest text-primary-400">Account center</p>
        <h1 className="mt-1 text-2xl font-bold text-white">My Profile</h1>
        <p className="mt-1 text-sm text-slate-400">Review your account and recent sign-in security details.</p>
      </div>

      <section className="rounded-xl border border-navy-700/80 bg-navy-850 p-5 shadow-sm sm:p-6">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-4">
            <div className="flex h-16 w-16 items-center justify-center rounded-2xl border border-primary-500/40 bg-primary-600/20 text-xl font-bold text-primary-300">
              {initials}
            </div>
            <div>
              <h2 className="text-xl font-bold text-white">{currentProfile.username || 'SecurePay User'}</h2>
              <p className="mt-1 flex items-center gap-1.5 text-sm text-slate-400"><Mail className="h-3.5 w-3.5" />{currentProfile.email || 'Email unavailable'}</p>
            </div>
          </div>
          <Badge variant="success" dot>{currentProfile.enabled === false ? 'Disabled' : 'Active account'}</Badge>
        </div>
      </section>

      <section>
        <div className="mb-3 flex items-center gap-2"><User className="h-4 w-4 text-primary-400" /><h2 className="text-sm font-semibold uppercase tracking-wider text-slate-300">Account information</h2></div>
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          <Detail icon={User} label="Username" value={currentProfile.username} />
          <Detail icon={Mail} label="Email address" value={currentProfile.email} />
          <Detail icon={Shield} label="Role" value={currentProfile.role} />
          <Detail icon={Calendar} label="Account created" value={formatDate(currentProfile.createdAt)} />
          <Detail icon={CheckCircle2} label="Account status" value={currentProfile.enabled === false ? 'Disabled' : 'Active'} />
        </div>
      </section>

      <section>
        <div className="mb-3 flex items-center gap-2"><Shield className="h-4 w-4 text-primary-400" /><h2 className="text-sm font-semibold uppercase tracking-wider text-slate-300">Latest login security</h2></div>
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          <Detail icon={Calendar} label="Login time" value={formatDate(currentProfile.lastLoginAt)} />
          <Detail icon={Laptop} label="Login device" value={currentProfile.lastLoginDevice} />
          <Detail icon={Globe2} label="Login IP" value={currentProfile.lastLoginIp} />
          <Detail icon={MapPin} label="Location" value={currentProfile.lastLoginLocation} />
        </div>
      </section>
    </div>
  );
}
