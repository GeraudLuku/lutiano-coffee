import { Link } from 'react-router-dom';
import { inventoryApi } from '../../../api/services';
import { DataTable, ErrorNote, Panel, TitleCell, ListState } from '../../../components/ui';
import { ProductThumb, StockPill } from '../../../components/domain';
import { useLoad } from '../../../hooks/useLoad';
import { num } from '../../../utils/format';
import type { ReorderAlert } from '../../../types/models';

/** Items at or below their threshold, with what is already on its way and a suggested quantity. */
export function ReorderTab({ version, onRequestTransfer, onRecordReceipt }: {
  version: number; onRequestTransfer: (r: ReorderAlert) => void; onRecordReceipt: (r: ReorderAlert) => void;
}) {
  const { data, error, loading, reload } = useLoad(() => inventoryApi.reorder(), [version]);
  return (
    <Panel>
      <p className="muted small">Suggested quantity tops stock up to three times the product's low-stock threshold, minus stock already requested or in transit.</p>
      <ErrorNote error={error} retry={reload} />
      <ListState loading={loading} data={data} items={data ?? []} empty={{ title: 'No reorder alerts', text: 'All stock is above its low-stock threshold.' }}>
        <DataTable rows={data ?? []} rowKey={r => `${r.operation_id ?? 'central'}-${r.product_id}`} columns={[
          { header: 'Location', cell: r => r.operation_id ? <TitleCell title={<Link className="link" to={`/locations/${r.operation_id}`}>{r.operation_name}</Link>} sub={r.operation_code ?? undefined} /> : <b>Central stock</b> },
          { header: 'Product', cell: r => <TitleCell lead={<ProductThumb url={r.image_url} size={36} />} title={r.name} sub={r.sku} /> },
          { header: 'On hand', align: 'right', cell: r => num(r.quantity) },
          { header: 'Threshold', align: 'right', cell: r => num(r.low_stock_threshold) },
          { header: 'On its way', align: 'right', cell: r => (r.operation_id ? num(r.inbound) : '—') },
          { header: 'Suggested', align: 'right', cell: r => <b>{num(r.suggested_quantity)}</b> },
          { header: 'Status', cell: r => <StockPill quantity={r.quantity} threshold={r.low_stock_threshold} /> },
          { header: '', align: 'right', cell: r => r.operation_id
            ? <button className="btn sm gold" disabled={r.suggested_quantity === 0} onClick={() => onRequestTransfer(r)}>Request transfer</button>
            : <button className="btn sm" onClick={() => onRecordReceipt(r)}>Record receipt</button> },
        ]} />
      </ListState>
    </Panel>
  );
}
