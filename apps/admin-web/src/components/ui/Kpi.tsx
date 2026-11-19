import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { ArrowDownRight, ArrowUpRight } from 'lucide-react';

export type KpiTone = 'navy' | 'gold' | 'teal' | 'blue' | 'purple' | 'green';

/** Large coloured headline card (design: dashboard / overview pages). With `to`, the card links to the records behind it. */
export function Kpi({ tone, icon, value, label, delta, to, foot }: {
  tone: KpiTone; icon: ReactNode; value: ReactNode; label: string; delta?: number | null; to?: string; foot?: ReactNode;
}) {
  const body = (
    <>
      <div className="kpi-icon">{icon}</div>
      <strong>{value}</strong>
      <span>{label}</span>
      {delta !== undefined && (
        <em className={delta === null ? 'flat' : delta >= 0 ? 'up' : 'down'}>
          {delta === null ? 'No previous data'
            : <>{delta >= 0 ? <ArrowUpRight size={14} /> : <ArrowDownRight size={14} />}{Math.abs(delta).toFixed(1)}% vs previous</>}
        </em>
      )}
      {foot && <div className="kpi-foot">{foot}</div>}
    </>
  );
  return to
    ? <Link to={to} className={`kpi kpi-${tone} kpi-link`} aria-label={`${label}: view details`}>{body}</Link>
    : <article className={`kpi kpi-${tone}`}>{body}</article>;
}

export function KpiRow({ children, columns = 4 }: { children: ReactNode; columns?: 2 | 4 }) {
  return <div className={`kpis ${columns === 2 ? 'two' : ''}`}>{children}</div>;
}

/** Small white summary card with a tinted icon (design: list page summaries). */
export function StatCard({ icon, tone, value, label }: { icon: ReactNode; tone: string; value: ReactNode; label: string }) {
  return (
    <article className="stat">
      <div className={`stat-icon ${tone}`}>{icon}</div>
      <div><strong>{value}</strong><span>{label}</span></div>
    </article>
  );
}
export const StatRow = ({ children, five }: { children: ReactNode; five?: boolean }) => <div className={five ? 'stats4 stats5' : 'stats4'}>{children}</div>;
