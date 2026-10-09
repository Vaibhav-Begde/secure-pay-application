import React from 'react';
import RiskBadge from './RiskBadge';

export default function RiskScore({ score = 0, level, showBar = true, compact = false }) {
  const safeScore = Math.max(0, Math.min(100, Number(score) || 0));

  let colorClass = 'text-emerald-400';
  let barColor = 'bg-emerald-500';
  let computedLevel = 'LOW';

  if (safeScore >= 75) {
    colorClass = 'text-rose-400';
    barColor = 'bg-rose-500';
    computedLevel = 'HIGH';
  } else if (safeScore >= 40) {
    colorClass = 'text-amber-400';
    barColor = 'bg-amber-500';
    computedLevel = 'MEDIUM';
  }

  const effectiveLevel = level || computedLevel;

  if (compact) {
    return (
      <div className="inline-flex items-center gap-2">
        <span className={`text-base font-bold tracking-tight ${colorClass}`}>
          {safeScore}
          <span className="text-xs text-slate-400 font-normal">/100</span>
        </span>
        <RiskBadge level={effectiveLevel} size="sm" showIcon={false} />
      </div>
    );
  }

  return (
    <div className="bg-navy-900 border border-navy-700/80 rounded-lg p-4">
      <div className="flex items-center justify-between mb-2">
        <span className="text-xs font-medium uppercase tracking-wider text-slate-400">
          Fraud Risk Score
        </span>
        <RiskBadge level={effectiveLevel} size="sm" />
      </div>

      <div className="flex items-baseline gap-2 mb-3">
        <span className={`text-3xl font-extrabold tracking-tight ${colorClass}`}>
          {safeScore}
        </span>
        <span className="text-sm text-slate-400 font-medium">/ 100 max risk</span>
      </div>

      {showBar && (
        <div>
          <div className="w-full bg-navy-800 rounded-full h-2 overflow-hidden flex">
            <div
              className={`h-full transition-all duration-500 rounded-full ${barColor}`}
              style={{ width: `${safeScore}%` }}
            />
          </div>
          <div className="flex justify-between text-[10px] text-slate-500 mt-1 font-mono">
            <span>0 (Low)</span>
            <span>40 (Med)</span>
            <span>75 (High)</span>
            <span>100</span>
          </div>
        </div>
      )}
    </div>
  );
}
