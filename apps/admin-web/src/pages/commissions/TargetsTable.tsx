import { Link } from 'react-router-dom';
import { DataTable, LabelPill, LevelBar, TitleCell } from '../../components/ui';
import { date } from '../../utils/format';
import { PERIOD_TYPE, bonusLabel, targetAmount } from '../../utils/earnings';
import { xaf } from '../../utils/format';
import type { SellerTarget } from '../../types/models';

const STATE: Record<string, [string, string]> = { CURRENT: ['In progress', 'blue'], UPCOMING: ['Upcoming', 'grey'], ENDED: ['Ended', 'navy'], CANCELLED: ['Cancelled', 'grey'] };

/** Targets with live progress; `withSeller` adds the seller column (programme-wide list). */
export function TargetsTable({ rows, withSeller, actions }: { rows: SellerTarget[]; withSeller?: boolean; actions?: (t: SellerTarget) => React.ReactNode }) {
  return (
    <DataTable rows={rows} rowKey={t => t.id} columns={[
      ...(withSeller ? [{ header: 'Seller', cell: (t: SellerTarget) => <Link className="link" to={`/users/${t.seller_id}?tab=earnings`}>{t.seller_name}</Link> }] : []),
      { header: 'Target', cell: t => <TitleCell title={t.name} sub={`${t.metric === 'VALUE' ? 'Sales value' : 'Units'}${t.product_name ? ` · ${t.product_name}` : ''}`} /> },
      { header: 'Period', cell: t => <TitleCell title={PERIOD_TYPE[t.period_type]} sub={t.starts_on === t.ends_on ? date(t.starts_on) : `${date(t.starts_on)} – ${date(t.ends_on)}`} /> },
      { header: 'Progress', cell: t => (
        <div className="target-progress">
          <span><b>{targetAmount(t, t.achieved)}</b> of {targetAmount(t, t.target_value)}</span>
          <LevelBar value={t.achieved} max={t.target_value} tone={t.reached ? 'green' : t.state === 'ENDED' ? 'red' : 'amber'} />
          <small>{t.progress_pct}%</small>
        </div>
      ) },
      { header: 'Bonus', cell: t => <TitleCell title={bonusLabel(t)} sub={t.bonus_reversed ? 'Reversed by a refund' : t.bonus_paid_xaf != null ? `${xaf(t.bonus_paid_xaf)} finalised` : t.reached ? `${xaf(t.bonus_earned)} earned` : 'Not reached'} /> },
      { header: 'Status', cell: t => <LabelPill map={STATE} value={t.status === 'CANCELLED' ? 'CANCELLED' : t.state} /> },
      ...(actions ? [{ header: '', cell: actions }] : []),
    ]} />
  );
}
