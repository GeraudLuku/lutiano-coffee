import { useState } from 'react';
import { transfersApi } from '../../../api/services';
import { useAction } from '../../../hooks/useAction';
import { Field, FormModal } from '../../../components/ui';
import type { Allocation } from '../../../types/models';

/** Confirms the quantity that arrived at the location (normally done by the seller in the app). */
export function ConfirmReceiptModal({ allocation, onClose, onDone }: { allocation: Allocation; onClose: () => void; onDone: () => void }) {
  const [quantity, setQuantity] = useState(String(allocation.sent_quantity));
  const [note, setNote] = useState('');
  const { busy, error, run } = useAction();
  const differs = Number(quantity) !== allocation.sent_quantity;
  const submit = async () => {
    if (await run(() => transfersApi.confirm(allocation.id, Number(quantity), note), differs ? 'Receipt confirmed; discrepancy recorded.' : 'Receipt confirmed.')) onDone();
  };
  return (
    <FormModal title="Confirm receipt" submitLabel="Confirm Receipt" busy={busy} error={error} onSubmit={submit} onClose={onClose}>
      <p><b>{allocation.sent_quantity} × {allocation.product_name}</b> sent to <b>{allocation.operation_name}</b>.</p>
      <Field label="Quantity actually received" required>
        <input className="input" type="number" min={0} max={allocation.sent_quantity} step={1} value={quantity} onChange={e => setQuantity(e.target.value)} required />
      </Field>
      {differs && <Field label="Reason for the difference" required><textarea className="input" rows={2} value={note} onChange={e => setNote(e.target.value)} required maxLength={500} /></Field>}
    </FormModal>
  );
}
