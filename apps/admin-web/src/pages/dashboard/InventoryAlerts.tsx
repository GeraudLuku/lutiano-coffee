import { Link } from 'react-router-dom';
import { ArrowRight, TriangleAlert } from 'lucide-react';
import { inventoryApi } from '../../api/services';
import { useLoad } from '../../hooks/useLoad';
import { ErrorNote, Loading, Panel, Pill } from '../../components/ui';
import { ProductThumb } from '../../components/domain';
import { num } from '../../utils/format';

const SHOWN = 4;

/** Products at or below their threshold (the open stock alerts), lowest first, with where they are low. */
export function InventoryAlerts() {
  const { data, error, loading, reload } = useLoad(() => inventoryApi.reorder(), []);
  return (
    <Panel title="Inventory Alerts" icon={<TriangleAlert size={19} />} action={<Link className="link" to="/inventory?tab=reorder">View Inventory <ArrowRight size={15} /></Link>}>
      <ErrorNote error={error} retry={reload} />
      {loading && !data ? <Loading /> : !data?.length ? <p className="muted pad">No stock is running low.</p> : (
        <ul className="stock-list">
          {data.slice(0, SHOWN).map(a => (
            <li key={`${a.operation_id}-${a.product_id}`}>
              <ProductThumb url={a.image_url} />
              <div><Link className="link" to={`/products/${a.product_id}`}>{a.name}</Link><small>{a.sku} · {a.operation_name}</small></div>
              <Pill tone={a.quantity <= 0 ? 'red' : 'amber'}>{a.quantity <= 0 ? 'Out of stock' : `${num(a.quantity)} units left`}</Pill>
            </li>
          ))}
        </ul>
      )}
    </Panel>
  );
}
