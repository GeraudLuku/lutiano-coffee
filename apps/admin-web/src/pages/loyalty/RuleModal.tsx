import { useState } from 'react';
import { loyaltyApi, productsApi, categoriesApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { useLoad } from '../../hooks/useLoad';
import { ChoiceTiles, Field, FormGrid, FormModal, Switch } from '../../components/ui';
import { RULE_METHOD } from '../../utils/loyalty';
import type { EarningRule, RuleMethod, RuleScope } from '../../types/models';

export function RuleModal({ rule, onClose, onDone }: { rule?: EarningRule; onClose: () => void; onDone: () => void }) {
  const products = useLoad(() => productsApi.all('catalog'), []);
  const categories = useLoad(() => categoriesApi.list(), []);
  const [name, setName] = useState(rule?.name ?? '');
  const [method, setMethod] = useState<RuleMethod>(rule?.method ?? 'RATE');
  const [scope, setScope] = useState<RuleScope>(rule?.scope ?? 'CATEGORY');
  const [productId, setProductId] = useState(rule?.product_id ?? '');
  const [category, setCategory] = useState(rule?.category ?? '');
  const [points, setPoints] = useState(String(rule?.points ?? ''));
  const [amount, setAmount] = useState(String(rule?.amount_xaf ?? 1000));
  const [multiplier, setMultiplier] = useState(String(rule?.multiplier_pct ? rule.multiplier_pct / 100 : 2));
  const [startsOn, setStartsOn] = useState(rule?.starts_on?.slice(0, 10) ?? '');
  const [endsOn, setEndsOn] = useState(rule?.ends_on?.slice(0, 10) ?? '');
  const [active, setActive] = useState(rule?.active ?? true);
  const { busy, error, run } = useAction();
  const scopes: RuleScope[] = method === 'FIRST_PURCHASE' ? ['ALL'] : method === 'MULTIPLIER' ? ['ALL', 'CATEGORY', 'PRODUCT'] : ['CATEGORY', 'PRODUCT'];
  const effectiveScope = scopes.includes(scope) ? scope : scopes[0];

  async function submit() {
    const body = {
      name, method, scope: effectiveScope, productId: effectiveScope === 'PRODUCT' ? productId : null, category: effectiveScope === 'CATEGORY' ? category : null,
      points: Number(points) || 0, amountXaf: method === 'RATE' ? Number(amount) : null,
      multiplierPct: method === 'MULTIPLIER' ? Math.round(Number(multiplier) * 100) : null, startsOn: startsOn || null, endsOn: endsOn || null, active,
    };
    const ok = await run(() => (rule ? loyaltyApi.updateRule(rule.id, body) : loyaltyApi.createRule(body)), rule ? 'Rule updated.' : 'Rule added.');
    if (ok) onDone();
  }

  return (
    <FormModal title={rule ? 'Edit earning rule' : 'Add earning rule'} submitLabel={rule ? 'Save Rule' : 'Add Rule'} busy={busy} error={error} onSubmit={submit} onClose={onClose}>
      <Field label="Rule name" required><input className="input" value={name} onChange={e => setName(e.target.value)} required maxLength={100} placeholder="e.g. Arabica Beans Bonus" /></Field>
      <Field label="Earning method" required group>
        <ChoiceTiles name="method" value={method} onChange={setMethod}
          options={(Object.keys(RULE_METHOD) as RuleMethod[]).map(m => ({ value: m, label: RULE_METHOD[m][0], description: RULE_METHOD[m][1] }))} />
      </Field>
      {method !== 'FIRST_PURCHASE' && (
        <FormGrid>
          <Field label="Applies to" required>
            <select className="input" value={effectiveScope} onChange={e => setScope(e.target.value as RuleScope)}>
              {scopes.map(s => <option key={s} value={s}>{s === 'ALL' ? 'All products' : s === 'CATEGORY' ? 'A category' : 'A product (and its variants)'}</option>)}
            </select>
          </Field>
          {effectiveScope === 'CATEGORY' && (
            <Field label="Category" required>
              <select className="input" value={category} onChange={e => setCategory(e.target.value)} required>
                <option value="">Choose…</option>{(categories.data ?? []).map(c => <option key={c.id} value={c.name}>{c.name}</option>)}
              </select>
            </Field>
          )}
          {effectiveScope === 'PRODUCT' && (
            <Field label="Product" required>
              <select className="input" value={productId} onChange={e => setProductId(e.target.value)} required>
                <option value="">Choose…</option>{(products.data ?? []).filter(p => p.kind !== 'VARIANT').map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
              </select>
            </Field>
          )}
        </FormGrid>
      )}
      <FormGrid>
        {method === 'RATE' && <>
          <Field label="Points" required><input className="input" type="number" min={1} value={points} onChange={e => setPoints(e.target.value)} required /></Field>
          <Field label="Per amount spent (XAF)" required><input className="input" type="number" min={1} value={amount} onChange={e => setAmount(e.target.value)} required /></Field>
        </>}
        {(method === 'PER_UNIT' || method === 'FIRST_PURCHASE') && (
          <Field label={method === 'PER_UNIT' ? 'Points per unit' : 'Bonus points'} required><input className="input" type="number" min={1} value={points} onChange={e => setPoints(e.target.value)} required /></Field>
        )}
        {method === 'MULTIPLIER' && (
          <Field label="Multiplier" required hint="2 = double points"><input className="input" type="number" min={1.01} step={0.05} value={multiplier} onChange={e => setMultiplier(e.target.value)} required /></Field>
        )}
      </FormGrid>
      <FormGrid>
        <Field label="Starts on" hint="Optional: schedule a promotion"><input className="input" type="date" value={startsOn} onChange={e => setStartsOn(e.target.value)} /></Field>
        <Field label="Ends on"><input className="input" type="date" value={endsOn} min={startsOn || undefined} onChange={e => setEndsOn(e.target.value)} /></Field>
      </FormGrid>
      <Field label="Status" group><Switch checked={active} onChange={setActive} label={active ? 'Active' : 'Inactive'} /></Field>
    </FormModal>
  );
}
