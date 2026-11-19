import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { Ban, BadgePercent, Boxes, Check, History, Info, Layers, Package, Pencil, Receipt, Star, Wallet } from 'lucide-react';
import { productsApi } from '../../api/services';
import { useAuth } from '../../auth/AuthContext';
import { useAction } from '../../hooks/useAction';
import { useLoad } from '../../hooks/useLoad';
import { DataTable, ErrorNote, Facts, Loading, PageHeader, Panel, Pill, StatCard, StatRow, Tabs, TitleCell } from '../../components/ui';
import { PagedActivity, ProductImage, StatusPill, StockPill } from '../../components/domain';
import { date, dateTime, num, xaf } from '../../utils/format';
import { centralUnits, priceLabel } from '../../utils/catalog';
import type { ProductDetail } from '../../types/models';
import { VariantsPanel } from './VariantsPanel';
import { AddonsPanel } from './AddonsPanel';
import { PriceHistoryTable } from './PriceHistoryTable';
import { RewardsTab } from './RewardsTab';
import { PerformanceTab } from './PerformanceTab';

type Tab = 'overview' | 'variants' | 'addons' | 'stock' | 'rewards' | 'performance' | 'prices' | 'activity';
const yesNo = (v: boolean) => (v ? <Pill tone="green">Yes</Pill> : <Pill tone="grey">No</Pill>);

/** Product Details (design: Product Catalog board, panel 3). */
export function ProductDetailPage() {
  const { id } = useParams();
  const nav = useNavigate();
  const { can } = useAuth();
  const [params, setParams] = useSearchParams();
  const { data: p, error, loading, reload } = useLoad(() => productsApi.get(id!), [id]);
  const action = useAction();
  const admin = can('ADMIN');

  if (loading && !p) return <Loading />;
  if (!p) return <ErrorNote error={error} retry={reload} />;
  const parent = p.kind === 'PARENT', variant = p.kind === 'VARIANT';
  const tabs: [Tab, string][] = [
    ['overview', 'Overview'],
    ...(parent ? [['variants', `Variants (${p.variants.length})`] as [Tab, string]] : []),
    ...(!variant ? [['addons', `Add-ons (${p.addon_ids.length})`] as [Tab, string]] : []),
    ['stock', 'Inventory'], ['rewards', 'Loyalty & Commission'], ['performance', 'Sales Performance'], ['prices', 'Price History'], ['activity', 'Activity'],
  ];
  const tab = (params.get('tab') as Tab) && tabs.some(t => t[0] === params.get('tab')) ? (params.get('tab') as Tab) : 'overview';
  const setTab = (t: Tab) => setParams({ tab: t }, { replace: true });

  async function toggle() {
    if (await action.run(() => productsApi.setActive(p!.id, !p!.active), p!.active ? 'Product deactivated.' : 'Product activated.')) reload();
  }

  return (
    <>
      <PageHeader crumbs={[['Products', '/products'], ...(variant && p.parent_id ? [[p.parent_name ?? 'Product', `/products/${p.parent_id}`] as [string, string]] : []), [variant ? p.variant_name ?? p.name : p.name]]} />
      <ErrorNote error={action.error} />
      <section className="panel product-head">
        <ProductImage url={p.image_url} />
        <div>
          <h1>{p.name} <StatusPill active={p.active} /></h1>
          {variant && <p>Variant of <Link className="link" to={`/products/${p.parent_id}`}>{p.parent_name}</Link>{p.parent_active === false && ' (parent inactive — not on sale)'}</p>}
          <p>{p.description ?? 'No description.'}</p>
          <div className="row">
            <Pill tone="purple">{p.category}</Pill>
            <span className="muted">{parent ? 'Base SKU' : 'SKU'}: {p.sku}</span>
            {p.unit && <span className="muted">Unit: {p.unit}</span>}
            <span className="muted">Created: {date(p.created_at)}</span>
          </div>
        </div>
        {admin && (
          <div className="header-actions">
            {variant
              ? <button className="btn" onClick={() => nav(`/products/${p.parent_id}?tab=variants`)}><Pencil size={16} />Edit in parent</button>
              : <button className="btn" onClick={() => nav(`/products/${p.id}/edit`)}><Pencil size={16} />Edit Product</button>}
            <button className={p.active ? 'btn danger' : 'btn'} onClick={toggle} disabled={action.busy}>{p.active ? <><Ban size={16} />Deactivate</> : <><Check size={16} />Activate</>}</button>
          </div>
        )}
      </section>
      <Tabs value={tab} onChange={setTab} tabs={tabs} />

      {tab === 'overview' && (
        <>
          <StatRow>
            <StatCard tone="green" icon={<Package size={20} />} value={num(p.stats.units_sold)} label="Total Units Sold" />
            <StatCard tone="blue" icon={<Wallet size={20} />} value={xaf(p.stats.revenue_xaf)} label="Total Revenue" />
            <StatCard tone="purple" icon={<BadgePercent size={20} />} value={xaf(p.stats.commission_xaf)} label="Commission Generated" />
            <StatCard tone="red" icon={<Star size={20} />} value={num(p.stats.points_awarded)} label="Points Awarded" />
          </StatRow>
          <div className="grid-2">
            <Panel title="Product Information" icon={<Info size={19} />}>
              <Facts items={[
                ['Category', p.category], [parent ? 'Base SKU' : 'SKU', p.sku], ['Unit', p.unit ?? '—'],
                ['Price', parent ? priceLabel(p) : <b>{xaf(p.price_xaf)}</b>],
                ['Variants', parent ? 'Required' : variant ? `Variant of ${p.parent_name}` : 'None (sold as one item)'],
                ['Stock tracking', parent ? `Default for new variants: ${p.track_stock ? 'tracked' : 'not tracked'}` : yesNo(p.track_stock)],
                ...(!parent && p.track_stock ? [['Central stock', <StockPill quantity={p.central_quantity} threshold={p.low_stock_threshold} />] as [string, JSX.Element]] : []),
                ['Low-stock threshold', `${num(p.low_stock_threshold)} units`],
                ['Loyalty points', yesNo(p.loyalty_eligible)], ['Promotions', yesNo(p.promotion_eligible)],
                ['Status', <StatusPill active={p.active} />], ['Last updated', p.updated_at ? dateTime(p.updated_at) : '—'],
              ]} />
            </Panel>
            {parent ? <VariantSummary product={p} onOpen={() => setTab('variants')} /> : (
              <Panel title="Sales" icon={<Receipt size={19} />}>
                <Facts items={[['Units sold', num(p.stats.units_sold)], ['Transactions', num(p.stats.transactions)], ['Revenue (incl. add-ons)', xaf(p.stats.revenue_xaf)]]} />
              </Panel>
            )}
          </div>
        </>
      )}
      {tab === 'variants' && parent && <VariantsPanel product={p} canEdit={admin} onChanged={reload} />}
      {tab === 'addons' && !variant && <AddonsPanel product={p} canEdit={admin} onChanged={reload} />}
      {tab === 'rewards' && <RewardsTab productId={p.id} />}
      {tab === 'performance' && <PerformanceTab productId={p.id} parent={parent} />}
      {tab === 'stock' && <Panel title="Stock by Location" icon={<Boxes size={19} />}><StockByLocation product={p} /></Panel>}
      {tab === 'prices' && <Panel title="Price History" icon={<History size={19} />}><PriceHistoryTable productId={p.id} showVariant={parent} /></Panel>}
      {tab === 'activity' && <Panel title="Change History" icon={<Info size={19} />}><PagedActivity entityType="product" entityId={p.id} /></Panel>}
    </>
  );
}

