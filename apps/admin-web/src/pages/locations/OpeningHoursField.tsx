import type { OpeningHours } from '../../types/models';

const DAYS = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];

/** One row per weekday: whether the location is open and from when to when (Douala time). */
export type HoursDraft = { open: boolean; opens: string; closes: string }[];

export const EMPTY_HOURS: HoursDraft = DAYS.map(() => ({ open: false, opens: '07:00', closes: '19:00' }));

export function hoursToDraft(hours: OpeningHours[] | undefined): HoursDraft {
  return EMPTY_HOURS.map((d, i) => {
    const h = hours?.find(x => x.weekday === i + 1);
    return h ? { open: true, opens: h.opens.slice(0, 5), closes: h.closes.slice(0, 5) } : d;
  });
}

export function draftToHours(draft: HoursDraft): OpeningHours[] {
  return draft.flatMap((d, i) => (d.open ? [{ weekday: i + 1, opens: d.opens, closes: d.closes }] : []));
}

/** Opening hours editor. With every day switched off the location counts as always open. */
export function OpeningHoursField({ value, onChange }: { value: HoursDraft; onChange: (v: HoursDraft) => void }) {
  const set = (i: number, patch: Partial<HoursDraft[number]>) => onChange(value.map((d, j) => (j === i ? { ...d, ...patch } : d)));
  return (
    <div className="stack">
      {value.map((d, i) => (
        <div key={DAYS[i]} className="input-row">
          <label className="check-line" style={{ minWidth: 120 }}>
            <input type="checkbox" checked={d.open} onChange={e => set(i, { open: e.target.checked })} />{DAYS[i]}
          </label>
          <input className="input" type="time" aria-label={`${DAYS[i]} opens`} value={d.opens} disabled={!d.open} onChange={e => set(i, { opens: e.target.value })} />
          <span className="muted">to</span>
          <input className="input" type="time" aria-label={`${DAYS[i]} closes`} value={d.closes} disabled={!d.open} onChange={e => set(i, { closes: e.target.value })} />
        </div>
      ))}
      <small className="muted">Customers see whether the location is open now. Leave every day off for a location that is always open.</small>
    </div>
  );
}
