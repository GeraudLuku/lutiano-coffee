import { useState } from 'react';
import { commissionsApi, productsApi } from '../../api/services';
import { useLoad } from '../../hooks/useLoad';
import { usePaged } from '../../hooks/usePaged';
import { DataTable, Empty, ErrorNote, Loading, Pager, Panel, SearchInput, TitleCell } from '../../components/ui';
import { SELLER_ROLE_LABEL } from '../../utils/labels';
import { rateLabel } from '../../utils/earnings';
import { matches } from '../../utils/text';
import type { CommissionRule, Product, SellerRole } from '../../types/models';
import { RuleModal } from './RuleModal';

const ROLES = Object.keys(SELLER_ROLE_LABEL) as SellerRole[];

/** Commission matrix: one rule per product and seller role. Variants without their own rule use their parent's. */
export function RulesTab() {
  const data = useLoad(async () => {
    const [products, rules] = await Promise.all([productsApi.all('all'), commissionsApi.rules()]);
    return { products: products.filter(p => p.active), rules };
  }, []);
  const [q, setQ] = useState('');
  const [editing, setEditing] = useState<{ product: Product; role: SellerRole; rule?: CommissionRule } | null>(null);
  const rows = (data.data?.products ?? []).filter(p => matches(q, p.name, p.sku, p.category));
  const { items, pager } = usePaged(rows);
  const ruleOf = (productId: string, role: SellerRole) => data.data?.rules.find(r => r.product_id === productId && r.seller_role === role);
  const parentOf = (p: Product) => data.data?.products.find(x => x.id === p.parent_id);

  return (
    <Panel title="Commission Rules" action={<SearchInput value={q} onChange={setQ} placeholder="Product, SKU or category…" />}>
      <p className="muted small">Click a cell to set or change the commission. Percentages apply to the paid line value; fixed amounts apply per unit.</p>
      <ErrorNote error={data.error} retry={data.reload} />
      {data.loading && !data.data ? <Loading /> : !rows.length ? <Empty title="No products found" /> : (
        <DataTable rows={items} rowKey={p => p.id} columns={[
          { header: 'Product', cell: p => <TitleCell title={p.name} sub={p.kind === 'PARENT' ? 'Applies to its variants' : p.sku} /> },
          ...ROLES.map(role => ({
            header: SELLER_ROLE_LABEL[role], cell: (p: Product) => {
              const own = ruleOf(p.id, role), inherited = !own && p.parent_id ? ruleOf(p.parent_id, role) : undefined;
              return (
                <button className={`rate-cell ${own ? 'set' : inherited ? 'inherited' : ''}`} onClick={() => setEditing({ product: p, role, rule: own })}>
                  {own ? rateLabel(own.basis, own.rate) : inherited ? <>{rateLabel(inherited.basis, inherited.rate)}<small>from {parentOf(p)?.name ?? 'parent'}</small></> : 'Not set'}
                </button>
              );
            },
          })),
        ]} />
      )}
      <Pager {...pager} />
      {editing && <RuleModal {...editing} onClose={() => setEditing(null)} onDone={() => { setEditing(null); data.reload(); }} />}
    </Panel>
  );
}
