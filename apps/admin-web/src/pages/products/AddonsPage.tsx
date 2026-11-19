import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Pencil, Plus, PlusSquare } from 'lucide-react';
import { addonsApi } from '../../api/services';
import { useAuth } from '../../auth/AuthContext';
import { useAction } from '../../hooks/useAction';
import { useLoad } from '../../hooks/useLoad';
import { DataTable, ErrorNote, Field, FormGrid, FormModal, ImageField, Note, PageHeader, Panel, Switch, TitleCell, ListState } from '../../components/ui';
import { ProductThumb, StatusPill } from '../../components/domain';
import { uploadPicked } from '../../utils/images';
import { xaf } from '../../utils/format';
import type { Addon } from '../../types/models';

/** Add-ons such as an extra shot or syrup. Choose which products allow each add-on on the product page. */
export function AddonsPage() {
  const { can } = useAuth();
  const { data, error, loading, reload } = useLoad(() => addonsApi.list(), []);
  const [editing, setEditing] = useState<Addon | 'new' | null>(null);
  const admin = can('ADMIN');
  return (
    <>
      <PageHeader title="Add-ons" subtitle="Extras customers can add to a product, each with its own price per unit." crumbs={[['Products', '/products'], ['Add-ons']]}
        actions={admin && <button className="btn gold" onClick={() => setEditing('new')}><Plus size={18} />Add Add-on</button>} />
      <ErrorNote error={error} retry={reload} />
      <Note>Add-on prices are added to the product price for each unit sold and are saved on the sale. Add-ons do not track stock. To offer one, open a product and select it under Add-ons.</Note>
      <Panel title={`Add-ons (${data?.length ?? 0})`} icon={<PlusSquare size={19} />}>
        <ListState loading={loading} data={data} items={data ?? []} empty={{ title: 'No add-ons yet', text: 'Create extras such as Extra shot, Caramel syrup or Oat milk.' }}>
          <DataTable rows={data ?? []} rowKey={a => a.id} columns={[
            { header: 'Name', cell: a => <TitleCell lead={<ProductThumb url={a.image_url} />} title={a.name} /> },
            { header: 'Price', align: 'right', cell: a => `+${xaf(a.price_xaf)}` },
            { header: 'Products offering it', cell: a => <OfferedBy products={a.products ?? []} /> },
            { header: 'Sort order', align: 'right', cell: a => a.sort_order },
            { header: 'Status', cell: a => <StatusPill active={a.active} /> },
            ...(admin ? [{ header: '', align: 'right' as const, cell: (a: Addon) => <button className="btn sm" onClick={() => setEditing(a)}><Pencil size={14} />Edit</button> }] : []),
          ]} />
        </ListState>
      </Panel>
      {editing && <AddonModal addon={editing === 'new' ? undefined : editing} onClose={() => setEditing(null)} onDone={() => { setEditing(null); reload(); }} />}
    </>
  );
}

const SHOWN_PRODUCTS = 3;

/** The products that offer an add-on, each linking to its page; the rest are summarised. */
function OfferedBy({ products }: { products: NonNullable<Addon['products']> }) {
  if (!products.length) return <span className="muted">None yet</span>;
  const rest = products.slice(SHOWN_PRODUCTS);
  return (
    <span className="offered-by">
      {products.slice(0, SHOWN_PRODUCTS).map((p, i) => <span key={p.id}>{i > 0 && ', '}<Link className="link" to={`/products/${p.id}`}>{p.name}</Link></span>)}
      {rest.length > 0 && <span className="muted" title={rest.map(p => p.name).join(', ')}> +{rest.length} more</span>}
    </span>
  );
}

function AddonModal({ addon, onClose, onDone }: { addon?: Addon; onClose: () => void; onDone: () => void }) {
  const [name, setName] = useState(addon?.name ?? '');
  const [price, setPrice] = useState(addon ? String(addon.price_xaf) : '');
  const [sortOrder, setSortOrder] = useState(String(addon?.sort_order ?? 0));
  const [active, setActive] = useState(addon?.active ?? true);
  const [image, setImage] = useState<File | null>(null);
  const { busy, error, setError, run } = useAction();
  async function submit() {
    const p = Number(price);
    if (!Number.isInteger(p) || p < 0 || price === '') { setError('Price must be a whole number of FCFA, zero or more.'); return; }
    const body = { name, priceXaf: p, sortOrder: Number(sortOrder) || 0, active };
    const saved = await run(async () => {
      const a = addon ? await addonsApi.update(addon.id, body) : await addonsApi.create(body);
      await uploadPicked(image, file => addonsApi.uploadImage(a.id, file), 'Add-on');
      return a;
    }, addon ? 'Add-on updated.' : 'Add-on created.');
    if (saved) onDone();
  }
  return (
    <FormModal title={addon ? 'Edit add-on' : 'Add add-on'} submitLabel={addon ? 'Save Changes' : 'Add Add-on'} busy={busy} error={error} onSubmit={submit} onClose={onClose}>
      <Field label="Image" group hint="JPG, PNG or WebP, up to 5 MB."><ImageField url={addon?.image_url} file={image} onChange={setImage} compact /></Field>
      <Field label="Name" required><input className="input" value={name} onChange={e => setName(e.target.value)} required maxLength={80} placeholder="e.g. Extra shot" /></Field>
      <FormGrid>
        <Field label="Price per unit (XAF)" required hint={addon ? 'Changes apply to new sales only.' : undefined}>
          <input className="input" type="number" min={0} step={1} value={price} onChange={e => setPrice(e.target.value)} required />
        </Field>
        <Field label="Sort order"><input className="input" type="number" step={1} value={sortOrder} onChange={e => setSortOrder(e.target.value)} /></Field>
      </FormGrid>
      <Field label="Status" group hint={!active ? 'Inactive add-ons cannot be added to new sales.' : undefined}>
        <Switch checked={active} onChange={setActive} label={active ? 'Active' : 'Inactive'} />
      </Field>
    </FormModal>
  );
}
