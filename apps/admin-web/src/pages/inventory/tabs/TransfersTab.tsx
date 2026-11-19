import { useState } from 'react';
import { Link } from 'react-router-dom';
import { transfersApi } from '../../../api/services';
import { DataTable, ErrorNote, FilterBar, Pager, Panel, SelectFilter, TitleCell, ListState } from '../../../components/ui';
import { usePagedQuery } from '../../../hooks/usePagedQuery';
import { dateTime, num } from '../../../utils/format';
import { TRANSFER_STATUS } from '../../../utils/inventoryLabels';
import type { Allocation, Operation, TransferStatus } from '../../../types/models';
import { TransferActions } from '../ApprovalButtons';
import { TransferStatusPill } from '../InventoryBadges';
import type { InventoryActions } from '../useInventoryActions';

/** Every transfer with its full trail (requested → approved → dispatched → received). */
export function TransfersTab({ operations, actions, version, onConfirm }: {
  operations: Operation[]; actions: InventoryActions; version: number; onConfirm: (t: Allocation) => void;
}) {
  const [status, setStatus] = useState('');
  const [op, setOp] = useState('');
  const { data, items, error, loading, reload, pager } = usePagedQuery(
    (page, size) => transfersApi.list({ status: (status || undefined) as TransferStatus | undefined, operationId: op || undefined, page, size }), [status, op, version]);
  return (
    <Panel>
      <FilterBar>
        <SelectFilter label="Status" value={status} onChange={setStatus} allLabel="All statuses" options={Object.entries(TRANSFER_STATUS).map(([k, [l]]) => [k, l])} />
        <SelectFilter label="Sales location" value={op} onChange={setOp} allLabel="All locations" options={operations.map(o => [o.id, o.name])} />
      </FilterBar>
      <ErrorNote error={error} retry={reload} />
      <ListState loading={loading} data={data} items={items} empty={{ title: 'No transfers' }}>
        <DataTable rows={items} rowKey={t => t.id} columns={[
          { header: 'Requested', cell: t => <TitleCell title={dateTime(t.created_at)} sub={t.created_by_name ?? undefined} /> },
          { header: 'Product', cell: t => <TitleCell title={t.product_name} sub={t.request_note ?? t.sku} /> },
          { header: 'To', cell: t => <Link className="link" to={`/locations/${t.operation_id}`}>{t.operation_name}</Link> },
          { header: 'Sent', align: 'right', cell: t => num(t.sent_quantity) },
          { header: 'Received', align: 'right', cell: t => (t.received_quantity == null ? '—' : <span className={t.received_quantity !== t.sent_quantity ? 'warn' : ''}>{num(t.received_quantity)}</span>) },
          { header: 'Status', cell: t => <TitleCell title={<TransferStatusPill status={t.status} />} sub={trail(t)} /> },
          { header: '', align: 'right', cell: t => t.status === 'IN_TRANSIT'
            ? <button className="btn sm" onClick={() => onConfirm(t)}>Confirm receipt</button>
            : <TransferActions t={t} actions={actions} /> },
        ]} />
      </ListState>
      <Pager {...pager} />
    </Panel>
  );
}

/** Who did the latest step, e.g. "Approved by Inventory Manager". */
function trail(t: Allocation): string | undefined {
  switch (t.status) {
    case 'APPROVED': return t.approved_by_name ? `by ${t.approved_by_name}` : undefined;
    case 'IN_TRANSIT': return t.dispatched_by_name ? `dispatched by ${t.dispatched_by_name}` : undefined;
    case 'RECEIVED': return t.confirmed_by_name ? `confirmed by ${t.confirmed_by_name}` : undefined;
    case 'REJECTED': case 'CANCELLED': return [t.closed_by_name, t.decision_note].filter(Boolean).join(': ') || undefined;
    default: return undefined;
  }
}
