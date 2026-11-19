import { useNavigate } from 'react-router-dom';
import { DataTable, Pill, type Column } from '../ui';
import { CompletedPill, TypePill } from './Pills';
import { dateTime, num, xaf } from '../../utils/format';
import { paymentLabel } from '../../utils/payment';
import type { SaleSummary } from '../../types/models';

/**
 * Sales list used by the Sales page, location detail and user detail.
 * `full` shows every column; `bySeller` hides the seller (user page); `byLocation` hides the location (location page).
 */
export function SalesTable({ rows, variant = 'full', empty = 'No sales yet.' }: {
  rows: SaleSummary[]; variant?: 'full' | 'bySeller' | 'byLocation'; empty?: string;
}) {
  const nav = useNavigate();
  if (!rows.length) return <p className="muted pad">{empty}</p>;
  const columns: (Column<SaleSummary> | false)[] = [
    { header: 'Reference', cell: s => <b>{s.reference}</b> },
    { header: 'Date & Time', cell: s => dateTime(s.created_at) },
    variant !== 'byLocation' && { header: 'Sales Location', cell: s => <>{s.operation_name}{variant === 'full' && <div><TypePill type={s.operation_type} /></div>}</> },
    variant !== 'bySeller' && { header: 'Seller', cell: s => s.seller_name },
    variant === 'full' && { header: 'Items', align: 'right', cell: s => `${num(s.item_count)} ${Number(s.item_count) === 1 ? 'item' : 'items'}` },
    { header: 'Amount', align: 'right', cell: s => <b>{xaf(s.total_xaf)}</b> },
    { header: 'Payment', cell: s => paymentLabel(s.payment_method ?? 'CASH') },
    variant === 'full' && { header: 'Status', cell: s => <SaleStatus sale={s} /> },
  ];
  return <DataTable rows={rows} columns={columns.filter((c): c is Column<SaleSummary> => !!c)} rowKey={s => s.id} onRowClick={s => nav(`/sales/${s.id}`)} />;
}

/** Completed, with refund markers: fully/partly refunded, or a refund waiting for approval or payment. */
function SaleStatus({ sale: s }: { sale: SaleSummary }) {
  const refunded = Number(s.refunded_xaf ?? 0);
  return (
    <div className="pill-stack">
      {refunded >= Number(s.total_xaf) && refunded > 0 ? <Pill tone="purple">Refunded</Pill> : <CompletedPill />}
      {refunded > 0 && refunded < Number(s.total_xaf) && <Pill tone="purple">Partly refunded</Pill>}
      {Number(s.open_refunds ?? 0) > 0 && <Pill tone="amber">Refund pending</Pill>}
    </div>
  );
}
