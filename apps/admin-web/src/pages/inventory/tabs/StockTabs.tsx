import { useState } from 'react';
import { Link } from 'react-router-dom';
import { DataTable, Empty, FilterBar, Pager, Panel, SearchInput, SelectFilter, TitleCell } from '../../../components/ui';
import { CentralStockTable, StockPill } from '../../../components/domain';
import { usePaged } from '../../../hooks/usePaged';
import { num } from '../../../utils/format';
import { matches } from '../../../utils/text';
import type { CentralStock, LocationStock, Operation } from '../../../types/models';

export function CentralTab({ rows }: { rows: CentralStock[] }) {
  const [q, setQ] = useState('');
  const filtered = rows.filter(r => matches(q, r.name, r.sku));
  const { items, pager } = usePaged(filtered);
  return (
    <Panel>
      <FilterBar><SearchInput value={q} onChange={setQ} placeholder="Search product or SKU…" /></FilterBar>
      {filtered.length === 0 ? <Empty title="No products" /> : <CentralStockTable rows={items} />}
      <Pager {...pager} />
    </Panel>
  );
}

export function LocationStockTab({ rows, operations }: { rows: LocationStock[]; operations: Operation[] }) {
  const [op, setOp] = useState('');
  const [lowOnly, setLowOnly] = useState(false);
  const filtered = rows.filter(r => (!op || r.operation_id === op) && (!lowOnly || r.quantity <= r.low_stock_threshold));
  const { items, pager } = usePaged(filtered);
  return (
    <Panel>
      <FilterBar>
        <SelectFilter label="Sales location" value={op} onChange={setOp} allLabel="All Locations" options={operations.map(o => [o.id, o.name])} />
        <label className="check"><input type="checkbox" checked={lowOnly} onChange={e => setLowOnly(e.target.checked)} />Low or out of stock only</label>
      </FilterBar>
      {filtered.length === 0 ? <Empty title="No stock at locations" text="Approved and received transfers appear here." /> : (
        <DataTable rows={items} rowKey={r => r.operation_id + r.product_id} columns={[
          { header: 'Location', cell: r => <TitleCell title={<Link className="link" to={`/locations/${r.operation_id}`}>{r.operation_name}</Link>} sub={r.operation_code} /> },
          { header: 'Product', cell: r => <TitleCell title={r.name} sub={r.sku} /> },
          { header: 'Available', align: 'right', cell: r => num(r.quantity) },
          { header: 'Status', cell: r => <StockPill quantity={r.quantity} threshold={r.low_stock_threshold} /> },
        ]} />
      )}
      <Pager {...pager} />
    </Panel>
  );
}
