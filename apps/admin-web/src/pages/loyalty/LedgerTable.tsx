import { Link } from 'react-router-dom';
import { DataTable, LabelPill, TitleCell } from '../../components/ui';
import { dateTime } from '../../utils/format';
import { LEDGER_TYPE, pts, signedPts } from '../../utils/loyalty';
import type { LoyaltyLedgerEntry } from '../../types/models';

const SOURCE_LINK: Record<string, (id: string) => string> = { SALE: id => `/sales/${id}`, REFUND: id => `/sales/refunds/${id}` };

/** Points ledger rows; `withCustomer` adds the member column (programme-wide views). */
export function LedgerTable({ rows, withCustomer }: { rows: LoyaltyLedgerEntry[]; withCustomer?: boolean }) {
  return (
    <DataTable rows={rows} rowKey={e => e.id} columns={[
      { header: 'Date & Time', cell: e => dateTime(e.created_at) },
      ...(withCustomer ? [{ header: 'Member', cell: (e: LoyaltyLedgerEntry) => <Link className="link" to={`/loyalty/members/${e.customer_id}`}>{e.customer_name}</Link> }] : []),
      { header: 'Type', cell: e => <LabelPill map={LEDGER_TYPE} value={e.entry_type} /> },
      { header: 'Description', cell: e => <TitleCell title={e.description ?? '—'} sub={e.actor_name ?? undefined} /> },
      { header: 'Reference', cell: e => (e.reference && e.source_type && e.source_id && SOURCE_LINK[e.source_type]
        ? <Link className="link" to={SOURCE_LINK[e.source_type](e.source_id)}>{e.reference}</Link> : e.reference ?? '—') },
      { header: 'Points', align: 'right', cell: e => <b className={e.points < 0 ? 'warn' : 'ok'}>{signedPts(e.points)}</b> },
      { header: 'Balance', align: 'right', cell: e => pts(e.balance_after) },
    ]} />
  );
}
