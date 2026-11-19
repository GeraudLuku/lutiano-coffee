import { useState } from 'react';
import { payoutsApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { Field, FormModal, Note } from '../../components/ui';
import { xaf } from '../../utils/format';
import { PAYOUT_METHOD } from '../../utils/payouts';
import type { PayoutDetail } from '../../types/models';

/** Confirms the money was handed over. Mobile money needs the provider's transaction reference (it can be used for one payout only). */
export function PayPayoutModal({ payout, onClose, onDone }: { payout: PayoutDetail; onClose: () => void; onDone: () => void }) {
  const mobile = payout.method !== 'CASH';
  const [reference, setReference] = useState('');
  const [note, setNote] = useState('');
  const { busy, error, run } = useAction();
  async function submit() {
    if (await run(() => payoutsApi.pay(payout.id, { providerReference: mobile ? reference : undefined, note: note || undefined }), `Payout ${payout.reference} marked as paid.`)) onDone();
  }
  return (
    <FormModal title={`Mark as paid · ${xaf(payout.amount_xaf)}`} submitLabel="Confirm Payment" busy={busy} error={error} onSubmit={submit} onClose={onClose}>
      <Note>{mobile
        ? `Confirm only after ${PAYOUT_METHOD[payout.method]} has sent ${xaf(payout.amount_xaf)} to ${payout.destination_name} · ${payout.destination_phone}.`
        : `Confirm only after ${xaf(payout.amount_xaf)} in cash has been handed to ${payout.seller_name}.`} Add the transfer screenshot or signed receipt as evidence afterwards.</Note>
      {mobile && (
        <Field label="Provider transaction reference" required hint="From the MoMo / Orange Money confirmation. It cannot be reused for another payout.">
          <input className="input" value={reference} onChange={e => setReference(e.target.value)} required maxLength={80} />
        </Field>
      )}
      <Field label="Note"><input className="input" value={note} onChange={e => setNote(e.target.value)} maxLength={500} placeholder="Optional" /></Field>
    </FormModal>
  );
}
