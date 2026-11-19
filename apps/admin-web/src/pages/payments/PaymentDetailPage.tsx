import { Link, useParams } from 'react-router-dom';
import { History, Info, Receipt } from 'lucide-react';
import { paymentsApi } from '../../api/services';
import { useLoad } from '../../hooks/useLoad';
import { ErrorNote, Facts, Loading, Note, PageHeader, Panel } from '../../components/ui';
import { dateTime, xaf } from '../../utils/format';
import { MethodPill, PaymentStatusPill } from './PaymentBadges';
import { PaymentEventsList } from './PaymentEventsList';

export function PaymentDetailPage() {
  const { id } = useParams();
  const { data: p, error, loading, reload } = useLoad(() => paymentsApi.get(id!), [id]);
  if (loading && !p) return <Loading />;
  if (!p) return <ErrorNote error={error} retry={reload} />;
  const cash = p.method === 'CASH';
  return (
    <>
      <PageHeader title={p.reference} badge={<PaymentStatusPill status={p.status} />} subtitle={`Recorded ${dateTime(p.created_at)}`}
        crumbs={[['Payments', '/payments'], [p.reference]]} />
      {p.amount_xaf !== p.sale_total_xaf && <Note tone="warn">This payment ({xaf(p.amount_xaf)}) does not match the sale total ({xaf(p.sale_total_xaf)}). See Reconciliation.</Note>}
      <div className="grid-2">
        <Panel title="Payment Information" icon={<Info size={19} />}>
          <Facts items={[
            ['Method', <MethodPill code={p.method} label={p.method_label} />], ['Amount', <b>{xaf(p.amount_xaf)}</b>], ['Currency', p.currency],
            ['Status', <PaymentStatusPill status={p.status} />],
            ['Confirmed', p.confirmed_at ? `${dateTime(p.confirmed_at)}${p.confirmed_by_name ? ` by ${p.confirmed_by_name}` : ''}` : 'Not confirmed'],
            ...(cash
              ? [['Cash received', p.cash_received_xaf == null ? '—' : xaf(p.cash_received_xaf)], ['Change given', p.change_xaf == null ? '—' : xaf(p.change_xaf)]] as [string, string][]
              : [['Provider reference', p.provider_reference ?? '—'], ['Payer account', p.payer_account ?? '—'], ['Merchant account', p.merchant_account ?? '—']] as [string, string][]),
          ]} />
        </Panel>
        <Panel title="Sale" icon={<Receipt size={19} />}>
          <Facts items={[
            ['Sale', <Link className="link" to={`/sales/${p.sale_id}`}>{p.sale_reference}</Link>], ['Sale total', xaf(p.sale_total_xaf)],
            ['Sales location', <Link className="link" to={`/locations/${p.operation_id}`}>{p.operation_name} ({p.operation_code})</Link>],
            ['Seller', <Link className="link" to={`/users/${p.seller_id}`}>{p.seller_name}</Link>],
          ]} />
        </Panel>
      </div>
      <Panel title="Payment History" icon={<History size={19} />}><PaymentEventsList events={p.events ?? []} /></Panel>
    </>
  );
}
