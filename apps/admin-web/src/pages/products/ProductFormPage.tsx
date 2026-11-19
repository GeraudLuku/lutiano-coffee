import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ImagePlus, Info, Layers, Plus, Settings2, Trash2, Wallet } from 'lucide-react';
import { mediaUrl } from '../../api/client';
import { categoriesApi, productsApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { useLoad } from '../../hooks/useLoad';
import { useObjectUrl } from '../../hooks/useObjectUrl';
import { ErrorNote, Field, FormGrid, Loading, Note, PageHeader, Panel, Switch, toast } from '../../components/ui';
import { errorMessage } from '../../utils/errors';
import { CategoryField } from './CategoryField';

interface FormState {
  name: string; category: string; sku: string; description: string; unit: string; priceXaf: string; lowStockThreshold: string;
  active: boolean; trackStock: boolean; loyaltyEligible: boolean; promotionEligible: boolean; requireVariants: boolean;
}
const EMPTY: FormState = {
  name: '', category: '', sku: '', description: '', unit: '', priceXaf: '', lowStockThreshold: '10',
  active: true, trackStock: true, loyaltyEligible: true, promotionEligible: true, requireVariants: false,
};
const MAX_IMAGE_BYTES = 5 * 1024 * 1024;

/** A variant typed into the form before the product exists; saved right after the product. */
interface VariantDraft { name: string; sku: string; unit: string; price: string; threshold: string }

/** Add / Edit Product (design: Product Catalog board, panel 2). Variants are managed on the product page. */
export function ProductFormPage() {
  const { id } = useParams();
  const nav = useNavigate();
  const editing = !!id;
  const existing = useLoad(() => (id ? productsApi.get(id) : Promise.resolve(null)), [id]);
  const categories = useLoad(() => categoriesApi.list(), []);
  const [f, setF] = useState<FormState>(EMPTY);
  const [file, setFile] = useState<File | null>(null);
  const [drafts, setDrafts] = useState<VariantDraft[]>([]);
  const setDraft = (i: number, patch: Partial<VariantDraft>) => setDrafts(prev => prev.map((d, n) => (n === i ? { ...d, ...patch } : d)));
  const filePreview = useObjectUrl(file);
  const { busy, error, setError, run } = useAction();

  useEffect(() => {
    const d = existing.data;
    if (!d) return;
    if (d.kind === 'VARIANT') { nav(`/products/${d.parent_id}?tab=variants`, { replace: true }); return; }
    setF({
      name: d.name, category: d.category, sku: d.sku, description: d.description ?? '', unit: d.unit ?? '',
      priceXaf: String(d.price_xaf), lowStockThreshold: String(d.low_stock_threshold), active: d.active, trackStock: d.track_stock,
      loyaltyEligible: d.loyalty_eligible, promotionEligible: d.promotion_eligible, requireVariants: d.kind === 'PARENT',
    });
  }, [existing.data, nav]);
  const set = <K extends keyof FormState>(k: K, v: FormState[K]) => setF(prev => ({ ...prev, [k]: v }));
  const setCategory = useCallback((v: string) => setF(prev => ({ ...prev, category: v })), []);

  async function submit(e: FormEvent) {
    e.preventDefault();
    const price = Number(f.priceXaf), threshold = Number(f.lowStockThreshold);
    if (!f.requireVariants && (!Number.isInteger(price) || price < 0 || f.priceXaf === '')) { setError('Price must be a whole number of FCFA, zero or more.'); return; }
    for (const d of drafts) {
      const price = Number(d.price);
      if (!d.name.trim() || !d.sku.trim() || d.price === '' || !Number.isInteger(price) || price < 0) { setError('Each variant needs a name, a SKU and a whole price in FCFA.'); return; }
    }
    const saved = await run(async () => {
      const body = {
        name: f.name, category: f.category, sku: f.sku, description: f.description, unit: f.unit, lowStockThreshold: threshold,
        priceXaf: f.requireVariants ? undefined : price, active: f.active, trackStock: f.trackStock,
        loyaltyEligible: f.loyaltyEligible, promotionEligible: f.promotionEligible, requireVariants: f.requireVariants,
      };
      const product = editing ? await productsApi.update(id!, body) : await productsApi.create(body);
      if (!editing && f.requireVariants) {
        for (const d of drafts) {
          try { await productsApi.addVariant(product.id, { variantName: d.name, sku: d.sku, unit: d.unit || f.unit, priceXaf: Number(d.price), lowStockThreshold: Number(d.threshold) || threshold, trackStock: f.trackStock, active: true }); }
          catch (x) { toast(`Product saved, but variant "${d.name}" failed: ${errorMessage(x)}. Add it on the product page.`, 'error'); }
        }
      }
      if (file) {
        try { await productsApi.uploadImage(product.id, file); }
        catch (x) { toast(`Product saved, but the image upload failed: ${errorMessage(x)}`, 'error'); }
      }
      return product;
    }, editing ? 'Product updated.' : f.requireVariants ? 'Product created. Now add its variants.' : 'Product created.');
    if (saved) nav(`/products/${saved.id}${!editing && f.requireVariants ? '?tab=variants' : ''}`);
  }

  function pickFile(picked: File | null) {
    if (picked && picked.size > MAX_IMAGE_BYTES) { toast('Images must be 5 MB or smaller.', 'error'); return; }
    setFile(picked);
  }

  if (editing && existing.loading) return <Loading />;
  const preview = filePreview ?? mediaUrl(existing.data?.image_url);
  const wasParent = existing.data?.kind === 'PARENT';
  return (
    <form onSubmit={submit}>
      <PageHeader title={editing ? 'Edit Product' : 'Add New Product'}
        subtitle={editing ? 'Price changes apply to new sales only; completed sales keep their original price.' : 'Create a product and, if it comes in sizes or packs, its variants.'}
        crumbs={[['Products', '/products'], [editing ? 'Edit Product' : 'Add Product']]}
        actions={<><button type="button" className="btn" onClick={() => nav(-1)}>Cancel</button><button className="btn gold" disabled={busy}>{busy ? 'Saving…' : 'Save Product'}</button></>} />
      <ErrorNote error={error || existing.error} />
      <div className="grid-form wide-left">
        <div className="stack">
          <Panel title="Product Information" icon={<Info size={19} />}>
            <FormGrid>
              <Field label="Product Name" required><input className="input" value={f.name} onChange={e => set('name', e.target.value)} required maxLength={150} /></Field>
              <CategoryField value={f.category} categories={categories.data ?? []} onChange={setCategory} onCreated={categories.reload} />
              <Field label={f.requireVariants ? 'Base SKU' : 'SKU'} required><input className="input" value={f.sku} onChange={e => set('sku', e.target.value.toUpperCase())} required maxLength={80} /></Field>
              <Field label="Unit" hint="How it is sold, e.g. cup, 250 g pack, bottle."><input className="input" value={f.unit} onChange={e => set('unit', e.target.value)} maxLength={30} /></Field>
            </FormGrid>
            <Field label="Description"><textarea className="input" rows={3} value={f.description} onChange={e => set('description', e.target.value)} maxLength={500} /></Field>
          </Panel>
          <Panel title="Product Settings" icon={<Settings2 size={19} />}>
            <FormGrid>
              <Field label="Require variant" group hint="Sold only as variants (sizes, packs). The product itself has no price or stock.">
                <Switch checked={f.requireVariants} onChange={v => set('requireVariants', v)} label={f.requireVariants ? 'Variants required' : 'Sold as one item'} />
              </Field>
              <Field label="Track stock" group hint={f.requireVariants ? 'Default for new variants.' : 'Off for made-to-order items: sold without stock checks.'}>
                <Switch checked={f.trackStock} onChange={v => set('trackStock', v)} label={f.trackStock ? 'Tracked' : 'Not tracked'} />
              </Field>
              <Field label="Loyalty points" group hint="Whether purchases can earn points (when loyalty launches).">
                <Switch checked={f.loyaltyEligible} onChange={v => set('loyaltyEligible', v)} label={f.loyaltyEligible ? 'Eligible' : 'Not eligible'} />
              </Field>
              <Field label="Promotions" group hint="Whether promotions may include this product.">
                <Switch checked={f.promotionEligible} onChange={v => set('promotionEligible', v)} label={f.promotionEligible ? 'Eligible' : 'Not eligible'} />
              </Field>
              <Field label="Status" group><Switch checked={f.active} onChange={v => set('active', v)} label={f.active ? 'Active' : 'Inactive'} /></Field>
            </FormGrid>
            {editing && f.requireVariants !== wasParent && (
              <Note tone="warn">{f.requireVariants
                ? 'A product can only switch to variants if it has never been sold and holds no stock.'
                : 'Deactivate or remove its variants first; then it can be sold as one item.'}</Note>
            )}
          </Panel>
          <Panel title={f.requireVariants ? 'Stock Defaults' : 'Pricing & Stock'} icon={<Wallet size={19} />}>
            <FormGrid>
              {!f.requireVariants && (
                <Field label="Price (XAF)" required hint="Sellers cannot change prices.">
                  <input className="input" type="number" min={0} step={1} value={f.priceXaf} onChange={e => set('priceXaf', e.target.value)} required />
                </Field>
              )}
              <Field label="Low Stock Threshold" required hint={f.requireVariants ? 'Default for new variants.' : 'Alert when stock is at or below this.'}>
                <input className="input" type="number" min={0} step={1} value={f.lowStockThreshold} onChange={e => set('lowStockThreshold', e.target.value)} required />
              </Field>
            </FormGrid>
            {f.requireVariants && <p className="muted small">Each variant has its own price, SKU and stock.</p>}
          </Panel>
          {f.requireVariants && (
            <Panel title="Product Variants" icon={<Layers size={19} />}
              action={!editing && <button type="button" className="btn gold sm" onClick={() => setDrafts(prev => [...prev, { name: '', sku: `${f.sku}-`, unit: f.unit, price: '', threshold: f.lowStockThreshold }])}><Plus size={15} />Add Variant</button>}>
              {editing ? <p className="muted">Add, edit or deactivate variants on the product page, under Variants (each shows its price, stock, loyalty points and commission).</p>
                : drafts.length === 0 ? <p className="muted">Add the sizes or packs this product is sold in (e.g. Small 250 ml, Large 500 ml). You can also add them later.</p> : (
                  <div className="draft-variants">
                    <div className="draft-head"><span>Variant name</span><span>SKU</span><span>Price (XAF)</span><span>Low stock</span><span /></div>
                    {drafts.map((d, i) => (
                      <div className="draft-row" key={i}>
                        <input className="input" aria-label="Variant name" value={d.name} onChange={e => setDraft(i, { name: e.target.value })} maxLength={80} placeholder="Small (250 ml)" />
                        <input className="input" aria-label="Variant SKU" value={d.sku} onChange={e => setDraft(i, { sku: e.target.value.toUpperCase() })} maxLength={80} />
                        <input className="input" aria-label="Price" type="number" min={0} step={1} value={d.price} onChange={e => setDraft(i, { price: e.target.value })} />
                        <input className="input" aria-label="Low stock threshold" type="number" min={0} step={1} value={d.threshold} onChange={e => setDraft(i, { threshold: e.target.value })} />
                        <button type="button" className="btn sm" aria-label="Remove variant" onClick={() => setDrafts(prev => prev.filter((_, n) => n !== i))}><Trash2 size={14} /></button>
                      </div>
                    ))}
                  </div>
                )}
              <p className="muted small">Loyalty points and commission follow the loyalty and commission rules; see them on the product page, under Loyalty & Commission.</p>
            </Panel>
          )}
        </div>
        <Panel title="Product Image" icon={<ImagePlus size={19} />}>
          <label className="image-drop">
            {preview ? <img src={preview} alt="" /> : <><ImagePlus size={36} /><span>Choose an image</span></>}
            <input type="file" accept="image/jpeg,image/png,image/webp" onChange={e => pickFile(e.target.files?.[0] ?? null)} />
          </label>
          <small className="muted">Recommended: 800 × 800 px (JPG, PNG or WebP, up to 5 MB).{f.requireVariants && ' Variants use this image.'}</small>
        </Panel>
      </div>
    </form>
  );
}
