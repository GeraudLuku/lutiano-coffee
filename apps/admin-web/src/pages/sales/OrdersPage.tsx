import { useState } from 'react';
import { Bell, Coffee, PackageCheck } from 'lucide-react';
import { Link } from 'react-router-dom';
import { ordersApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { usePagedQuery } from '../../hooks/usePagedQuery';
import { DataTable, ErrorNote, FilterBar, LabelPill, ListState, Modal, PageHeader, Pager, Panel, ReasonModal, StatRow, StatCard, TitleCell } from '../../components/ui';
import { dateTime, xaf } from '../../utils/format';
import type { CustomerOrder, OrderStatus } from '../../types/models';

const STATUS: Record<OrderStatus, [string, string]> = {
  PLACED: ['New', 'blue'], ACCEPTED: ['Preparing', 'amber'], READY: ['Ready', 'purple'], COLLECTED: ['Completed', 'green'],
  REJECTED: ['Declined', 'red'], CANCELLED: ['Cancelled', 'grey'], EXPIRED: ['Expired', 'grey'],
};

/** Orders customers placed in the Customer app: who ordered, where, and where each one stands. An order still in progress can be cancelled. */
export function OrdersPage() {
  const [group, setGroup] = useState('');
  const [viewing, setViewing] = useState<string | null>(null);
  const [cancelling, setCancelling] = useState<CustomerOrder | null>(null);
  const action = useAction();
  const { data, items, summary, pager, error, loading, reload } = usePagedQuery((page, size) => ordersApi.list({ group, page, size }), [group]);

  return (
    <>
      <PageHeader title="Customer Orders" subtitle="Orders placed in the Customer app and handled by the sales points." crumbs={[['Sales'], ['Customer Orders']]} />
      <StatRow>
        <StatCard tone="blue" icon={<Bell size={20} />} label="New" value={String(summary?.placed ?? 0)} />
        <StatCard tone="amber" icon={<Coffee size={20} />} label="Preparing" value={String(summary?.accepted ?? 0)} />
        <StatCard tone="purple" icon={<PackageCheck size={20} />} label="Ready" value={String(summary?.ready ?? 0)} />
      </StatRow>
      <FilterBar>
        <select className="input" value={group} onChange={e => setGroup(e.target.value)} aria-label="Status">
          <option value="">All orders</option><option value="PENDING">In progress</option><option value="COMPLETED">Completed</option><option value="CANCELLED">Declined, cancelled or expired</option>
        </select>
      </FilterBar>
      <Panel title="Orders">
        <ErrorNote error={error} retry={reload} />
        <ListState loading={loading} data={data} items={items} empty={{ title: 'No orders yet', text: 'Orders customers place in the app appear here.' }}>
          <DataTable rows={items} rowKey={o => o.id} columns={[
            { header: 'Order', cell: o => <TitleCell title={o.reference} sub={dateTime(o.placed_at)} /> },
            { header: 'Customer', cell: o => o.customer_name },
            { header: 'Sales location', cell: o => o.operation_name },
            { header: 'Total', cell: o => xaf(o.total_xaf) },
            { header: 'Status', cell: o => <LabelPill map={STATUS} value={o.status} /> },
            { header: '', cell: o => (
              <div className="row-actions">
                <button className="btn sm" onClick={() => setViewing(o.id)}>View</button>
                {o.sale_id && <Link className="btn sm" to={`/sales/${o.sale_id}`}>Sale</Link>}
                {o.status_group === 'PENDING' && <button className="btn sm" onClick={() => setCancelling(o)}>Cancel</button>}
              </div>
            ) },
          ]} />
        </ListState>
        <Pager {...pager} />
      </Panel>
      {viewing && <OrderModal id={viewing} onClose={() => setViewing(null)} />}
      {cancelling && (
        <ReasonModal prompt={{ title: `Cancel ${cancelling.reference}`, label: 'Reason (the customer is told)', confirm: 'Cancel Order' }} busy={action.busy} error={action.error}
          onClose={() => { setCancelling(null); action.setError(''); }}
          onSubmit={async reason => { if (await action.run(() => ordersApi.cancel(cancelling.id, reason), 'Order cancelled.')) { setCancelling(null); reload(); } }} />
      )}
    </>
  );
}

function OrderModal({ id, onClose }: { id: string; onClose: () => void }) {
  const [order, setOrder] = useState<CustomerOrder | null>(null);
  const [error, setError] = useState('');
  useState(() => { ordersApi.get(id).then(setOrder).catch(e => setError(String(e?.message ?? e))); });
  return (
    <Modal title={order ? order.reference : 'Order'} onClose={onClose}>
      <ErrorNote error={error} />
      {order && (
        <div className="stack">
          <p><LabelPill map={STATUS} value={order.status} /> {order.customer_name} · {order.customer_phone} · {order.operation_name}</p>
          {order.note && <p><b>Note:</b> {order.note}</p>}
          {order.reason && <p><b>Reason:</b> {order.reason}</p>}
          <DataTable rows={order.lines ?? []} rowKey={l => String(l.line_no)} columns={[
            { header: 'Item', cell: l => <TitleCell title={`${l.quantity}× ${l.product_name}`} sub={l.addons.map(a => a.addon_name).join(', ')} /> },
            { header: 'Amount', cell: l => xaf((l.unit_price_xaf + l.addons_unit_xaf) * l.quantity) },
          ]} />
          <b>Total {xaf(order.total_xaf)}</b>
          <DataTable rows={order.timeline ?? []} rowKey={t => t.status + t.at} columns={[
            { header: 'Step', cell: t => <LabelPill map={STATUS} value={t.status} /> },
            { header: 'When', cell: t => dateTime(t.at) },
          ]} />
        </div>
      )}
    </Modal>
  );
}
