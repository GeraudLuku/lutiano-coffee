import { Link, useNavigate } from 'react-router-dom';
import { CreditCard } from 'lucide-react';
import { paymentsApi } from '../../api/services';
import { usePagedQuery } from '../../hooks/usePagedQuery';
import { DataTable, ErrorNote, Pager, Panel, TitleCell, ListState } from '../../components/ui';
import { MethodPill, PaymentStatusPill } from '../payments/PaymentBadges';
import { dateTime, xaf } from '../../utils/format';

/** Payments taken at one location, newest first. */
export function LocationPaymentsTab({ operationId }: { operationId: string }) {
  const nav = useNavigate();
  const { data, items, pager, error, loading, reload } = usePagedQuery((page, size) => paymentsApi.list({ operationId, page, size }), [operationId]);
  return (
    <Panel title="Payments at this location" icon={<CreditCard size={19} />} action={<Link className="link" to={`/payments?operation=${operationId}`}>Open in Payments</Link>}>
      <ErrorNote error={error} retry={reload} />
      <ListState loading={loading} data={data} items={items} empty={{ title: 'No payments yet' }}>
        <DataTable rows={items} rowKey={p => p.id} onRowClick={p => nav(`/payments/${p.id}`)} columns={[
          { header: 'Payment', cell: p => <TitleCell title={p.reference} sub={dateTime(p.created_at)} /> },
          { header: 'Sale', cell: p => <Link className="link" to={`/sales/${p.sale_id}`} onClick={e => e.stopPropagation()}>{p.sale_reference}</Link> },
          { header: 'Method', cell: p => <MethodPill code={p.method} label={p.method_label} /> },
          { header: 'Amount', align: 'right', cell: p => <b>{xaf(p.amount_xaf)}</b> },
          { header: 'Status', cell: p => <PaymentStatusPill status={p.status} /> },
        ]} />
      </ListState>
      <Pager {...pager} />
    </Panel>
  );
}
