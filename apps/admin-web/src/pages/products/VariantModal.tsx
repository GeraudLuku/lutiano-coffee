import { useState } from 'react';
import { productsApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { Field, FormGrid, FormModal, Switch } from '../../components/ui';
import type { Product, ProductDetail } from '../../types/models';

/** Add or edit one variant (design: Variant Management). */
export function VariantModal({ parent, variant, onClose, onDone }: { parent: ProductDetail; variant?: Product; onClose: () => void; onDone: () => void }) {
  const [name, setName] = useState(variant?.variant_name ?? '');
  const [sku, setSku] = useState(variant?.sku ?? `${parent.sku}-`);
  const [unit, setUnit] = useState(variant?.unit ?? parent.unit ?? '');
  const [price, setPrice] = useState(variant ? String(variant.price_xaf) : '');
  const [threshold, setThreshold] = useState(String(variant?.low_stock_threshold ?? parent.low_stock_threshold));
  const [trackStock, setTrackStock] = useState(variant?.track_stock ?? parent.track_stock);
  const [active, setActive] = useState(variant?.active ?? true);
  const { busy, error, setError, run } = useAction();

  async function submit() {
    const p = Number(price);
    if (!Number.isInteger(p) || p < 0 || price === '') { setError('Price must be a whole number of FCFA, zero or more.'); return; }
    const body = { variantName: name, sku, unit, priceXaf: p, lowStockThreshold: Number(threshold), trackStock, active };
    const ok = await run(() => (variant ? productsApi.updateVariant(parent.id, variant.id, body) : productsApi.addVariant(parent.id, body)),
      variant ? 'Variant updated.' : 'Variant added.');
    if (ok) onDone();
  }

  return (
    <FormModal title={variant ? `Edit variant — ${variant.variant_name}` : `Add variant to ${parent.name}`} submitLabel={variant ? 'Save Changes' : 'Add Variant'}
      busy={busy} error={error} onSubmit={submit} onClose={onClose}>
      <FormGrid>
        <Field label="Variant name" required hint="e.g. Small (250 ml), 500 g pack">
          <input className="input" value={name} onChange={e => setName(e.target.value)} required maxLength={80} />
        </Field>
        <Field label="Variant SKU" required><input className="input" value={sku} onChange={e => setSku(e.target.value.toUpperCase())} required maxLength={80} /></Field>
        <Field label="Price (XAF)" required hint={variant ? 'Changes apply to new sales only.' : undefined}>
          <input className="input" type="number" min={0} step={1} value={price} onChange={e => setPrice(e.target.value)} required />
        </Field>
        <Field label="Unit"><input className="input" value={unit} onChange={e => setUnit(e.target.value)} maxLength={30} /></Field>
        <Field label="Low stock threshold" required><input className="input" type="number" min={0} step={1} value={threshold} onChange={e => setThreshold(e.target.value)} required /></Field>
        <Field label="Track stock" group><Switch checked={trackStock} onChange={setTrackStock} label={trackStock ? 'Tracked' : 'Not tracked'} /></Field>
      </FormGrid>
      <Field label="Status" group><Switch checked={active} onChange={setActive} label={active ? 'Active' : 'Inactive'} /></Field>
    </FormModal>
  );
}