function VariantSummary({ product, onOpen }: { product: ProductDetail; onOpen: () => void }) {
  return (
    <Panel title="Variant Summary" icon={<Layers size={19} />} action={<button className="link" onClick={onOpen}>Manage variants</button>}>
      {product.variants.length === 0 ? <p className="muted pad">No variants yet. Add sizes or packs to start selling this product.</p> : (
        <DataTable rows={product.variants} rowKey={v => v.id} columns={[
          { header: 'Variant', cell: v => v.variant_name },
          { header: 'Price', align: 'right', cell: v => xaf(v.price_xaf) },
          { header: 'Stock', align: 'right', cell: v => (v.track_stock ? num(v.central_quantity) : '—') },
          { header: 'Status', cell: v => <StatusPill active={v.active} /> },
        ]} />
      )}
      <p className="muted small">Total central stock: {num(centralUnits(product))} units.</p>
    </Panel>
  );
}

function StockByLocation({ product }: { product: ProductDetail }) {
  const nav = useNavigate();
  if (!product.stock.length) return <p className="muted pad">No stock has been received at any sales location yet.</p>;
  const showVariant = product.kind === 'PARENT';
  return (
    <DataTable rows={product.stock} rowKey={r => r.operation_id + r.product_id} onRowClick={r => nav(`/locations/${r.operation_id}`)} columns={[
      { header: 'Location', cell: r => <TitleCell title={r.operation_name} sub={r.code} /> },
      ...(showVariant ? [{ header: 'Variant', cell: (r: ProductDetail['stock'][number]) => r.variant_name ?? r.product_name }] : []),
      { header: 'Available', align: 'right', cell: r => num(r.quantity) },
      { header: 'Status', cell: r => <StockPill quantity={r.quantity} threshold={product.variants.find(v => v.id === r.product_id)?.low_stock_threshold ?? product.low_stock_threshold} /> },
    ]} />
  );
}
