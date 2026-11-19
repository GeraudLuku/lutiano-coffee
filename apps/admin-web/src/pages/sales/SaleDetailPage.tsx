import { Link, useNavigate, useParams } from 'react-router-dom';
import { useState } from 'react';
import { Banknote, Calendar, Coins, CreditCard, Info, Printer, Receipt, RotateCcw, Star, Store, User, Wallet } from 'lucide-react';
import { salesApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { useLoad } from '../../hooks/useLoad';
import { DataTable, ErrorNote, Facts, Loading, PageHeader, Panel, Pill, StatCard, StatRow, TitleCell } from '../../components/ui';
import { CompletedPill, ProductThumb } from '../../components/domain';
import { dateTime, xaf } from '../../utils/format';
import { saveFile } from '../../utils/files';
import { TYPE_LABEL } from '../../utils/labels';
import { useAuth } from '../../auth/AuthContext';
import { MethodPill, PaymentStatusPill } from '../payments/PaymentBadges';
import { PaymentEventsList } from '../payments/PaymentEventsList';
import { CreateRefundModal } from '../refunds/CreateRefundModal';
import { RefundStatusPill } from '../refunds/RefundStatusPill';
import { REFUND_REASON } from '../../utils/refunds';
import { rateLabel } from '../../utils/earnings';

const SALE_PAYMENT_STATUS: Record<string, [string, string]> = {
  CONFIRMED: ['Confirmed', 'green'], PENDING: ['Awaiting confirmation', 'amber'],
};

export function SaleDetailPage() {
  const { id } = useParams();
  const { data: s, error, loading, reload } = useLoad(() => salesApi.get(id!), [id]);
  const receipt = useAction();
  const { can } = useAuth();
  const nav = useNavigate();
  const [refunding, setRefunding] = useState(false);
  if (loading && !s) return <Loading />;
  if (!s) return <ErrorNote error={error} retry={reload} />;
  const committed = s.refunds.filter(r => r.status !== 'REJECTED' && r.status !== 'CANCELLED').reduce((n, r) => n + Number(r.amount_xaf), 0);
  const refundable = Number(s.total_xaf) - committed;
  const printReceipt = () => receipt.run(async () => saveFile(await salesApi.receipt(s.id), `receipt-${s.reference}.pdf`));

  return (
    <>
      <PageHeader title={s.reference} badge={<CompletedPill />} subtitle={`Created on ${dateTime(s.created_at)}`}
        crumbs={[['Sales Transactions', '/sales'], [s.reference]]}
        actions={<>
          <button className="btn" onClick={printReceipt} disabled={receipt.busy}><Printer size={16} />Print Receipt</button>
          {can('ADMIN') && (
            <button className="btn gold" onClick={() => setRefunding(true)} disabled={refundable <= 0} title={refundable <= 0 ? 'This sale has been fully refunded or has refunds pending for the full amount.' : undefined}>
              <RotateCcw size={16} />Refund Sale</button>
          )}
        </>} />
      <ErrorNote error={receipt.error} />
      <div className="grid-sale">
        <Panel title="Transaction Information">
          <Facts withIcons items={[
            [<><Store size={15} />Sales Location</>, <><Link className="link" to={`/locations/${s.operation_id}`}>{s.operation_name}</Link> ({s.operation_code})</>],
            [<><User size={15} />Seller</>, s.seller_name],
            [<><Info size={15} />Channel</>, s.operation_type ? TYPE_LABEL[s.operation_type] : '—'],
            [<><User size={15} />Customer</>, s.customer_id
              ? <>{can('ADMIN') ? <Link className="link" to={`/loyalty/members/${s.customer_id}`}>{s.customer_name}</Link> : s.customer_name} ({s.customer_code})</>
              : <span className="muted">Anonymous sale</span>],
            ...(s.loyalty ? [[<><Star size={15} />Loyalty Points</>, <span title={s.loyalty.breakdown ?? undefined}>
              <b className="ok">+{s.loyalty.total_points} pts</b>{s.loyalty.reversed_points ? <small className="block muted">{s.loyalty.reversed_points} pts reversed by refunds</small> : null}
              {s.loyalty.breakdown && <small className="block muted">{s.loyalty.breakdown}</small>}</span>] as [JSX.Element, JSX.Element]] : []),
            [<><Wallet size={15} />Payment Method</>, s.payments.length ? <>{s.payments.map(p => <MethodPill key={p.id} code={p.method} label={p.method_label} />)}</> : '—'],
            [<><Receipt size={15} />Sale Status</>, <CompletedPill />],
            [<><Banknote size={15} />Payment Status</>, <Pill tone={(SALE_PAYMENT_STATUS[s.payment_status] ?? [s.payment_status, 'grey'])[1]}>{(SALE_PAYMENT_STATUS[s.payment_status] ?? [s.payment_status])[0]}</Pill>],
          ]} />
        </Panel>
        <Panel title={`Items (${s.lines.length})`}>
          <DataTable rows={s.lines} rowKey={l => String(l.line_no)} columns={[
            { header: 'Product', cell: l => <TitleCell lead={<ProductThumb url={l.image_url} size={38} />} title={l.product_name}
                sub={<>{l.sku}{l.addons?.map(a => <span key={a.addon_id} className="line-addons">+ {a.addon_name} ({xaf(a.unit_price_xaf)})</span>)}</>} /> },
            { header: 'Unit Price', align: 'right', cell: l => (l.addons_unit_xaf ? <>{xaf(l.unit_price_xaf + l.addons_unit_xaf)}<small className="block">incl. add-ons</small></> : xaf(l.unit_price_xaf)) },
            { header: 'Qty', align: 'right', cell: l => l.quantity },
            { header: 'Total', align: 'right', cell: l => xaf(l.line_total_xaf) },
          ]} />
          <div className="totals">
            <div><span>Subtotal</span><b>{xaf(s.total_xaf)}</b></div>
            <div className="grand"><span>Total Amount</span><b>{xaf(s.total_xaf)}</b></div>
          </div>
        </Panel>
      </div>
      <StatRow>
        <StatCard tone="green" icon={<Banknote size={20} />} value={s.cash_received_xaf != null ? xaf(s.cash_received_xaf) : '—'} label="Cash Received" />
        <StatCard tone="amber" icon={<Wallet size={20} />} value={s.change_xaf != null ? xaf(s.change_xaf) : '—'} label="Change Given" />
        <StatCard tone="blue" icon={<Calendar size={20} />} value={dateTime(s.created_at)} label="Recorded (Douala time)" />
        <StatCard tone="purple" icon={<Receipt size={20} />} value={s.lines.reduce((n, l) => n + l.quantity, 0)} label="Units" />
      </StatRow>
      <Panel title={`Payments (${s.payments.length})`} icon={<CreditCard size={19} />}>
        {s.payments.length === 0 ? <p className="muted">No payment recorded for this sale.</p> : s.payments.map(p => (
          <div key={p.id} className="grid-2">
            <Facts items={[
              ['Payment', can('ADMIN') ? <Link className="link" to={`/payments/${p.id}`}>{p.reference}</Link> : p.reference],
              ['Method', <MethodPill code={p.method} label={p.method_label} />], ['Amount', xaf(p.amount_xaf)], ['Status', <PaymentStatusPill status={p.status} />],
              ['Confirmed', p.confirmed_at ? `${dateTime(p.confirmed_at)}${p.confirmed_by_name ? ` by ${p.confirmed_by_name}` : ''}` : 'Not confirmed'],
            ]} />
            <PaymentEventsList events={p.events ?? []} />
          </div>
        ))}
      </Panel>
      {can('ADMIN') && s.commissions.length > 0 && (
        <Panel title={`Seller Commission · ${xaf(s.commissions.reduce((n, c) => n + Number(c.commission_xaf), 0))}`} icon={<Coins size={19} />}>
          <DataTable rows={s.commissions} rowKey={c => String(c.line_no)} columns={[
            { header: 'Product', cell: c => c.product_name },
            { header: 'Line value', align: 'right', cell: c => xaf(c.line_value_xaf) },
            { header: 'Rate (at sale time)', cell: c => rateLabel(c.basis, c.rate) },
            { header: 'Commission', align: 'right', cell: c => <TitleCell title={<b>{xaf(c.commission_xaf)}</b>} sub={c.reversed_xaf ? `${xaf(c.reversed_xaf)} reversed by refunds` : undefined} /> },
          ]} />
        </Panel>
      )}
      {s.refunds.length > 0 && (
        <Panel title={`Refunds (${s.refunds.length})`} icon={<RotateCcw size={19} />}>
          <DataTable rows={s.refunds} rowKey={r => r.id} onRowClick={can('ADMIN') ? r => nav(`/sales/refunds/${r.id}`) : undefined} columns={[
            { header: 'Refund', cell: r => <TitleCell title={r.reference} sub={dateTime(r.requested_at)} /> },
            { header: 'Reason', cell: r => REFUND_REASON[r.reason] },
            { header: 'Amount', align: 'right', cell: r => <b>{xaf(r.amount_xaf)}</b> },
            { header: 'Status', cell: r => <RefundStatusPill status={r.status} /> },
          ]} />
          <p className="muted small">Refunds do not change this sale record or its stock. Returned goods are recorded on the refund.</p>
        </Panel>
      )}
      {refunding && <CreateRefundModal saleId={s.id} onClose={() => setRefunding(false)} onDone={r => nav(`/sales/refunds/${r.id}`)} />}
      <p className="muted small">Line prices are snapshots taken at the time of sale; later price changes do not alter this record.</p>
    </>
  );
}
