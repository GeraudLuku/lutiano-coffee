import { Link } from 'react-router-dom';
import { DataTable, TitleCell } from '../../components/ui';
import { date, num, xaf } from '../../utils/format';
import { SELLER_ROLE_LABEL } from '../../utils/labels';
import { signedXaf } from '../../utils/earnings';
import type { SellerStatement } from '../../types/models';

/** Seller statements: by seller within a period, or by period for one seller (`byPeriod`). */
export function StatementsTable({ rows, byPeriod }: { rows: SellerStatement[]; byPeriod?: boolean }) {
  return (
    <DataTable rows={rows} rowKey={r => `${r.period_id}-${r.seller_id}`} columns={[
      byPeriod
        ? { header: 'Period', cell: r => <Link className="link" to={`/commissions/periods/${r.period_id}`}>{date(r.starts_on!)} – {date(r.ends_on!)}</Link> }
        : { header: 'Seller', cell: r => <TitleCell title={<Link className="link" to={`/users/${r.seller_id}?tab=earnings`}>{r.seller_name}</Link>}
            sub={r.seller_role ? SELLER_ROLE_LABEL[r.seller_role] : undefined} /> },
      { header: 'Sales', align: 'right', cell: r => <TitleCell title={xaf(r.sales_xaf)} sub={`${num(r.sales_count)} sales`} /> },
      { header: 'Commission', align: 'right', cell: r => xaf(r.commission_xaf) },
      { header: 'Bonuses', align: 'right', cell: r => xaf(r.bonus_xaf) },
      { header: 'Adjustments', align: 'right', cell: r => <span className={r.adjustments_xaf < 0 ? 'warn' : ''}>{signedXaf(Number(r.adjustments_xaf))}</span> },
      { header: 'Total', align: 'right', cell: r => <b className={r.total_xaf < 0 ? 'warn' : ''}>{xaf(r.total_xaf)}</b> },
      { header: 'Payable', align: 'right', cell: r => <TitleCell title={xaf(r.payable_xaf)} sub={r.carried_xaf < 0 ? `${xaf(r.carried_xaf)} carried as recovery` : undefined} /> },
    ]} />
  );
}
