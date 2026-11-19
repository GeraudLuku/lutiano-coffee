import { Link, useNavigate } from 'react-router-dom';
import { DataTable, LabelPill, TitleCell } from '../../components/ui';
import { dateTime, num, signedXaf, xaf } from '../../utils/format';
import { OUTFLOW_CATEGORY, OUTFLOW_STATUS, REMITTANCE_METHOD, REMITTANCE_STATUS, REVIEW_STATUS } from '../../utils/cash';
import type { CashOutflow, CashRemittance, CashShift } from '../../types/models';


export function ShiftsTable({ rows }: { rows: CashShift[] }) {
  const nav = useNavigate();
  return (
    <DataTable rows={rows} rowKey={s => s.id} onRowClick={s => nav(`/sales/shifts/${s.id}`)} columns={[
      { header: 'Seller', cell: s => <TitleCell title={s.seller_name} sub={`Opened ${dateTime(s.opened_at)}`} /> },
      { header: 'Closed', cell: s => (s.closed_at ? dateTime(s.closed_at) : <LabelPill map={{ OPEN: ['Open', 'green'] }} value="OPEN" />) },
      { header: 'Float', align: 'right', cell: s => xaf(s.opening_float) },
      { header: 'Cash Sales', align: 'right', cell: s => <TitleCell title={xaf(s.cash_sales_xaf ?? 0)} sub={`${num(s.sale_count)} sales`} /> },
      { header: 'Out', align: 'right', cell: s => {
        const out = Number(s.refunds_paid_xaf ?? 0) + Number(s.outflows_xaf ?? 0) + Number(s.remitted_xaf ?? 0);
        return out ? <TitleCell title={xaf(out)} sub="refunds, outflows, remittances" /> : '—';
      } },
      { header: 'Expected', align: 'right', cell: s => <b>{xaf(s.expected_cash)}</b> },
      { header: 'Counted', align: 'right', cell: s => (s.counted_cash == null ? '—' : xaf(s.counted_cash)) },
      { header: 'Variance', align: 'right', cell: s => (s.counted_cash == null ? '—'
        : <span className={Number(s.variance) ? 'warn' : ''}>{signedXaf(Number(s.counted_cash) - Number(s.expected_cash))}</span>) },
      { header: 'Review', cell: s => (s.review_status ? <LabelPill map={REVIEW_STATUS} value={s.review_status} /> : '—') },
    ]} />
  );
}

export function OutflowsTable({ rows, actions }: { rows: CashOutflow[]; actions?: (o: CashOutflow) => React.ReactNode }) {
  return (
    <DataTable rows={rows} rowKey={o => o.id} columns={[
      { header: 'Outflow', cell: o => <TitleCell title={o.reference} sub={dateTime(o.requested_at)} /> },
      { header: 'Seller', cell: o => <Link className="link" to={`/sales/shifts/${o.shift_id}`}>{o.seller_name}</Link> },
      { header: 'Purpose', cell: o => <TitleCell title={OUTFLOW_CATEGORY[o.category] ?? o.category} sub={o.description} /> },
      { header: 'Amount', align: 'right', cell: o => <b>{xaf(o.amount_xaf)}</b> },
      { header: 'Status', cell: o => <TitleCell title={<LabelPill map={OUTFLOW_STATUS} value={o.status} />}
          sub={o.decided_at ? `${o.decided_by_name ?? ''} ${dateTime(o.decided_at)}${o.decision_note ? ` · ${o.decision_note}` : ''}` : undefined} /> },
      ...(actions ? [{ header: '', cell: actions }] : []),
    ]} />
  );
}

export function RemittancesTable({ rows, actions }: { rows: CashRemittance[]; actions?: (r: CashRemittance) => React.ReactNode }) {
  return (
    <DataTable rows={rows} rowKey={r => r.id} columns={[
      { header: 'Remittance', cell: r => <TitleCell title={r.reference} sub={dateTime(r.recorded_at)} /> },
      { header: 'Seller', cell: r => <Link className="link" to={`/sales/shifts/${r.shift_id}`}>{r.seller_name}</Link> },
      { header: 'To', cell: r => <TitleCell title={REMITTANCE_METHOD[r.method]} sub={r.method === 'HANDOVER' ? r.recipient_name : `Slip ${r.bank_reference}`} /> },
      { header: 'Sent', align: 'right', cell: r => <b>{xaf(r.amount_xaf)}</b> },
      { header: 'Received', align: 'right', cell: r => (r.received_xaf == null ? '—'
        : <span className={Number(r.received_xaf) !== Number(r.amount_xaf) ? 'warn' : ''}>{xaf(r.received_xaf)}</span>) },
      { header: 'Status', cell: r => <TitleCell title={<LabelPill map={REMITTANCE_STATUS} value={r.status} />}
          sub={r.acknowledged_at ? `${r.acknowledged_by_name ?? ''} ${dateTime(r.acknowledged_at)}${r.acknowledgement_note ? ` · ${r.acknowledgement_note}` : ''}` : undefined} /> },
      ...(actions ? [{ header: '', cell: actions }] : []),
    ]} />
  );
}
