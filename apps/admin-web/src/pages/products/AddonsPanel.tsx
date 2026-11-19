import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { PlusSquare } from 'lucide-react';
import { addonsApi, productsApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { useLoad } from '../../hooks/useLoad';
import { Empty, ErrorNote, Loading, Panel } from '../../components/ui';
import { xaf } from '../../utils/format';
import type { ProductDetail } from '../../types/models';

/** Which add-ons can be ordered with this product (on a parent, for all its variants). */
export function AddonsPanel({ product, canEdit, onChanged }: { product: ProductDetail; canEdit: boolean; onChanged: () => void }) {
  const all = useLoad(() => addonsApi.list(), []);
  const [selected, setSelected] = useState<string[]>(product.addon_ids);
  useEffect(() => setSelected(product.addon_ids), [product.addon_ids]);
  const action = useAction();
  const dirty = [...selected].sort().join() !== [...product.addon_ids].sort().join();
  const toggle = (id: string) => setSelected(s => (s.includes(id) ? s.filter(x => x !== id) : [...s, id]));

  async function save() {
    if (await action.run(() => productsApi.setAddons(product.id, selected), 'Add-ons saved.')) onChanged();
  }

  const options = (all.data ?? []).filter(a => a.active || selected.includes(a.id));
  return (
    <Panel title="Allowed Add-ons" icon={<PlusSquare size={19} />}
      action={canEdit && <button className="btn gold sm" disabled={!dirty || action.busy} onClick={save}>Save add-ons</button>}>
      <p className="muted small">
        {product.kind === 'PARENT' ? 'Customers can add these to any variant of this product.' : 'Customers can add these when buying this product.'}
        {' '}Add-on prices are added per unit. <Link className="link" to="/products/addons">Manage add-ons</Link>
      </p>
      <ErrorNote error={all.error || action.error} />
      {all.loading && !all.data ? <Loading /> : options.length === 0 ? <Empty title="No add-ons yet" text="Create add-ons such as Extra shot or Caramel syrup first." /> : (
        <ul className="check-list">
          {options.map(a => (
            <li key={a.id}>
              <label className="check">
                <input type="checkbox" checked={selected.includes(a.id)} disabled={!canEdit} onChange={() => toggle(a.id)} />
                <span>{a.name}{!a.active && <em className="muted"> (inactive)</em>}</span>
              </label>
              <b>+{xaf(a.price_xaf)}</b>
            </li>
          ))}
        </ul>
      )}
    </Panel>
  );
}
