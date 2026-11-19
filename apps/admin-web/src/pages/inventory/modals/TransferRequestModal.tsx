import { useState } from 'react';
import { Truck } from 'lucide-react';
import { transfersApi } from '../../../api/services';
import { useAction } from '../../../hooks/useAction';
import { Field, FormModal, Note } from '../../../components/ui';
import { num } from '../../../utils/format';
import type { CentralStock, Operation } from '../../../types/models';

export interface TransferPrefill { operationId?: string; productId?: string; quantity?: number }

/** Creates a transfer request. Nothing moves until a manager approves it. */
export function TransferRequestModal({ central, operations, prefill, secondApprover, onClose, onDone }: {
  central: CentralStock[]; operations: Operation[]; prefill?: TransferPrefill; secondApprover: boolean; onClose: () => void; onDone: () => void;
}) {
  const [operationId, setOperationId] = useState(prefill?.operationId ?? '');
  const [productId, setProductId] = useState(prefill?.productId ?? '');
  const [quantity, setQuantity] = useState(prefill?.quantity ? String(prefill.quantity) : '');
  const [note, setNote] = useState('');
  const { busy, error, run } = useAction();
  const available = Number(central.find(c => c.product_id === productId)?.quantity ?? 0);

  async function submit() {
    if (await run(() => transfersApi.request(operationId, productId, Number(quantity), note), 'Transfer requested. It is waiting for approval.')) onDone();
  }

  return (
    <FormModal title="Request stock transfer" submitLabel="Request Transfer" busy={busy} error={error} onSubmit={submit} onClose={onClose}>
      <Field label="To sales location" required>
        <select className="input" value={operationId} onChange={e => setOperationId(e.target.value)} required>
          <option value="">Choose location…</option>{operations.filter(o => o.active).map(o => <option key={o.id} value={o.id}>{o.name} ({o.code})</option>)}
        </select>
      </Field>
      <Field label="Product" required>
        <select className="input" value={productId} onChange={e => setProductId(e.target.value)} required>
          <option value="">Choose product…</option>
          {central.filter(c => c.active).map(c => <option key={c.product_id} value={c.product_id}>{c.name} — {num(c.quantity)} in central stock</option>)}
        </select>
      </Field>
      <Field label="Quantity" required hint={productId ? `${num(available)} units currently in central stock. Availability is checked again on approval.` : undefined}>
        <input className="input" type="number" min={1} step={1} value={quantity} onChange={e => setQuantity(e.target.value)} required />
      </Field>
      <Field label="Note for the approver"><input className="input" value={note} onChange={e => setNote(e.target.value)} maxLength={500} placeholder="e.g. Weekend event" /></Field>
      <Note icon={<Truck size={16} />}>
        Requested → approved (stock set aside) → dispatched → confirmed by the location.{secondApprover && ' Another manager must approve your request.'}
      </Note>
    </FormModal>
  );
}
