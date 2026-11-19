import { useState } from 'react';
import { Pencil, Plus, Tags } from 'lucide-react';
import { categoriesApi } from '../../api/services';
import { useAuth } from '../../auth/AuthContext';
import { useAction } from '../../hooks/useAction';
import { useLoad } from '../../hooks/useLoad';
import { DataTable, ErrorNote, Field, FormGrid, FormModal, ImageField, PageHeader, Panel, Switch, TitleCell, ListState } from '../../components/ui';
import { ProductThumb, StatusPill } from '../../components/domain';
import { uploadPicked } from '../../utils/images';
import { num } from '../../utils/format';
import type { Category } from '../../types/models';

/** Managed product categories. Renaming updates every product in the category; inactive categories are hidden from product forms. */
export function CategoriesPage() {
  const { can } = useAuth();
  const { data, error, loading, reload } = useLoad(() => categoriesApi.list(), []);
  const [editing, setEditing] = useState<Category | 'new' | null>(null);
  const admin = can('ADMIN');
  return (
    <>
      <PageHeader title="Product Categories" subtitle="Group products for the catalogue, filters and reports." crumbs={[['Products', '/products'], ['Categories']]}
        actions={admin && <button className="btn gold" onClick={() => setEditing('new')}><Plus size={18} />Add Category</button>} />
      <ErrorNote error={error} retry={reload} />
      <Panel title={`Categories (${data?.length ?? 0})`} icon={<Tags size={19} />}>
        <ListState loading={loading} data={data} items={data ?? []} empty={{ title: 'No categories yet' }}>
          <DataTable rows={data ?? []} rowKey={c => c.id} columns={[
            { header: 'Name', cell: c => <TitleCell lead={<ProductThumb url={c.image_url} />} title={c.name} sub={c.description ?? undefined} /> },
            { header: 'Products', align: 'right', cell: c => num(c.product_count) },
            { header: 'Sort order', align: 'right', cell: c => c.sort_order },
            { header: 'Status', cell: c => <StatusPill active={c.active} /> },
            ...(admin ? [{ header: '', align: 'right' as const, cell: (c: Category) => <button className="btn sm" onClick={() => setEditing(c)}><Pencil size={14} />Edit</button> }] : []),
          ]} />
        </ListState>
      </Panel>
      {editing && <CategoryModal category={editing === 'new' ? undefined : editing} onClose={() => setEditing(null)} onDone={() => { setEditing(null); reload(); }} />}
    </>
  );
}

function CategoryModal({ category, onClose, onDone }: { category?: Category; onClose: () => void; onDone: () => void }) {
  const [name, setName] = useState(category?.name ?? '');
  const [description, setDescription] = useState(category?.description ?? '');
  const [sortOrder, setSortOrder] = useState(String(category?.sort_order ?? 0));
  const [active, setActive] = useState(category?.active ?? true);
  const [image, setImage] = useState<File | null>(null);
  const { busy, error, run } = useAction();
  async function submit() {
    const body = { name, description, sortOrder: Number(sortOrder) || 0, active };
    const saved = await run(async () => {
      const c = category ? await categoriesApi.update(category.id, body) : await categoriesApi.create(body);
      await uploadPicked(image, file => categoriesApi.uploadImage(c.id, file), 'Category');
      return c;
    }, category ? 'Category updated.' : 'Category created.');
    if (saved) onDone();
  }
  const renamed = category && name.trim() && name.trim() !== category.name;
  return (
    <FormModal title={category ? 'Edit category' : 'Add category'} submitLabel={category ? 'Save Changes' : 'Add Category'} busy={busy} error={error} onSubmit={submit} onClose={onClose}>
      <Field label="Image" group hint="JPG, PNG or WebP, up to 5 MB."><ImageField url={category?.image_url} file={image} onChange={setImage} compact /></Field>
      <Field label="Name" required hint={renamed ? `All ${category!.product_count} products in this category will move to the new name.` : undefined}>
        <input className="input" value={name} onChange={e => setName(e.target.value)} required maxLength={60} />
      </Field>
      <Field label="Description"><input className="input" value={description} onChange={e => setDescription(e.target.value)} maxLength={255} /></Field>
      <FormGrid>
        <Field label="Sort order" hint="Lower numbers appear first."><input className="input" type="number" step={1} value={sortOrder} onChange={e => setSortOrder(e.target.value)} /></Field>
        <Field label="Status" group hint={!active ? 'Hidden from product forms; existing products keep it.' : undefined}>
          <Switch checked={active} onChange={setActive} label={active ? 'Active' : 'Inactive'} />
        </Field>
      </FormGrid>
    </FormModal>
  );
}
