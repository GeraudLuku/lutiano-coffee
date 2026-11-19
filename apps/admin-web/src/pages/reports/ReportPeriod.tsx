import { doualaDate } from '../../utils/format';

export interface ReportRange { from: string; to: string }
export const defaultRange = (): ReportRange => ({ from: doualaDate(-29), to: doualaDate() });

/** From / To pickers shared by every report. */
export function ReportPeriod({ range, onChange }: { range: ReportRange; onChange: (r: ReportRange) => void }) {
  return (
    <div className="report-period">
      <label className="inline-date"><span>From</span><input className="input" type="date" value={range.from} max={range.to} onChange={e => onChange({ ...range, from: e.target.value })} /></label>
      <label className="inline-date"><span>To</span><input className="input" type="date" value={range.to} min={range.from} onChange={e => onChange({ ...range, to: e.target.value })} /></label>
    </div>
  );
}
