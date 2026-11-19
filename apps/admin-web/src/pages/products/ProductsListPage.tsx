import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { CheckCircle2, Download, Grid2x2, Layers, LayoutGrid, List, Plus, Upload, XCircle } from 'lucide-react';
import { categoriesApi, productsApi } from '../../api/services';
import { useAuth } from '../../auth/AuthContext';
import { useLoad } from '../../hooks/useLoad';
import { usePagedQuery } from '../../hooks/usePagedQuery';
import { useDebouncedState } from '../../hooks/useDebounced';
import { useAction } from '../../hooks/useAction';
import { DataTable, Empty, ErrorNote, FilterBar, Loading, PageHeader, Pager, Panel, Pill, SearchInput, SelectFilter, StatCard, StatRow, TitleCell } from '../../components/ui';
import { ProductThumb, StatusPill } from '../../components/domain';
import { num } from '../../utils/format';
import { centralUnits, priceLabel, variantsLabel } from '../../utils/catalog';
import { saveFile, toCsv } from '../../utils/files';
import { ProductCard } from './ProductCard';
import { ImportProductsModal } from './ImportProductsModal';
import { toast } from '../../components/ui';

export function ProductsListPage() {
  const nav = useNavigate();
  const { can } = useAuth();
  const categories = useLoad(() => categoriesApi.list(), []);
  const [q, setQ, search] = useDebouncedState('');
  const [category, setCategory] = useState('');
  const [status, setStatus] = useState('');
  const [view, setView] = useState<'grid' | 'list'>('grid');
  // The catalogue shows simple products and parents; variants appear inside their parent (search also matches variant SKUs).
  const { data, items, summary, pager, error, loading, reload } = usePagedQuery(
    (page, size) => productsApi.list({ view: 'catalog', q: search, category, active: status, page, size }), [search, category, status]);
  const exporting = useAction();
  const [importing, setImporting] = useState(false);
  const open = (id: string) => nav(`/products/${id}`);

  const exportCsv = () => exporting.run(async () => saveFile(toCsv((await productsApi.all('sellable')).map(p => ({ ...p, track_stock: p.track_stock ? 'yes' : 'no', active: p.active ? 'yes' : 'no' })),
    [['sku', 'SKU'], ['name', 'Name'], ['category', 'Category'], ['unit', 'Unit'], ['price_xaf', 'Price (XAF)'], ['track_stock', 'Tracks stock'],
      ['central_quantity', 'Central stock'], ['low_stock_threshold', 'Low-stock threshold'], ['active', 'Active']]), 'lutiano-products.csv'));

  return (
    <>
      <PageHeader title="Product Catalog" subtitle="Manage products, variants, centrally controlled prices, add-ons and stock settings." crumbs={[['Products']]}
        actions={<>
          {can('ADMIN') && <button className="btn" onClick={() => setImporting(true)}><Upload size={16} />Import</button>}
          <button className="btn" onClick={exportCsv} disabled={exporting.busy}><Download size={16} />{exporting.busy ? 'Exporting…' : 'Export'}</button>
          {can('ADMIN') && <button className="btn gold" onClick={() => nav('/products/new')}><Plus size={18} />Add Product</button>}
        </>} />
      <ErrorNote error={error || categories.error || exporting.error} retry={reload} />
      <StatRow>
        <StatCard tone="amber" icon={<Grid2x2 size={20} />} value={num(summary?.products ?? 0)} label="Total Products" />
        <StatCard tone="green" icon={<Layers size={20} />} value={num(summary?.variants ?? 0)} label="Total Variants" />
        <StatCard tone="blue" icon={<CheckCircle2 size={20} />} value={num(summary?.active ?? 0)} label="Active Products" />
        <StatCard tone="red" icon={<XCircle size={20} />} value={num(summary?.inactive ?? 0)} label="Inactive Products" />
      </StatRow>
      <div className="toolbar">
        <FilterBar>
          <SearchInput value={q} onChange={setQ} placeholder="Search products, SKU or category…" />
          <SelectFilter label="Category" value={category} onChange={setCategory} allLabel="All Categories" options={(categories.data ?? []).map(c => [c.name, c.name])} />
          <SelectFilter label="Status" value={status} onChange={setStatus} allLabel="All Status" options={[['true', 'Active'], ['false', 'Inactive']]} />
        </FilterBar>
        <div className="seg" role="group" aria-label="View">
          <button className={view === 'grid' ? 'active' : ''} onClick={() => setView('grid')} aria-label="Grid view" aria-pressed={view === 'grid'}><LayoutGrid size={17} /></button>
          <button className={view === 'list' ? 'active' : ''} onClick={() => setView('list')} aria-label="List view" aria-pressed={view === 'list'}><List size={17} /></button>
        </div>
      </div>
      {loading && !data ? <Loading /> : items.length === 0 ? <Panel><Empty title="No products found" text="Adjust the filters or add a product." /></Panel>
        : view === 'grid' ? <div className="product-grid">{items.map(p => <ProductCard key={p.id} product={p} onOpen={() => open(p.id)} />)}</div>
        : (
          <Panel>
            <DataTable rows={items} rowKey={p => p.id} onRowClick={p => open(p.id)} columns={[
              { header: 'Product', cell: p => <TitleCell lead={<ProductThumb url={p.image_url} size={40} />} title={p.name} sub={p.sku} /> },
              { header: 'Category', cell: p => <Pill tone="purple">{p.category}</Pill> },
              { header: 'Variants / unit', cell: p => variantsLabel(p) || '—' },
              { header: 'Price', align: 'right', cell: p => priceLabel(p) },
              { header: 'Central Stock', align: 'right', cell: p => (p.track_stock || p.kind === 'PARENT' ? num(centralUnits(p)) : 'Not tracked') },
              { header: 'Status', cell: p => <StatusPill active={p.active} /> },
            ]} />
          </Panel>
        )}
      <Pager {...pager} />
      {importing && <ImportProductsModal onClose={() => setImporting(false)} onDone={() => { setImporting(false); toast('Products imported.'); reload(); }} />}
    </>
  );
}
