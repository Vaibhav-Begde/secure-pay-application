import React from 'react';
import { ShieldCheck, ShieldAlert, AlertTriangle } from 'lucide-react';

export default function RiskBadge({ level, score, showIcon = true, size = 'md' }) {
  // Normalize level
  let normalized = (level || '').toUpperCase();
  if (!normalized && typeof score === 'number') {
    if (score < 40) normalized = 'LOW';
    else if (score < 75) normalized = 'MEDIUM';
    else normalized = 'HIGH';
  }

  const configs = {
    LOW: {
      label: 'LOW RISK',
      bg: 'bg-emerald-950/80 text-emerald-400 border-emerald-800/60',
      icon: ShieldCheck,
      dot: 'bg-emerald-400',
    },
    MEDIUM: {
      label: 'MEDIUM RISK',
      bg: 'bg-amber-950/80 text-amber-400 border-amber-800/60',
      icon: AlertTriangle,
      dot: 'bg-amber-400',
    },
    HIGH: {
      label: 'HIGH RISK',
      bg: 'bg-rose-950/80 text-rose-400 border-rose-800/60',
      icon: ShieldAlert,
      dot: 'bg-rose-400',
    },
  };

  const current = configs[normalized] || configs.LOW;
  const Icon = current.icon;

  const sizeStyles = {
    sm: 'text-[10px] px-2 py-0.5 gap-1 font-semibold tracking-wider',
    md: 'text-xs px-2.5 py-1 gap-1.5 font-semibold tracking-wider',
    lg: 'text-sm px-3 py-1.5 gap-2 font-bold tracking-wider',
  };

  const iconSizes = {
    sm: 'w-3 h-3',
    md: 'w-3.5 h-3.5',
    lg: 'w-4 h-4',
  };

  return (
    <span
      className={`inline-flex items-center rounded-full border shadow-sm ${sizeStyles[size] || sizeStyles.md} ${current.bg}`}
    >
      {showIcon ? (
        <Icon className={`${iconSizes[size] || iconSizes.md} shrink-0`} />
      ) : (
        <span className={`w-1.5 h-1.5 rounded-full shrink-0 ${current.dot}`} />
      )}
      {current.label}
    </span>
  );
}
