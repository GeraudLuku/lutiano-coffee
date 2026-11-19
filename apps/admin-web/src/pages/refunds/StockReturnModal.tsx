import { useState } from 'react';
import { refundsApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { DataTable, Field, FormModal, Note } from '../../components/ui';
import type { RefundDetail } from '../../types/models';

/** Submits returned goods as CUSTOMER_RETURN stock adjustments; stock changes only once another manager approves them. */
export function StockReturnModal({ refund, secondApprover, onClose, onDone }: { refund: RefundDetail; secondApprover: boolean; onClose: () => void; onDone: () => void }) {
  const [qty, setQty] = useState<Record<string, string>>({});
  const [note, setNote] = useState('');
  const { busy, error, setError, run } = useAction();
  const lines = refund.returnable.filter(p => p.tracked && p.returnable > 0);

  async function submit() {
    const chosen = Object.entries(qty).map(([productId, q]) => ({ productId, quantity: Number(q) })).filter(l => l.quantity > 0);
    if (!chosen.length) { setError('Enter a quantity for at least one product.'); return; }
    const ok = await run(() => refundsApi.returnStock(refund.id, chosen, note), 'Returned stock submitted for approval.');
    if (ok) onDone();
  }

  return (
    <FormModal title="Record returned stock" submitLabel="Submit for Approval" busy={busy} error={error} onSubmit={submit} onClose={onClose}>
      <p className="muted">Only goods physically handed back and fit to sell again. They go back into {refund.operation_name}’s stock after approval.</p>
      {lines.length === 0 ? <Note tone="warn">Nothing on this sale can be returned to stock (already returned, or not stock-tracked).</Note> : (
        <DataTable rows={lines} rowKey={p => p.product_id} columns={[
          { header: 'Product', cell: p => p.product_name },
          { header: 'Sold', align: 'right', cell: p => p.sold },
          { header: 'Already returned', align: 'right', cell: p => p.returned },
          { header: 'Returning', align: 'right', cell: p => (
            <input className="input qty" type="number" min={0} max={p.returnable} step={1} aria-label={`Quantity of ${p.product_name} returned`}
              value={qty[p.product_id] ?? ''} onChange={e => setQty(prev => ({ ...prev, [p.product_id]: e.target.value }))} placeholder="0" />) },
        ]} />
      )}
      <Field label="Condition of the returned goods" required><textarea className="input" rows={2} value={note} onChange={e => setNote(e.target.value)} required maxLength={500} /></Field>
      <Note>Stock changes only after approval{secondApprover ? ' by another manager' : ''}.</Note>
    </FormModal>
  );
}
