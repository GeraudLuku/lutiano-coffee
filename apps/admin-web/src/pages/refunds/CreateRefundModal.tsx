import { useEffect, useState } from 'react';
import { Search } from 'lucide-react';
import { refundsApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { Facts, Field, FormGrid, FormModal, Note } from '../../components/ui';
import { xaf } from '../../utils/format';
import { REFUND_REASON } from '../../utils/refunds';
import type { RefundableSale, RefundDetail, RefundReason } from '../../types/models';

/** Requests a full or partial refund on a sale. Opened from a sale (saleId) or from the refunds list (look up by reference). */
export function CreateRefundModal({ saleId, onClose, onDone }: { saleId?: string; onClose: () => void; onDone: (r: RefundDetail) => void }) {
  const [reference, setReference] = useState('');
  const [sale, setSale] = useState<RefundableSale | null>(null);
  const [amount, setAmount] = useState('');
  const [reason, setReason] = useState<RefundReason>('DAMAGED_PRODUCT');
  const [note, setNote] = useState('');
  const [customerName, setCustomerName] = useState('');
  const [customerPhone, setCustomerPhone] = useState('');
  const lookup = useAction();
  const save = useAction();

  async function find(q: { saleId?: string; saleReference?: string }) {
    const s = await lookup.run(() => refundsApi.sale(q));
    if (s) { setSale(s); setAmount(String(s.refundable_xaf)); }
  }
  useEffect(() => { if (saleId) void find({ saleId }); }, [saleId]); // eslint-disable-line react-hooks/exhaustive-deps

  async function submit() {
    if (!sale) { if (reference.trim()) await find({ saleReference: reference.trim() }); return; }
    const r = await save.run(() => refundsApi.request({
      saleId: sale.id, amountXaf: Number(amount), reason, note: note || undefined,
      ...(sale.customer_id ? {} : { customerName: customerName || undefined, customerPhone: customerPhone || undefined }),
    }), 'Refund requested. It now needs approval.');
    if (r) onDone(r);
  }

  const value = Number(amount);
  const nothingLeft = sale != null && sale.refundable_xaf <= 0;
  return (
    <FormModal title="Create refund" submitLabel={sale ? 'Request Refund' : 'Find Sale'} busy={lookup.busy || save.busy}
      error={lookup.error || save.error} onSubmit={submit} onClose={onClose}>
      {!saleId && (
        <Field label="Original sale reference" required hint="e.g. SL-20261116-00001">
          <div className="input-action">
            <input className="input" value={reference} onChange={e => { setReference(e.target.value); setSale(null); }} placeholder="SL-…" required={!sale} autoFocus />
            <button type="button" className="btn" disabled={!reference.trim() || lookup.busy} onClick={() => find({ saleReference: reference.trim() })}><Search size={15} />Find</button>
          </div>
        </Field>
      )}
      {sale && (
        <>
          <Facts items={[
            ['Sale', sale.reference], ['Customer', sale.customer_id ? `${sale.customer_name} (${sale.customer_code}) · ${sale.customer_phone}` : 'Anonymous sale'],
            ['Sale total', xaf(sale.total_xaf)], ['Already refunded or requested', xaf(sale.committed_xaf)],
            ['Can still be refunded', <b>{xaf(sale.refundable_xaf)}</b>],
          ]} />
          {nothingLeft ? <Note tone="warn">Nothing left to refund on this sale.</Note> : (
            <>
              <FormGrid>
                <Field label="Refund amount (XAF)" required hint={value === sale.total_xaf ? 'Full refund' : value > 0 ? 'Partial refund' : undefined}>
                  <input className="input" type="number" min={1} max={sale.refundable_xaf} step={1} value={amount} onChange={e => setAmount(e.target.value)} required />
                </Field>
                <Field label="Reason" required>
                  <select className="input" value={reason} onChange={e => setReason(e.target.value as RefundReason)}>
                    {(Object.keys(REFUND_REASON) as RefundReason[]).map(r => <option key={r} value={r}>{REFUND_REASON[r]}</option>)}
                  </select>
                </Field>
                {!sale.customer_id && <>
                  <Field label="Customer name" hint="Anonymous sale: who is being refunded?"><input className="input" value={customerName} onChange={e => setCustomerName(e.target.value)} maxLength={150} /></Field>
                  <Field label="Customer phone"><input className="input" value={customerPhone} onChange={e => setCustomerPhone(e.target.value)} maxLength={30} placeholder="+237 6XX XX XX XX" /></Field>
                </>}
              </FormGrid>
              <Field label={reason === 'OTHER' ? 'Explanation' : 'Note'} required={reason === 'OTHER'}>
                <textarea className="input" rows={2} value={note} onChange={e => setNote(e.target.value)} required={reason === 'OTHER'} maxLength={500} />
              </Field>
              <Note>Approval and payment are separate steps: another administrator approves, then the cash refund is recorded. Refunds do not put stock back automatically.</Note>
            </>
          )}
        </>
      )}
    </FormModal>
  );
}
