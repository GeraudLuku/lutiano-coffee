import { useState } from 'react';
import { Layers, Pencil, Plus } from 'lucide-react';
import { productsApi } from '../../api/services';
import { useLoad } from '../../hooks/useLoad';
import { commissionSummary } from '../../utils/earnings';
import { useAction } from '../../hooks/useAction';
import { DataTable, Empty, ErrorNote, Panel, TitleCell } from '../../components/ui';
import { StatusPill, StockPill } from '../../components/domain';
import { num, xaf } from '../../utils/format';
import type { Product, ProductDetail } from '../../types/models';
import { VariantModal } from './VariantModal';

/** Variant table (design: Manage Variants). Variants are deactivated rather than deleted, so sales history stays intact. */
export function VariantsPanel({ product, canEdit, onChanged }: { product: ProductDetail; canEdit: boolean; onChanged: () => void }) {
  const [editing, setEditing] = useState<Product | 'new' | null>(null);
  const action = useAction();
  const rewards = useLoad(() => productsApi.rewards(product.id), [product.id, product.variants.length]);
  const rewardOf = (id: string) => rewards.data?.find(r => r.product_id === id);
  async function toggle(v: Product) {
    if (await action.run(() => productsApi.setActive(v.id, !v.active), v.active ? 'Variant deactivated.' : 'Variant activated.')) onChanged();
  }
  return (
    <Panel title={`Variants (${product.variants.length})`} icon={<Layers size={19} />}
      action={canEdit && <button className="btn gold sm" onClick={() => setEditing('new')}><Plus size={15} />Add Variant</button>}>
      <ErrorNote error={action.error} />
      {product.variants.length === 0 ? <Empty title="No variants yet" text="Add the sizes or packs this product is sold in. Each has its own SKU, price and stock." /> : (
        <DataTable rows={product.variants} rowKey={v => v.id} columns={[
          { header: 'Variant Name', cell: v => <TitleCell title={v.variant_name} sub={v.unit ?? undefined} /> },
          { header: 'SKU', cell: v => v.sku },
          { header: 'Price', align: 'right', cell: v => xaf(v.price_xaf) },
          { header: 'Central Stock', align: 'right', cell: v => (v.track_stock ? num(v.central_quantity) : 'Not tracked') },
          { header: 'Loyalty Points', align: 'right', cell: v => { const r = rewardOf(v.id); return !r ? '…' : r.loyalty_eligible ? `${num(r.loyalty_points)} pts` : '—'; } },
          { header: 'Commission', align: 'right', cell: v => { const r = rewardOf(v.id); return r ? commissionSummary(r.commission) : '…'; } },
          { header: 'Stock Status', cell: v => (v.track_stock ? <StockPill quantity={v.central_quantity} threshold={v.low_stock_threshold} /> : '—') },
          { header: 'Status', cell: v => <StatusPill active={v.active} /> },
          ...(canEdit ? [{ header: '', align: 'right' as const, cell: (v: Product) => (
            <div className="row-actions">
              <button className="btn sm" onClick={() => setEditing(v)} aria-label={`Edit ${v.variant_name}`}><Pencil size={14} />Edit</button>
              <button className="btn sm" disabled={action.busy} onClick={() => toggle(v)}>{v.active ? 'Deactivate' : 'Activate'}</button>
            </div>
          ) }] : []),
        ]} />
      )}
      {editing && (
        <VariantModal parent={product} variant={editing === 'new' ? undefined : editing}
          onClose={() => setEditing(null)} onDone={() => { setEditing(null); onChanged(); }} />
      )}
    </Panel>
  );
}
