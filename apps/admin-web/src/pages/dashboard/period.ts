import type { DashboardRange } from '../../api/services';
import { doualaDate } from '../../utils/format';

export const PRESETS = [7, 30, 90] as const;

/** Reads the dashboard period from the URL: ?days=N or ?from=YYYY-MM-DD&to=YYYY-MM-DD (default: last 30 days). */
export function rangeFromParams(params: URLSearchParams): DashboardRange {
  const from = params.get('from'), to = params.get('to');
  if (from && to) return { from, to };
  const days = Number(params.get('days'));
  return { days: PRESETS.includes(days as (typeof PRESETS)[number]) ? days : 30 };
}

/** Inclusive Douala dates for a range, for linking to the Sales list. */
export function rangeDates(range: DashboardRange): { from: string; to: string } {
  return 'from' in range ? range : { from: doualaDate(-(range.days - 1)), to: doualaDate() };
}

/** Link to the Sales list for the period, with extra filters (channel, payment, seller…). */
export function salesLink(dates: { from: string; to: string }, extra: Record<string, string | undefined> = {}) {
  const p = new URLSearchParams({ from: dates.from, to: dates.to });
  Object.entries(extra).forEach(([k, v]) => { if (v) p.set(k, v); });
  return `/sales?${p}`;
}
