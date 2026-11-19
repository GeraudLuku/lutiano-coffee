import { Link } from 'react-router-dom';
import { DataTable, LevelBar, TitleCell } from '../ui';
import { ProductThumb } from './ProductThumb';
import { StockPill } from './Pills';
import { num } from '../../utils/format';
import { stockScale, stockStatus } from '../../utils/stock';
import type { CentralStock, LocationStock } from '../../types/models';

/** Product stock at one location (location detail page). */
export function ProductStockTable({ rows, empty = 'No stock held here.' }: { rows: LocationStock[]; empty?: string }) {
  if (!rows.length) return <p className="muted pad">{empty}</p>;
  return (
    <DataTable rows={rows} rowKey={r => r.product_id} columns={[
      { header: 'Product', cell: r => <TitleCell lead={<ProductThumb url={r.image_url} size={36} />} title={r.name} sub={r.sku} /> },
      { header: 'Available', align: 'right', cell: r => num(r.quantity) },
      { header: 'Status', cell: r => <StockPill quantity={r.quantity} threshold={r.low_stock_threshold} /> },
    ]} />
  );
}

/** Central warehouse stock with level bars (inventory page). */
export function CentralStockTable({ rows }: { rows: CentralStock[] }) {
  return (
    <DataTable rows={rows} rowKey={r => r.product_id} columns={[
      { header: 'Product', cell: r => <TitleCell lead={<ProductThumb url={r.image_url} size={38} />} title={<Link className="link" to={`/products/${r.product_id}`}>{r.name}</Link>} sub={`${r.sku}${r.active ? '' : ' · inactive'}`} /> },
      { header: 'Category', cell: r => r.category },
      { header: 'Quantity', align: 'right', cell: r => num(r.quantity) },
      { header: 'Level', className: 'level-cell', cell: r => <LevelBar value={r.quantity} max={stockScale(r.quantity, r.low_stock_threshold)} tone={stockStatus(r.quantity, r.low_stock_threshold)[0]} /> },
      { header: 'Status', cell: r => <StockPill quantity={r.quantity} threshold={r.low_stock_threshold} /> },
    ]} />
  );
}
