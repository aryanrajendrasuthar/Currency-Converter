import React, { useState, useRef, useEffect } from 'react';
import { ChevronDown } from 'lucide-react';
import { CURRENCIES } from '../types';

interface Props {
  value: string;
  onChange: (code: string) => void;
  label: string;
}

export default function CurrencySelect({ value, onChange, label }: Props) {
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState('');
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const selected = CURRENCIES.find(c => c.code === value);
  const filtered = CURRENCIES.filter(c =>
    c.code.toLowerCase().includes(search.toLowerCase()) ||
    c.name.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="currency-select-wrapper" ref={ref}>
      <label>{label}</label>
      <button
        type="button"
        className={`currency-select-trigger ${open ? 'open' : ''}`}
        onClick={() => { setOpen(o => !o); setSearch(''); }}
      >
        <span className="currency-flag">{selected?.flag}</span>
        <span className="currency-code">{selected?.code}</span>
        <span className="currency-name">{selected?.name}</span>
        <ChevronDown size={16} className={`select-chevron ${open ? 'open' : ''}`} />
      </button>

      {open && (
        <div className="currency-dropdown">
          <div className="currency-search">
            <input
              autoFocus
              placeholder="Search currency..."
              value={search}
              onChange={e => setSearch(e.target.value)}
            />
          </div>
          <div className="currency-options">
            {filtered.map(c => (
              <div
                key={c.code}
                className={`currency-option ${c.code === value ? 'selected' : ''}`}
                onClick={() => { onChange(c.code); setOpen(false); }}
              >
                <span className="currency-flag">{c.flag}</span>
                <span className="currency-code">{c.code}</span>
                <span className="currency-name">{c.name}</span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
