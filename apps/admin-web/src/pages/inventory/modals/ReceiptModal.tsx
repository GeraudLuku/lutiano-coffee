import { useState } from 'react';
import { inventoryApi } from '../../../api/services';
import { useAction } from '../../../hooks/useAction';
import { Field, FormGrid, FormModal } from '../../../components/ui';
import { RECEIPT_SOURCE } from '../../../utils/inventoryLabels';
import type { Product, ReceiptSource } from '../../../types/models';

/** Records stock arriving into central stock: a supplier delivery or an opening balance. */
export function ReceiptModal({ products, productId: initialProduct, onClose, onDone }: {
  products: Product[]; productId?: string; onClose: () => void; onDone: () => void;
}) {
  const [source, setSource] = useState<ReceiptSource>('SUPPLIER');
  const [productId, setProductId] = useState(initialProduct ?? '');
  const [quantity, setQuantity] = useState('');
  const [supplier, setSupplier] = useState('');
  const [reference, setReference] = useState('');
  const [note, setNote] = useState('');
  const { busy, error, run } = useAction();

  async function submit() {
    const ok = await run(() => inventoryApi.recordReceipt({ productId, quantity: Number(quantity), source, supplier, reference, note }),
      source === 'SUPPLIER' ? 'Supplier receipt recorded.' : 'Opening balance recorded.');
    if (ok !== undefined) onDone();
  }

  return (
    <FormModal title="Record stock receipt" submitLabel="Record Receipt" busy={busy} error={error} onSubmit={submit} onClose={onClose}>
      <p className="muted">Adds stock to central stock and to the stock ledger.</p>
      <Field label="Type" required>
        <select className="input" value={source} onChange={e => setSource(e.target.value as ReceiptSource)}>
          {Object.entries(RECEIPT_SOURCE).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
        </select>
      </Field>
      <Field label="Product" required>
        <select className="input" value={productId} onChange={e => setProductId(e.target.value)} required>
          <option value="">Choose product…</option>{products.map(p => <option key={p.id} value={p.id}>{p.name} ({p.sku})</option>)}
        </select>
      </Field>
      <FormGrid>
        <Field label="Quantity received" required><input className="input" type="number" min={1} step={1} value={quantity} onChange={e => setQuantity(e.target.value)} required /></Field>
        <Field label="Invoice / delivery note no."><input className="input" value={reference} onChange={e => setReference(e.target.value)} maxLength={80} /></Field>
      </FormGrid>
      {source === 'SUPPLIER' && (
        <Field label="Supplier" required><input className="input" value={supplier} onChange={e => setSupplier(e.target.value)} required maxLength={150} /></Field>
      )}
      <Field label="Note"><input className="input" value={note} onChange={e => setNote(e.target.value)} maxLength={500} /></Field>
    </FormModal>
  );
}
