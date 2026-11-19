import { Link } from 'react-router-dom';
import { History, Layers, Plus, Store, Truck } from 'lucide-react';
import { DataTable, Empty, ErrorNote, Pager, Panel, Pill, TitleCell } from '../../../components/ui';
import { staffApi } from '../../../api/services';
import { usePagedQuery } from '../../../hooks/usePagedQuery';
import { StatusPill, StockPill, TypePill } from '../../../components/domain';
import { dateTime, num } from '../../../utils/format';
import { SELLER_ROLE_CHANNEL, TYPE_LABEL } from '../../../utils/labels';
import type { StaffDetail } from '../../../types/models';
import { TransferStatusPill } from '../../inventory/InventoryBadges';

/** Current assignments, assignment history, stock held at the seller's locations and deliveries on the way. */
export function SellerLocationsTab({ user, busy, onAssign, onRemove }: {
  user: StaffDetail; busy: boolean; onAssign: () => void; onRemove: (operationId: string) => void;
}) {
  const history = usePagedQuery((page, size) => staffApi.assignmentHistory(user.id, { page, size }), [user.id, user.operations.length]);
  const channel = user.seller_role ? SELLER_ROLE_CHANNEL[user.seller_role] : null;
  return (
    <div className="stack">
      <Panel title="Assigned Sales Locations" icon={<Store size={19} />}
        action={user.active && <button className="btn gold sm" onClick={onAssign}><Plus size={16} />Assign Location</button>}>
        {channel && <p className="muted small">This seller's role works {TYPE_LABEL[channel]} locations.</p>}
        {user.operations.length === 0 ? <Empty title="No locations assigned" text="Assign a location so this seller can record sales." /> : (
          <DataTable rows={user.operations} rowKey={o => o.id} columns={[
            { header: 'Location', cell: o => <TitleCell title={<Link className="link" to={`/locations/${o.id}`}>{o.name}</Link>} sub={o.code} /> },
            { header: 'Type', cell: o => <>{<TypePill type={o.type} />}{!o.channel_matches && <Pill tone="red">Not this seller's channel</Pill>}</> },
            { header: 'Status', cell: o => <StatusPill active={o.active} /> },
            { header: '', align: 'right', cell: o => <button className="btn sm" disabled={busy} onClick={() => onRemove(o.id)}>Remove</button> },
          ]} />
        )}
      </Panel>
      <div className="grid-2">
        <Panel title="Stock at these locations" icon={<Layers size={19} />}>
          {!user.assignedStock?.length ? <p className="muted pad">No stock held at this seller's locations.</p> : (
            <DataTable rows={user.assignedStock} rowKey={s => s.operation_id + s.product_id} columns={[
              { header: 'Location', cell: s => s.operation_name },
              { header: 'Product', cell: s => <TitleCell title={s.product_name} sub={s.sku} /> },
              { header: 'Available', align: 'right', cell: s => num(s.quantity) },
              { header: 'Status', cell: s => <StockPill quantity={s.quantity} threshold={s.low_stock_threshold} /> },
            ]} />
          )}
        </Panel>
        <Panel title="Stock on its way" icon={<Truck size={19} />} action={<Link className="link" to="/inventory?tab=transfers">Transfers</Link>}>
          {!user.incomingStock?.length ? <p className="muted pad">Nothing requested or in transit.</p> : (
            <DataTable rows={user.incomingStock} rowKey={t => t.id} columns={[
              { header: 'Requested', cell: t => dateTime(t.created_at) },
              { header: 'Product', cell: t => <TitleCell title={t.product_name} sub={t.operation_name} /> },
              { header: 'Qty', align: 'right', cell: t => num(t.sent_quantity) },
              { header: 'Status', cell: t => <TransferStatusPill status={t.status} /> },
            ]} />
          )}
        </Panel>
      </div>
      <Panel title="Assignment History" icon={<History size={19} />}>
        <ErrorNote error={history.error} retry={history.reload} />
        {!history.items.length ? !history.loading && <p className="muted pad">No assignment changes yet.</p> : (
          <DataTable rows={history.items} rowKey={h => h.id} columns={[
            { header: 'When', cell: h => dateTime(h.created_at) },
            { header: 'Change', cell: h => (h.action === 'ASSIGNED' ? <Pill tone="green">Assigned</Pill> : <Pill tone="grey">Removed</Pill>) },
            { header: 'Location', cell: h => <TitleCell title={h.operation_name} sub={h.operation_code} /> },
            { header: 'By', cell: h => <TitleCell title={h.actor_name ?? 'System'} sub={h.note ?? undefined} /> },
          ]} />
        )}
        <Pager {...history.pager} />
      </Panel>
    </div>
  );
}
