import { useState } from 'react';
import { adjustmentsApi } from '../../../api/services';
import { DataTable, ErrorNote, FilterBar, Pager, Panel, SelectFilter, TitleCell, ListState } from '../../../components/ui';
import { usePagedQuery } from '../../../hooks/usePagedQuery';
import { dateTime } from '../../../utils/format';
import { ADJUSTMENT_REASON, ADJUSTMENT_STATUS, CENTRAL } from '../../../utils/inventoryLabels';
import type { AdjustmentStatus, Operation } from '../../../types/models';
import { AdjustmentActions } from '../ApprovalButtons';
import { AdjustmentStatusPill, Delta } from '../InventoryBadges';
import type { InventoryActions } from '../useInventoryActions';

export function AdjustmentsTab({ operations, actions, version }: { operations: Operation[]; actions: InventoryActions; version: number }) {
  const [status, setStatus] = useState('');
  const [location, setLocation] = useState('');
  const { data, items, error, loading, reload, pager } = usePagedQuery(
    (page, size) => adjustmentsApi.list({ status: (status || undefined) as AdjustmentStatus | undefined, location: location || undefined, page, size }), [status, location, version]);
  return (
    <Panel>
      <FilterBar>
        <SelectFilter label="Status" value={status} onChange={setStatus} allLabel="All statuses" options={Object.entries(ADJUSTMENT_STATUS).map(([k, [l]]) => [k, l])} />
        <SelectFilter label="Location" value={location} onChange={setLocation} allLabel="All locations" options={[[CENTRAL, 'Central stock'], ...operations.map(o => [o.id, o.name] as [string, string])]} />
      </FilterBar>
      <ErrorNote error={error} retry={reload} />
      <ListState loading={loading} data={data} items={items} empty={{ title: 'No adjustments', text: 'Damaged, expired, missing or found stock and returns appear here.' }}>
        <DataTable rows={items} rowKey={a => a.id} columns={[
          { header: 'Requested', cell: a => <TitleCell title={dateTime(a.created_at)} sub={a.requested_by_name ?? undefined} /> },
          { header: 'Location', cell: a => a.location_name },
          { header: 'Product', cell: a => <TitleCell title={a.product_name} sub={a.sku} /> },
          { header: 'Change', align: 'right', cell: a => <Delta value={a.quantity_delta} /> },
          { header: 'Reason', cell: a => <TitleCell title={ADJUSTMENT_REASON[a.reason][0]} sub={a.note} /> },
          { header: 'Status', cell: a => <TitleCell title={<AdjustmentStatusPill status={a.status} />} sub={[a.decided_by_name, a.decision_note].filter(Boolean).join(': ') || undefined} /> },
          { header: '', align: 'right', cell: a => <AdjustmentActions a={a} actions={actions} /> },
        ]} />
      </ListState>
      <Pager {...pager} />
    </Panel>
  );
}
