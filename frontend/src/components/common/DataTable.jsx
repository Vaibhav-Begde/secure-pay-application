import React, { useState } from 'react';
import { ChevronLeft, ChevronRight, Inbox } from 'lucide-react';
import Loading from './Loading';
import EmptyState from './EmptyState';

export default function DataTable({
  columns,
  data = [],
  loading = false,
  pageSize = 10,
  pagination = true,
  onRowClick,
  emptyTitle = 'No data available',
  emptyDescription = 'No records match your criteria.',
}) {
  const [currentPage, setCurrentPage] = useState(1);

  if (loading) {
    return <Loading text="Loading table records..." />;
  }

  if (!data || data.length === 0) {
    return (
      <EmptyState
        icon={Inbox}
        title={emptyTitle}
        description={emptyDescription}
      />
    );
  }

  const totalPages = Math.ceil(data.length / pageSize);
  const startIndex = (currentPage - 1) * pageSize;
  const currentData = pagination
    ? data.slice(startIndex, startIndex + pageSize)
    : data;

  const handlePrev = () => setCurrentPage((p) => Math.max(p - 1, 1));
  const handleNext = () => setCurrentPage((p) => Math.min(p + 1, totalPages));

  return (
    <div className="w-full bg-navy-850 rounded-lg border border-navy-700/80 overflow-hidden shadow-sm">
      <div className="overflow-x-auto">
        <table className="w-full text-left text-sm text-slate-200">
          <thead className="bg-navy-900/90 text-xs uppercase tracking-wider text-slate-400 border-b border-navy-700/80 font-semibold">
            <tr>
              {columns.map((col, idx) => (
                <th
                  key={idx}
                  scope="col"
                  className={`px-4 py-3.5 ${col.className || ''}`}
                >
                  {col.header}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-navy-700/50">
            {currentData.map((row, rowIdx) => (
              <tr
                key={row.id || rowIdx}
                onClick={() => onRowClick && onRowClick(row)}
                className={`transition-colors ${
                  onRowClick
                    ? 'cursor-pointer hover:bg-navy-800/80'
                    : 'hover:bg-navy-800/40'
                }`}
              >
                {columns.map((col, colIdx) => {
                  let value;
                  if (typeof col.accessor === 'function') {
                    value = col.accessor(row);
                  } else if (typeof col.accessor === 'string') {
                    value = row[col.accessor];
                  }

                  return (
                    <td
                      key={colIdx}
                      className={`px-4 py-3.5 text-xs sm:text-sm whitespace-nowrap ${
                        col.cellClassName || ''
                      }`}
                    >
                      {col.cell ? col.cell(row, value) : value ?? '—'}
                    </td>
                  );
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Pagination Bar */}
      {pagination && totalPages > 1 && (
        <div className="flex items-center justify-between px-4 py-3 border-t border-navy-700/80 bg-navy-900/40 text-xs text-slate-400">
          <div>
            Showing <span className="text-slate-200 font-medium">{startIndex + 1}</span> to{' '}
            <span className="text-slate-200 font-medium">
              {Math.min(startIndex + pageSize, data.length)}
            </span>{' '}
            of <span className="text-slate-200 font-medium">{data.length}</span> results
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={handlePrev}
              disabled={currentPage === 1}
              className="p-1.5 rounded-md border border-navy-700 hover:bg-navy-800 disabled:opacity-40 disabled:cursor-not-allowed text-slate-300"
              title="Previous Page"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <span className="px-2 text-slate-300 font-mono">
              {currentPage} / {totalPages}
            </span>
            <button
              onClick={handleNext}
              disabled={currentPage === totalPages}
              className="p-1.5 rounded-md border border-navy-700 hover:bg-navy-800 disabled:opacity-40 disabled:cursor-not-allowed text-slate-300"
              title="Next Page"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
