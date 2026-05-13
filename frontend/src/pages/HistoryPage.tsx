import { useState, useEffect } from 'react';
import { ArrowRight, ChevronLeft, ChevronRight } from 'lucide-react';
import { historyApi } from '../api';
import type { ConversionHistoryItem, PageResponse } from '../types';

export default function HistoryPage() {
  const [data, setData] = useState<PageResponse<ConversionHistoryItem> | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    historyApi.getHistory(page, 15)
      .then(({ data }) => setData(data))
      .finally(() => setLoading(false));
  }, [page]);

  if (loading) return <div className="page"><div className="skeleton-table" /></div>;

  if (!data || data.content.length === 0) {
    return (
      <div className="page">
        <div className="empty-state">
          <h2>No conversion history yet</h2>
          <p>Your conversions will appear here once you start converting currencies.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="page history-page">
      <div className="page-header">
        <h1>Conversion History</h1>
        <span className="badge">{data.totalElements} total</span>
      </div>

      <div className="table-card">
        <table className="data-table">
          <thead>
            <tr>
              <th>From</th>
              <th></th>
              <th>To</th>
              <th>Amount</th>
              <th>Converted</th>
              <th>Rate</th>
              <th>Date</th>
            </tr>
          </thead>
          <tbody>
            {data.content.map(item => (
              <tr key={item.id}>
                <td><span className="currency-badge">{item.fromCurrency}</span></td>
                <td><ArrowRight size={14} className="arrow-icon" /></td>
                <td><span className="currency-badge">{item.toCurrency}</span></td>
                <td className="amount-cell">{Number(item.amount).toLocaleString('en-US', { maximumFractionDigits: 4 })}</td>
                <td className="converted-cell">{Number(item.convertedAmount).toLocaleString('en-US', { maximumFractionDigits: 4 })}</td>
                <td className="rate-cell">{Number(item.rate).toFixed(6)}</td>
                <td className="date-cell">{new Date(item.timestamp).toLocaleString()}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {data.totalPages > 1 && (
        <div className="pagination">
          <button
            className="page-btn"
            disabled={page === 0}
            onClick={() => setPage(p => p - 1)}
          >
            <ChevronLeft size={16} /> Prev
          </button>
          <span className="page-info">Page {page + 1} of {data.totalPages}</span>
          <button
            className="page-btn"
            disabled={page >= data.totalPages - 1}
            onClick={() => setPage(p => p + 1)}
          >
            Next <ChevronRight size={16} />
          </button>
        </div>
      )}
    </div>
  );
}
