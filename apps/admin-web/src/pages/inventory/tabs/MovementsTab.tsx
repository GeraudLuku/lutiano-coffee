import { useState } from 'react';
import { Download } from 'lucide-react';
import { inventoryApi } from '../../../api/services';
import { DataTable, ErrorNote, FilterBar, Pager, Panel, SelectFilter, TitleCell, ListState } from '../../../components/ui';
import { usePagedQuery } from '../../../hooks/usePagedQuery';
import { useAction } from '../../../hooks/useAction';
import { fetchAll } from '../../../api/pages';
import { dateTime, doualaDate } from '../../../utils/format';
import { saveFile, toCsv } from '../../../utils/files';
import { CENTRAL, MOVEMENT_TYPE } from '../../../utils/inventoryLabels';
import type { Operation, Product } from '../../../types/models';
import { Delta, MovementTypePill } from '../InventoryBadges';

/** The stock ledger: every change to every balance, newest first. */
export function MovementsTab({ operations, products, version }: { operations: Operation[]; products: Product[]; version: number }) {
  const [location, setLocation] = useState('');
  const [productId, setProductId] = useState('');
  const [type, setType] = useState('');
  const [from, setFrom] = useState(doualaDate(-29));
  const [to, setTo] = useState(doualaDate());
  const filters = { location: location || undefined, productId: productId || undefined, type: type || undefined, from, to };
  const { data, items, error, loading, reload, pager } = usePagedQuery((page, size) => inventoryApi.ledger({ ...filters, page, size }),
    [location, productId, type, from, to, version]);
  const exporting = useAction();

  const exportCsv = () => exporting.run(async () => saveFile(toCsv((await fetchAll((page, size) => inventoryApi.ledger({ ...filters, page, size }))).map(m => ({ ...m, created_at: dateTime(m.created_at), movement_type: MOVEMENT_TYPE[m.movement_type]?.[0] ?? m.movement_type })),
    [['created_at', 'Date & time (Douala)'], ['location_name', 'Location'], ['product_name', 'Product'], ['movement_type', 'Type'],
      ['quantity_delta', 'Change'], ['balance_after', 'Balance after'], ['reference_label', 'Reference'], ['actor_name', 'By'], ['note', 'Note']]),
    `lutiano-stock-movements-${from}-to-${to}.csv`));

  return (
    <Panel>
      <FilterBar labelled>
        <label><span>From</span><input className="input" type="date" value={from} max={to} onChange={e => setFrom(e.target.value)} /></label>
        <label><span>To</span><input className="input" type="date" value={to} min={from} onChange={e => setTo(e.target.value)} /></label>
        <label><span>Location</span><SelectFilter label="Location" value={location} onChange={setLocation} allLabel="All locations" options={[[CENTRAL, 'Central stock'], ...operations.map(o => [o.id, o.name] as [string, string])]} /></label>
        <label><span>Product</span><SelectFilter label="Product" value={productId} onChange={setProductId} allLabel="All products" options={products.map(p => [p.id, p.name])} /></label>
        <label><span>Type</span><SelectFilter label="Type" value={type} onChange={setType} allLabel="All types" options={Object.entries(MOVEMENT_TYPE).map(([k, [l]]) => [k, l])} /></label>
        <button className="btn" onClick={exportCsv} disabled={!data?.total || exporting.busy}><Download size={16} />{exporting.busy ? 'Exporting…' : 'Export'}</button>
      </FilterBar>
      <ErrorNote error={error} retry={reload} />
      <ListState loading={loading} data={data} items={items} empty={{ title: 'No stock movements in this period' }}>
        <DataTable rows={items} rowKey={m => m.id} columns={[
          { header: 'Date & Time', cell: m => dateTime(m.created_at) },
          { header: 'Location', cell: m => m.location_name },
          { header: 'Product', cell: m => <TitleCell title={m.product_name} sub={m.sku} /> },
          { header: 'Type', cell: m => <MovementTypePill type={m.movement_type} /> },
          { header: 'Change', align: 'right', cell: m => <Delta value={m.quantity_delta} /> },
          { header: 'Balance after', align: 'right', cell: m => m.balance_after ?? '—' },
          { header: 'Reference', cell: m => <TitleCell title={m.reference_label ?? '—'} sub={m.note ?? undefined} /> },
          { header: 'By', cell: m => m.actor_name ?? 'System' },
        ]} />
      </ListState>
      <Pager {...pager} />
    </Panel>
  );
}
