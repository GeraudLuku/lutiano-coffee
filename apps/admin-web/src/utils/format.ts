/** Display formatting. Business time zone is Africa/Douala; money is whole FCFA (XAF). */
const TZ = 'Africa/Douala';
const nf = new Intl.NumberFormat('en-US');

export const xaf = (v: unknown) => `${nf.format(Number(v ?? 0))} XAF`;
export const num = (v: unknown) => nf.format(Number(v ?? 0));
/** Money with an explicit + for gains (variances, adjustments). */
export const signedXaf = (v: number) => `${v > 0 ? '+' : ''}${xaf(v)}`;
export const compact = (v: number) =>
  v >= 1_000_000 ? `${(v / 1_000_000).toFixed(1).replace(/\.0$/, '')}M` : v >= 1000 ? `${Math.round(v / 1000)}K` : String(v);

export function dateTime(v?: string | null) {
  if (!v) return '—';
  return new Date(v).toLocaleString('en-GB', { timeZone: TZ, day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
}
export function date(v?: string | null) {
  if (!v) return '—';
  return new Date(v).toLocaleDateString('en-GB', { timeZone: TZ, day: '2-digit', month: 'short', year: 'numeric' });
}
export function time(v?: string | null) {
  if (!v) return '—';
  return new Date(v).toLocaleTimeString('en-GB', { timeZone: TZ, hour: '2-digit', minute: '2-digit' });
}
export function ago(v?: string | null) {
  if (!v) return '—';
  const s = Math.max(0, (Date.now() - new Date(v).getTime()) / 1000);
  if (s < 60) return 'just now';
  if (s < 3600) return `${Math.floor(s / 60)} min ago`;
  if (s < 86400) return `${Math.floor(s / 3600)} h ago`;
  if (s < 86400 * 7) return `${Math.floor(s / 86400)} d ago`;
  return date(v);
}
/** A Douala calendar date as YYYY-MM-DD, offset by whole days from today. */
export function doualaDate(offsetDays = 0) {
  return new Date(Date.now() + offsetDays * 86400000).toLocaleDateString('en-CA', { timeZone: TZ });
}
export const shortDay = (iso: string) => new Date(`${iso}T12:00:00Z`).toLocaleDateString('en-GB', { day: '2-digit', month: 'short' });
export const initials = (name?: string) => (name ?? '?').split(/\s+/).filter(Boolean).slice(0, 2).map(p => p[0]!.toUpperCase()).join('');

export function deltaPct(current: number, previous: number): number | null {
  return previous ? ((current - previous) / previous) * 100 : null;
}
/** "PRODUCT_CREATED" -> "Product created" */
export const humanize = (code: string) => code.toLowerCase().replace(/_/g, ' ').replace(/^\w/, c => c.toUpperCase());
