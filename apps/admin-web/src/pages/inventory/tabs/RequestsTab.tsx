import { useState } from 'react';
import { Link } from 'react-router-dom';
import { stockRequestsApi } from '../../../api/services';
import { DataTable, ErrorNote, FilterBar, Pager, Panel, SelectFilter, TitleCell, ListState } from '../../../components/ui';
import { usePagedQuery } from '../../../hooks/usePagedQuery';
import { date, dateTime, num } from '../../../utils/format';
import { TRANSFER_STATUS } from '../../../utils/inventoryLabels';
import type { Allocation, Operation, StockRequest, TransferStatus } from '../../../types/models';
import { RequestActions } from '../ApprovalButtons';
import { TransferStatusPill } from '../InventoryBadges';
import type { InventoryActions } from '../useInventoryActions';

/** Stock requests asked for by sellers (and staff): one row per request with its products, decided as a whole. */
export function RequestsTab({ operations, actions, version, onConfirm }: {
  operations: Operation[]; actions: InventoryActions; version: number; onConfirm: (t: Allocation) => void;
}) {
  const [status, setStatus] = useState('');
  const [op, setOp] = useState('');
  const { data, items, error, loading, reload, pager } = usePagedQuery(
    (page, size) => stockRequestsApi.list({ status: (status || undefined) as TransferStatus | undefined, operationId: op || undefined, page, size }), [status, op, version]);
  return (
    <Panel>
      <FilterBar>
        <SelectFilter label="Status" value={status} onChange={setStatus} allLabel="All statuses" options={Object.entries(TRANSFER_STATUS).map(([k, [l]]) => [k, l])} />
        <SelectFilter label="Sales location" value={op} onChange={setOp} allLabel="All locations" options={operations.map(o => [o.id, o.name])} />
      </FilterBar>
      <ErrorNote error={error} retry={reload} />
      <ListState loading={loading} data={data} items={items} empty={{ title: 'No stock requests', text: 'Requests sent from the Seller app appear here.' }}>
        <DataTable rows={items} rowKey={r => r.id} columns={[
          { header: 'Request', cell: r => <TitleCell title={r.reference} sub={`${dateTime(r.created_at)} · ${r.requested_by_name ?? ''}`} /> },
          { header: 'To', cell: r => <TitleCell title={<Link className="link" to={`/locations/${r.operation_id}`}>{r.operation_name}</Link>} sub={r.needed_by ? `needed by ${date(r.needed_by)}` : 'as soon as possible'} /> },
          { header: 'Products', cell: r => <RequestItems r={r} onConfirm={onConfirm} /> },
          { header: 'Units', align: 'right', cell: r => num(r.total_quantity) },
          { header: 'Status', cell: r => <TitleCell title={<TransferStatusPill status={r.status} />} sub={trail(r)} /> },
          { header: '', align: 'right', cell: r => <RequestActions r={r} actions={actions} /> },
        ]} />
      </ListState>
      <Pager {...pager} />
    </Panel>
  );
}

/** The products with their quantities; each product on its way can be confirmed (the recipient counts every delivery). */
function RequestItems({ r, onConfirm }: { r: StockRequest; onConfirm: (t: Allocation) => void }) {
  return (
    <div className="stack" style={{ gap: 4 }}>
      {r.items.map(i => (
        <div key={i.id} className="row-actions" style={{ justifyContent: 'flex-start' }}>
          <span>{num(i.sent_quantity)} × {i.product_name}</span>
          {r.status === 'IN_TRANSIT' && i.status === 'IN_TRANSIT' && <button className="btn sm" onClick={() => onConfirm(i)}>Confirm receipt</button>}
          {i.status === 'RECEIVED' && <small className={i.received_quantity !== i.sent_quantity ? 'warn' : ''}>received {num(i.received_quantity ?? 0)}</small>}
          {r.status === 'PENDING_APPROVAL' && i.central_quantity < i.sent_quantity && <small className="warn">only {num(i.central_quantity)} in central stock</small>}
        </div>
      ))}
      {r.reason && <small>{r.reason}</small>}
    </div>
  );
}

function trail(r: StockRequest): string | undefined {
  switch (r.status) {
    case 'APPROVED': return r.approved_by_name ? `by ${r.approved_by_name}` : undefined;
    case 'REJECTED': case 'CANCELLED': return [r.closed_by_name, r.decision_note].filter(Boolean).join(': ') || undefined;
    default: return undefined;
  }
}
