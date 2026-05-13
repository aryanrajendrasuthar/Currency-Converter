import { useState, useEffect, useCallback } from 'react';
import { ArrowLeftRight, Star, StarOff } from 'lucide-react';
import { LineChart, Line, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from 'recharts';
import CurrencySelect from '../components/CurrencySelect';
import { ratesApi, userApi } from '../api';
import { useAuth } from '../context/AuthContext';
import type { ConversionResult, HistoricalDataPoint } from '../types';

const PERIODS = ['7d', '30d', '90d', '1y'] as const;

export default function ConverterPage() {
  const { isAuthenticated } = useAuth();
  const [from, setFrom] = useState('USD');
  const [to, setTo] = useState('EUR');
  const [amount, setAmount] = useState('1');
  const [result, setResult] = useState<ConversionResult | null>(null);
  const [history, setHistory] = useState<HistoricalDataPoint[]>([]);
  const [period, setPeriod] = useState<typeof PERIODS[number]>('7d');
  const [loading, setLoading] = useState(false);
  const [chartLoading, setChartLoading] = useState(false);
  const [favorites, setFavorites] = useState<string[]>(() =>
    JSON.parse(localStorage.getItem('favorites') || '[]'));

  const convert = useCallback(async () => {
    const num = parseFloat(amount);
    if (!num || num <= 0) return;
    setLoading(true);
    try {
      const { data } = await ratesApi.convert(from, to, num);
      setResult(data);
    } catch {
      /* handled silently */
    } finally {
      setLoading(false);
    }
  }, [from, to, amount]);

  useEffect(() => {
    const t = setTimeout(convert, 400);
    return () => clearTimeout(t);
  }, [convert]);

  useEffect(() => {
    setChartLoading(true);
    ratesApi.getHistorical(from, to, period)
      .then(({ data }) => setHistory(data))
      .finally(() => setChartLoading(false));
  }, [from, to, period]);

  const swap = () => { setFrom(to); setTo(from); };

  const pairKey = `${from}/${to}`;
  const isFav = favorites.includes(pairKey);

  const toggleFavorite = async () => {
    const next = isFav ? favorites.filter(f => f !== pairKey) : [...favorites, pairKey];
    setFavorites(next);
    localStorage.setItem('favorites', JSON.stringify(next));
    if (isAuthenticated) {
      try {
        isFav ? await userApi.removeFavorite(pairKey) : await userApi.addFavorite(pairKey);
      } catch { /* best effort */ }
    }
  };

  const chartData = history.map(h => ({
    date: new Date(h.date).toLocaleDateString('en-US', { month: 'short', day: 'numeric' }),
    rate: Number(h.rate),
  }));

  return (
    <div className="page converter-page">
      <div className="converter-card">
        <div className="converter-header">
          <h1>Currency Converter</h1>
          <button className={`fav-btn ${isFav ? 'active' : ''}`} onClick={toggleFavorite} title="Favorite pair">
            {isFav ? <Star size={20} fill="currentColor" /> : <StarOff size={20} />}
          </button>
        </div>

        <div className="converter-inputs">
          <div className="amount-group">
            <label>Amount</label>
            <input
              type="number"
              className="amount-input"
              value={amount}
              min="0"
              step="any"
              onChange={e => setAmount(e.target.value)}
              placeholder="0.00"
            />
          </div>

          <div className="selector-row">
            <CurrencySelect value={from} onChange={setFrom} label="From" />
            <button className="swap-btn" onClick={swap} title="Swap currencies">
              <ArrowLeftRight size={20} />
            </button>
            <CurrencySelect value={to} onChange={setTo} label="To" />
          </div>
        </div>

        {loading && <div className="loading-bar" />}

        {result && (
          <div className="result-panel">
            <div className="result-amount">
              <span className="result-value">
                {result.convertedAmount.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 6 })}
              </span>
              <span className="result-currency">{result.toCurrency}</span>
            </div>
            <div className="result-rate">
              1 {result.fromCurrency} = {result.rate.toLocaleString('en-US', { minimumFractionDigits: 4, maximumFractionDigits: 8 })} {result.toCurrency}
            </div>
          </div>
        )}

        {favorites.length > 0 && (
          <div className="favorites-row">
            <span className="favorites-label">Favorites:</span>
            {favorites.map(pair => {
              const [f, t] = pair.split('/');
              return (
                <button
                  key={pair}
                  className={`pair-chip ${pair === pairKey ? 'active' : ''}`}
                  onClick={() => { setFrom(f); setTo(t); }}
                >
                  {pair}
                </button>
              );
            })}
          </div>
        )}
      </div>

      <div className="chart-card">
        <div className="chart-header">
          <h2>Historical Rates — {from}/{to}</h2>
          <div className="period-tabs">
            {PERIODS.map(p => (
              <button
                key={p}
                className={`period-tab ${period === p ? 'active' : ''}`}
                onClick={() => setPeriod(p)}
              >
                {p}
              </button>
            ))}
          </div>
        </div>

        {chartLoading ? (
          <div className="chart-skeleton" />
        ) : chartData.length > 0 ? (
          <ResponsiveContainer width="100%" height={280}>
            <LineChart data={chartData} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
              <XAxis dataKey="date" tick={{ fill: '#94a3b8', fontSize: 12 }} />
              <YAxis
                tick={{ fill: '#94a3b8', fontSize: 12 }}
                domain={['auto', 'auto']}
                tickFormatter={(v) => v.toFixed(4)}
              />
              <Tooltip
                contentStyle={{ background: '#1e293b', border: '1px solid #334155', borderRadius: 8 }}
                labelStyle={{ color: '#f1f5f9' }}
                itemStyle={{ color: '#f59e0b' }}
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                formatter={((v: number) => v?.toFixed(6) ?? v) as any}
              />
              <Line
                type="monotone"
                dataKey="rate"
                stroke="#f59e0b"
                strokeWidth={2}
                dot={false}
                activeDot={{ r: 4, fill: '#f59e0b' }}
              />
            </LineChart>
          </ResponsiveContainer>
        ) : (
          <div className="chart-empty">
            <p>No historical data yet. Data is stored daily — check back tomorrow.</p>
          </div>
        )}
      </div>
    </div>
  );
}
