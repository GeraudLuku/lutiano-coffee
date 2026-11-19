import { useState } from 'react';
import { commissionsApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { usePagedQuery } from '../../hooks/usePagedQuery';
import { ChoiceTiles, DataTable, Field, FormGrid, FormModal, Note, Pager } from '../../components/ui';
import { dateTime } from '../../utils/format';
import { SELLER_ROLE_LABEL } from '../../utils/labels';
import { rateLabel } from '../../utils/earnings';
import type { CommissionBasis, CommissionRule, Product, SellerRole } from '../../types/models';

/** Sets (or ends) the commission of one product for one seller role; previous rates stay in the history below. */
export function RuleModal({ product, role, rule, onClose, onDone }: {
  product: Product; role: SellerRole; rule?: CommissionRule; onClose: () => void; onDone: () => void;
}) {
  const [basis, setBasis] = useState<CommissionBasis>(rule?.basis ?? 'PERCENT');
  const [rate, setRate] = useState(rule ? String(rule.basis === 'PERCENT' ? rule.rate / 100 : rule.rate) : '');
  const [note, setNote] = useState('');
  const save = useAction();
  const end = useAction();
  const history = usePagedQuery((page, size) => commissionsApi.history(product.id, role, { page, size }), [product.id, role]);

  async function submit() {
    const value = Number(rate);
    const ok = await save.run(() => commissionsApi.saveRule({ productId: product.id, sellerRole: role, basis, rate: basis === 'PERCENT' ? Math.round(value * 100) : Math.round(value), note }),
      'Commission rule saved. It applies to sales from now on.');
    if (ok) onDone();
  }

  return (
    <FormModal title={`${product.name} · ${SELLER_ROLE_LABEL[role]}`} submitLabel={rule ? 'Change Rate' : 'Set Commission'} busy={save.busy} error={save.error || end.error}
      onSubmit={submit} onClose={onClose}>
      <Field label="Commission basis" required group>
        <ChoiceTiles name="basis" value={basis} onChange={setBasis} options={[
          { value: 'PERCENT', label: 'Percentage', description: 'Of the paid line value (add-ons included)' },
          { value: 'FIXED_PER_UNIT', label: 'Fixed per unit', description: 'XAF for every unit sold' },
        ]} />
      </Field>
      <FormGrid>
        <Field label={basis === 'PERCENT' ? 'Rate (%)' : 'Amount per unit (XAF)'} required>
          <input className="input" type="number" min={basis === 'PERCENT' ? 0.01 : 1} max={basis === 'PERCENT' ? 100 : undefined} step={basis === 'PERCENT' ? 0.01 : 1}
            value={rate} onChange={e => setRate(e.target.value)} required />
        </Field>
        <Field label="Note"><input className="input" value={note} onChange={e => setNote(e.target.value)} maxLength={255} placeholder="e.g. Launch rates" /></Field>
      </FormGrid>
      <Note>{product.kind === 'PARENT' ? 'Applies to every variant without its own rule. ' : ''}Changes apply to sales from now on; past sales keep the rate they were sold at.</Note>
      {rule && (
        <button type="button" className="btn danger sm" disabled={end.busy}
          onClick={async () => { if (await end.run(() => commissionsApi.endRule(rule.id), 'Commission stopped for this product and role.') !== undefined) onDone(); }}>
          Stop paying commission
        </button>
      )}
      {history.items.length > 0 && <>
        <h4 className="subhead">Rate history</h4>
        <DataTable rows={history.items} rowKey={r => r.id} columns={[
          { header: 'Rate', cell: r => <b>{rateLabel(r.basis, r.rate)}</b> },
          { header: 'From', cell: r => dateTime(r.created_at) },
          { header: 'Until', cell: r => (r.superseded_at ? dateTime(r.superseded_at) : 'Current') },
          { header: 'By', cell: r => r.created_by_name ?? '—' },
        ]} />
        <Pager {...history.pager} />
      </>}
    </FormModal>
  );
}
