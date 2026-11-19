import { useState } from 'react';
import { cashApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { Facts, Field, FormModal, Note } from '../../components/ui';
import { dateTime, xaf } from '../../utils/format';
import { REMITTANCE_METHOD } from '../../utils/cash';
import type { CashRemittance } from '../../types/models';

/** The recipient confirms the cash actually received. A different amount marks the remittance disputed and needs an explanation. */
export function AcknowledgeModal({ remittance: r, onClose, onDone }: { remittance: CashRemittance; onClose: () => void; onDone: () => void }) {
  const [received, setReceived] = useState(String(r.amount_xaf));
  const [note, setNote] = useState('');
  const { busy, error, run } = useAction();
  const differs = received !== '' && Number(received) !== Number(r.amount_xaf);

  async function submit() {
    const ok = await run(() => cashApi.acknowledge(r.id, Number(received), note || undefined), differs ? 'Remittance recorded as disputed.' : 'Remittance acknowledged.');
    if (ok) onDone();
  }

  return (
    <FormModal title={`Acknowledge ${r.reference}`} submitLabel={differs ? 'Record Difference' : 'Acknowledge'} busy={busy} error={error} onSubmit={submit} onClose={onClose}>
      <Facts items={[
        ['From', r.seller_name], ['Method', REMITTANCE_METHOD[r.method]], ['Recorded', dateTime(r.recorded_at)],
        ...(r.bank_reference ? [['Deposit slip', r.bank_reference] as [string, string]] : []), ['Amount sent', <b>{xaf(r.amount_xaf)}</b>],
      ]} />
      <Field label={r.method === 'BANK_DEPOSIT' ? 'Amount credited by the bank (XAF)' : 'Amount you received (XAF)'} required>
        <input className="input" type="number" min={0} step={1} value={received} onChange={e => setReceived(e.target.value)} required />
      </Field>
      <Field label={differs ? 'Explain the difference' : 'Note'} required={differs}>
        <textarea className="input" rows={2} value={note} onChange={e => setNote(e.target.value)} required={differs} maxLength={500} />
      </Field>
      {differs && <Note tone="warn">The remittance will be marked disputed: {xaf(Number(received) - Number(r.amount_xaf))} difference.</Note>}
    </FormModal>
  );
}
