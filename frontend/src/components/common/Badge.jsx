import React from 'react';

export default function Badge({
  children,
  variant = 'neutral',
  size = 'md',
  dot = false,
  className = '',
}) {
  const sizeStyles = {
    sm: 'text-[10px] px-1.5 py-0.5',
    md: 'text-xs px-2.5 py-0.5',
    lg: 'text-sm px-3 py-1',
  };

  const variantStyles = {
    neutral: 'bg-navy-800 text-slate-300 border-navy-700',
    primary: 'bg-blue-950/70 text-blue-400 border-blue-800/50',
    success: 'bg-emerald-950/70 text-emerald-400 border-emerald-800/50',
    warning: 'bg-amber-950/70 text-amber-400 border-amber-800/50',
    danger: 'bg-rose-950/70 text-rose-400 border-rose-800/50',
    purple: 'bg-purple-950/70 text-purple-400 border-purple-800/50',
  };

  const dotColors = {
    neutral: 'bg-slate-400',
    primary: 'bg-blue-400',
    success: 'bg-emerald-400',
    warning: 'bg-amber-400',
    danger: 'bg-rose-400',
    purple: 'bg-purple-400',
  };

  return (
    <span
      className={`inline-flex items-center gap-1.5 font-medium rounded-full border ${sizeStyles[size] || sizeStyles.md} ${variantStyles[variant] || variantStyles.neutral} ${className}`}
    >
      {dot && (
        <span
          className={`w-1.5 h-1.5 rounded-full shrink-0 ${dotColors[variant] || dotColors.neutral}`}
        />
      )}
      {children}
    </span>
  );
}
