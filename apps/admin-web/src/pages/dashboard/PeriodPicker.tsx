import { useState } from 'react';
import { Calendar } from 'lucide-react';
import type { DashboardRange } from '../../api/services';
import { doualaDate } from '../../utils/format';
import { PRESETS } from './period';

/** Last 7/30/90 days, or a custom inclusive date range (applied with a button so typing does not reload). */
export function PeriodPicker({ range, onChange }: { range: DashboardRange; onChange: (r: DashboardRange) => void }) {
  const custom = 'from' in range;
  const [editing, setEditing] = useState(custom);
  const [from, setFrom] = useState(custom ? range.from : doualaDate(-29));
  const [to, setTo] = useState(custom ? range.to : doualaDate());
  const value = editing ? 'custom' : String('days' in range ? range.days : 30);

  return (
    <div className="period-picker">
      <label className="select-pill"><Calendar size={16} aria-hidden="true" />
        <select value={value} aria-label="Period" onChange={e => {
          if (e.target.value === 'custom') { setEditing(true); return; }
          setEditing(false);
          onChange({ days: Number(e.target.value) });
        }}>
          {PRESETS.map(d => <option key={d} value={d}>Last {d} Days</option>)}
          <option value="custom">Custom range…</option>
        </select>
      </label>
      {editing && (
        <>
          <input className="input" type="date" aria-label="From" value={from} max={to} onChange={e => setFrom(e.target.value)} />
          <span className="muted">to</span>
          <input className="input" type="date" aria-label="To" value={to} min={from} max={doualaDate()} onChange={e => setTo(e.target.value)} />
          <button className="btn gold sm" disabled={!from || !to || from > to} onClick={() => onChange({ from, to })}>Apply</button>
        </>
      )}
    </div>
  );
}
