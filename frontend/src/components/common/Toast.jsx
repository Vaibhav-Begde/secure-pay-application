import React from 'react';
import { CheckCircle2, AlertCircle, AlertTriangle, Info, X } from 'lucide-react';

export default function Toast({
  type = 'info',
  title,
  message,
  onClose,
  className = '',
}) {
  const configs = {
    info: {
      bg: 'bg-blue-950/60 border-blue-800/60 text-blue-200',
      icon: Info,
      iconColor: 'text-blue-400',
    },
    success: {
      bg: 'bg-emerald-950/60 border-emerald-800/60 text-emerald-200',
      icon: CheckCircle2,
      iconColor: 'text-emerald-400',
    },
    warning: {
      bg: 'bg-amber-950/60 border-amber-800/60 text-amber-200',
      icon: AlertTriangle,
      iconColor: 'text-amber-400',
    },
    error: {
      bg: 'bg-rose-950/60 border-rose-800/60 text-rose-200',
      icon: AlertCircle,
      iconColor: 'text-rose-400',
    },
  };

  const current = configs[type] || configs.info;
  const Icon = current.icon;

  return (
    <div
      className={`flex items-start gap-3 p-4 rounded-lg border shadow-sm ${current.bg} ${className}`}
      role="alert"
    >
      <Icon className={`w-5 h-5 mt-0.5 shrink-0 ${current.iconColor}`} />
      <div className="flex-1 text-sm">
        {title && <p className="font-semibold mb-0.5">{title}</p>}
        <p className="leading-relaxed opacity-95">{message}</p>
      </div>
      {onClose && (
        <button
          onClick={onClose}
          className="p-1 -mr-1 -mt-1 rounded hover:bg-black/20 text-current opacity-70 hover:opacity-100 transition-opacity"
        >
          <X className="w-4 h-4" />
        </button>
      )}
    </div>
  );
}
