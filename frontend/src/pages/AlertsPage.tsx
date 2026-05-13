import { useState, useEffect, type FormEvent } from 'react';
import { Plus, Trash2, Bell, BellOff } from 'lucide-react';
import CurrencySelect from '../components/CurrencySelect';
import { alertsApi } from '../api';
import type { Alert } from '../types';

export default function AlertsPage() {
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [formData, setFormData] = useState({
    fromCurrency: 'USD',
    toCurrency: 'EUR',
    targetRate: '',
    direction: 'ABOVE' as 'ABOVE' | 'BELOW',
  });
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState('');

  const fetchAlerts = () => {
    alertsApi.getAlerts()
      .then(({ data }) => setAlerts(data))
      .finally(() => setLoading(false));
  };

  useEffect(() => { fetchAlerts(); }, []);

  const handleCreate = async (e: FormEvent) => {
    e.preventDefault();
    const rate = parseFloat(formData.targetRate);
    if (!rate || rate <= 0) { setError('Enter a valid target rate'); return; }
    setCreating(true);
    setError('');
    try {
      await alertsApi.createAlert(
        formData.fromCurrency, formData.toCurrency, rate, formData.direction);
      setShowForm(false);
      setFormData({ fromCurrency: 'USD', toCurrency: 'EUR', targetRate: '', direction: 'ABOVE' });
      fetchAlerts();
    } catch (err: any) {
      setError(err.response?.data?.error || 'Failed to create alert');
    } finally {
      setCreating(false);
    }
  };

  const handleDelete = async (id: number) => {
    await alertsApi.deleteAlert(id);
    setAlerts(prev => prev.filter(a => a.id !== id));
  };

  if (loading) return <div className="page"><div className="skeleton-table" /></div>;

  return (
    <div className="page alerts-page">
      <div className="page-header">
        <div>
          <h1>Rate Alerts</h1>
          <p className="page-subtitle">Get notified when a rate hits your target</p>
        </div>
        <button className="btn-primary" onClick={() => setShowForm(s => !s)}>
          <Plus size={16} /> New Alert
        </button>
      </div>

      {showForm && (
        <div className="alert-form-card">
          <h3>Create Rate Alert</h3>
          {error && <div className="error-banner">{error}</div>}
          <form onSubmit={handleCreate} className="alert-form">
            <div className="form-row">
              <CurrencySelect value={formData.fromCurrency} onChange={v => setFormData(f => ({ ...f, fromCurrency: v }))} label="From" />
              <CurrencySelect value={formData.toCurrency} onChange={v => setFormData(f => ({ ...f, toCurrency: v }))} label="To" />
            </div>

            <div className="form-row">
              <div className="field">
                <label>Direction</label>
                <div className="direction-toggle">
                  {(['ABOVE', 'BELOW'] as const).map(d => (
                    <button
                      key={d}
                      type="button"
                      className={`direction-btn ${formData.direction === d ? 'active' : ''}`}
                      onClick={() => setFormData(f => ({ ...f, direction: d }))}
                    >
                      {d === 'ABOVE' ? '↑ Above' : '↓ Below'}
                    </button>
                  ))}
                </div>
              </div>

              <div className="field">
                <label>Target Rate</label>
                <input
                  type="number"
                  step="any"
                  min="0"
                  value={formData.targetRate}
                  onChange={e => setFormData(f => ({ ...f, targetRate: e.target.value }))}
                  placeholder="e.g. 1.1500"
                  required
                />
              </div>
            </div>

            <div className="form-actions">
              <button type="button" className="btn-ghost" onClick={() => setShowForm(false)}>Cancel</button>
              <button type="submit" className="btn-primary" disabled={creating}>
                {creating ? 'Creating...' : 'Create Alert'}
              </button>
            </div>
          </form>
        </div>
      )}

      {alerts.length === 0 ? (
        <div className="empty-state">
          <BellOff size={48} className="empty-icon" />
          <h2>No alerts set</h2>
          <p>Create an alert to be notified when a currency rate reaches your target.</p>
        </div>
      ) : (
        <div className="alerts-grid">
          {alerts.map(alert => (
            <div key={alert.id} className={`alert-card ${alert.triggered ? 'triggered' : ''} ${!alert.active ? 'inactive' : ''}`}>
              <div className="alert-card-header">
                <div className="alert-pair">
                  <span className="currency-badge">{alert.fromCurrency}</span>
                  <span className="arrow">→</span>
                  <span className="currency-badge">{alert.toCurrency}</span>
                </div>
                <div className="alert-status">
                  {alert.triggered ? (
                    <span className="status-badge triggered">Triggered</span>
                  ) : alert.active ? (
                    <span className="status-badge active">
                      <Bell size={12} /> Active
                    </span>
                  ) : (
                    <span className="status-badge inactive">Inactive</span>
                  )}
                </div>
              </div>

              <div className="alert-details">
                <span className={`direction-indicator ${alert.direction.toLowerCase()}`}>
                  {alert.direction === 'ABOVE' ? '↑' : '↓'} {alert.direction}
                </span>
                <span className="target-rate">{Number(alert.targetRate).toFixed(6)}</span>
              </div>

              <div className="alert-footer">
                <span className="alert-date">
                  Created {new Date(alert.createdAt).toLocaleDateString()}
                </span>
                {alert.active && !alert.triggered && (
                  <button
                    className="delete-btn"
                    onClick={() => handleDelete(alert.id)}
                    title="Delete alert"
                  >
                    <Trash2 size={14} />
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
