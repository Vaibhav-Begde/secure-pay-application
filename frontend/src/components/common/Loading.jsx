import React from 'react';
import { Loader2 } from 'lucide-react';

export default function Loading({ text = 'Loading data...', fullScreen = false }) {
  const content = (
    <div className="flex flex-col items-center justify-center p-8 text-center">
      <Loader2 className="w-8 h-8 animate-spin text-primary-500 mb-3" />
      <p className="text-sm font-medium text-slate-400">{text}</p>
    </div>
  );

  if (fullScreen) {
    return (
      <div className="min-h-screen bg-navy-950 flex items-center justify-center">
        {content}
      </div>
    );
  }

  return content;
}
