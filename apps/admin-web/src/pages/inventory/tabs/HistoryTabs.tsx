import { DataTable, Pager, Panel, Pill, TitleCell, ListState } from '../../../components/ui';
import { usePagedQuery } from '../../../hooks/usePagedQuery';
import { inventoryApi } from '../../../api/services';
import { ErrorNote } from '../../../components/ui';
import { dateTime, num } from '../../../utils/format';
import { RECEIPT_SOURCE } from '../../../utils/inventoryLabels';

export function ReceiptsTab({ version }: { version: number }) {
  const { data, items, pager, error, loading, reload } = usePagedQuery((page, size) => inventoryApi.receipts({ page, size }), [version]);
  return (
    <Panel>
      <ErrorNote error={error} retry={reload} />
      <ListState loading={loading} data={data} items={items} empty={{ title: 'No receipts recorded' }}>
        <DataTable rows={items} rowKey={r => r.id} columns={[
          { header: 'Date', cell: r => dateTime(r.created_at) },
          { header: 'Product', cell: r => <TitleCell title={r.product_name} sub={r.sku} /> },
          { header: 'Type', cell: r => <Pill tone={r.source === 'SUPPLIER' ? 'green' : 'grey'}>{RECEIPT_SOURCE[r.source] ?? r.source}</Pill> },
          { header: 'Supplier / reference', cell: r => <TitleCell title={r.supplier ?? '—'} sub={r.reference ?? undefined} /> },
          { header: 'Quantity', align: 'right', cell: r => `+${num(r.quantity)}` },
          { header: 'Recorded By', cell: r => <TitleCell title={r.actor_name ?? '—'} sub={r.note ?? undefined} /> },
        ]} />
      </ListState>
      <Pager {...pager} />
    </Panel>
  );
}

export function DiscrepanciesTab({ version }: { version: number }) {
  const { data, items, pager, error, loading, reload } = usePagedQuery((page, size) => inventoryApi.discrepancies({ page, size }), [version]);
  return (
    <Panel>
      <ErrorNote error={error} retry={reload} />
      <ListState loading={loading} data={data} items={items} empty={{ title: 'No discrepancies', text: 'Every confirmed transfer matched the dispatched quantity.' }}>
        <DataTable rows={items} rowKey={d => d.id} columns={[
          { header: 'Date', cell: d => dateTime(d.created_at) },
          { header: 'Product', cell: d => <b>{d.product_name}</b> },
          { header: 'Location', cell: d => d.operation_name },
          { header: 'Sent', align: 'right', cell: d => num(d.sent_quantity) },
          { header: 'Received', align: 'right', cell: d => num(d.received_quantity) },
          { header: 'Difference', align: 'right', className: 'warn', cell: d => num(d.received_quantity - d.sent_quantity) },
          { header: 'Reason', cell: d => <TitleCell title={d.note ?? '—'} sub={d.confirmed_by_name ?? undefined} /> },
        ]} />
      </ListState>
      <Pager {...pager} />
    </Panel>
  );
}
