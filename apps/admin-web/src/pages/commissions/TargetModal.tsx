import { useState } from 'react';
import { productsApi, staffApi, targetsApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { useLoad } from '../../hooks/useLoad';
import { Field, FormGrid, FormModal, Note } from '../../components/ui';
import { doualaDate } from '../../utils/format';
import { PERIOD_TYPE } from '../../utils/earnings';
import type { SellerTarget, TargetInput } from '../../types/models';

/** Sets a target for one seller over a day, a week (Monday–Sunday), a month or custom dates. */
export function TargetModal({ sellerId, onClose, onDone }: { sellerId?: string; onClose: () => void; onDone: () => void }) {
  const sellers = useLoad(() => (sellerId ? Promise.resolve([]) : staffApi.all().then(all => all.filter(s => s.role === 'SELLER' && s.active))), [sellerId]);
  const products = useLoad(() => productsApi.all('catalog'), []);
  const [f, setF] = useState<TargetInput>({ sellerId: sellerId ?? '', name: '', metric: 'VALUE', productId: null, periodType: 'MONTHLY',
    startsOn: doualaDate(), endsOn: null, targetValue: 0, bonusType: 'FIXED', bonusXaf: 0 });
  const set = <K extends keyof TargetInput>(k: K, v: TargetInput[K]) => setF(p => ({ ...p, [k]: v }));
  const { busy, error, run } = useAction();

  async function submit() {
    if (await run(() => targetsApi.create(f), 'Target set.')) onDone();
  }

  return (
    <FormModal title="Set a sales target" submitLabel="Set Target" busy={busy} error={error} onSubmit={submit} onClose={onClose}>
      {!sellerId && (
        <Field label="Seller" required>
          <select className="input" value={f.sellerId} onChange={e => set('sellerId', e.target.value)} required>
            <option value="">Choose a seller…</option>{(sellers.data ?? []).map(s => <option key={s.id} value={s.id}>{s.full_name}</option>)}
          </select>
        </Field>
      )}
      <Field label="Target name" required><input className="input" value={f.name} onChange={e => set('name', e.target.value)} required maxLength={100} placeholder="e.g. Monthly sales December" /></Field>
      <FormGrid>
        <Field label="Measure" required>
          <select className="input" value={f.metric} onChange={e => set('metric', e.target.value as TargetInput['metric'])}>
            <option value="VALUE">Sales value (XAF)</option><option value="QUANTITY">Units sold</option>
          </select>
        </Field>
        <Field label="Product" hint="Optional: count one product (and its variants) only">
          <select className="input" value={f.productId ?? ''} onChange={e => set('productId', e.target.value || null)}>
            <option value="">All products</option>{(products.data ?? []).map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
          </select>
        </Field>
        <Field label="Period" required>
          <select className="input" value={f.periodType} onChange={e => set('periodType', e.target.value as SellerTarget['period_type'])}>
            {Object.entries(PERIOD_TYPE).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
          </select>
        </Field>
        <Field label={f.periodType === 'CUSTOM' ? 'From' : 'Starting in'} required hint={f.periodType === 'WEEKLY' ? 'The week (Monday–Sunday) containing this date' : f.periodType === 'MONTHLY' ? 'The month containing this date' : undefined}>
          <input className="input" type="date" value={f.startsOn} onChange={e => set('startsOn', e.target.value)} required />
        </Field>
        {f.periodType === 'CUSTOM' && (
          <Field label="To" required><input className="input" type="date" value={f.endsOn ?? ''} min={f.startsOn} onChange={e => set('endsOn', e.target.value)} required /></Field>
        )}
        <Field label={f.metric === 'VALUE' ? 'Target amount (XAF)' : 'Target units'} required>
          <input className="input" type="number" min={1} value={f.targetValue || ''} onChange={e => set('targetValue', Number(e.target.value))} required />
        </Field>
        <Field label="Bonus type" required>
          <select className="input" value={f.bonusType} onChange={e => set('bonusType', e.target.value as TargetInput['bonusType'])}>
            <option value="FIXED">Fixed amount</option><option value="PER_UNIT">Per unit sold</option>
          </select>
        </Field>
        <Field label={f.bonusType === 'FIXED' ? 'Bonus (XAF)' : 'Bonus per unit (XAF)'} required>
          <input className="input" type="number" min={0} value={f.bonusXaf || ''} onChange={e => set('bonusXaf', Number(e.target.value))} required />
        </Field>
      </FormGrid>
      <Note>The bonus is earned only if the target is reached. Only completed, paid sales count, and refunds reduce the total. It is paid with the earnings of the month the target ends in.</Note>
    </FormModal>
  );
}
