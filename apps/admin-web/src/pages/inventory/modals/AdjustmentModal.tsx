import { useMemo, useState } from 'react';
import { adjustmentsApi } from '../../../api/services';
import { useAction } from '../../../hooks/useAction';
import { Field, FormGrid, FormModal, Note } from '../../../components/ui';
import { ADJUSTMENT_REASON, CENTRAL } from '../../../utils/inventoryLabels';
import { num } from '../../../utils/format';
import type { AdjustmentReason, CentralStock, LocationStock, Operation } from '../../../types/models';

/** Requests a stock correction; it changes nothing until another manager approves. */
export function AdjustmentModal({ central, locationStock, operations, secondApprover, onClose, onDone }: {
  central: CentralStock[]; locationStock: LocationStock[]; operations: Operation[]; secondApprover: boolean; onClose: () => void; onDone: () => void;
}) {
  const [location, setLocation] = useState(CENTRAL);
  const [productId, setProductId] = useState('');
  const [reason, setReason] = useState<AdjustmentReason>('DAMAGED');
  const [direction, setDirection] = useState<1 | -1>(-1);
  const [quantity, setQuantity] = useState('');
  const [note, setNote] = useState('');
  const { busy, error, setError, run } = useAction();

  const reasons = (Object.keys(ADJUSTMENT_REASON) as AdjustmentReason[]).filter(r => r !== 'RETURN_TO_CENTRAL' || location !== CENTRAL);
  const fixed = ADJUSTMENT_REASON[reason][1];
  const sign = fixed === 0 ? direction : fixed;
  const products = useMemo(() => (location === CENTRAL
    ? central.map(c => ({ id: c.product_id, name: c.name, quantity: c.quantity }))
    : locationStock.filter(s => s.operation_id === location).map(s => ({ id: s.product_id, name: s.name, quantity: s.quantity }))), [location, central, locationStock]);
  const onHand = products.find(p => p.id === productId)?.quantity ?? 0;

  async function submit() {
    const qty = Number(quantity);
    if (sign < 0 && qty > onHand) { setError(`Only ${onHand} in stock at this location.`); return; }
    const ok = await run(() => adjustmentsApi.request({
      operationId: location === CENTRAL ? null : location, productId, quantityDelta: sign * qty, reason, note,
    }), 'Adjustment requested. It is waiting for approval.');
    if (ok) onDone();
  }

  return (
    <FormModal title="Request stock adjustment" submitLabel="Request Adjustment" busy={busy} error={error} onSubmit={submit} onClose={onClose}>
      <Field label="Location" required>
        <select className="input" value={location} onChange={e => { setLocation(e.target.value); setProductId(''); if (e.target.value === CENTRAL && reason === 'RETURN_TO_CENTRAL') setReason('DAMAGED'); }}>
          <option value={CENTRAL}>Central stock</option>
          {operations.map(o => <option key={o.id} value={o.id}>{o.name} ({o.code})</option>)}
        </select>
      </Field>
      <Field label="Product" required hint={location !== CENTRAL && products.length === 0 ? 'This location holds no stock yet.' : undefined}>
        <select className="input" value={productId} onChange={e => setProductId(e.target.value)} required>
          <option value="">Choose product…</option>
          {products.map(p => <option key={p.id} value={p.id}>{p.name} — {num(p.quantity)} on hand</option>)}
        </select>
      </Field>
      <FormGrid>
        <Field label="Reason" required>
          <select className="input" value={reason} onChange={e => setReason(e.target.value as AdjustmentReason)}>
            {reasons.map(r => <option key={r} value={r}>{ADJUSTMENT_REASON[r][0]}</option>)}
          </select>
        </Field>
        {fixed === 0 ? (
          <Field label="Change" required>
            <select className="input" value={direction} onChange={e => setDirection(Number(e.target.value) as 1 | -1)}>
              <option value={-1}>Remove stock</option><option value={1}>Add stock</option>
            </select>
          </Field>
        ) : <Field label="Change"><input className="input" readOnly value={fixed < 0 ? 'Remove stock' : 'Add stock'} /></Field>}
      </FormGrid>
      <Field label="Quantity" required hint={productId ? `${num(onHand)} on hand.` : undefined}>
        <input className="input" type="number" min={1} step={1} value={quantity} onChange={e => setQuantity(e.target.value)} required />
      </Field>
      <Field label="Explanation" required><textarea className="input" rows={2} value={note} onChange={e => setNote(e.target.value)} required maxLength={500} /></Field>
      <Note>{reason === 'RETURN_TO_CENTRAL' ? 'On approval, stock moves from this location back to central stock.' : 'Stock changes only after approval.'}{secondApprover && ' Another manager must approve your request.'}</Note>
    </FormModal>
  );
}
