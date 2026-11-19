import { useState } from 'react';
import { payoutsApi, staffApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { useLoad } from '../../hooks/useLoad';
import { ChoiceTiles, Field, FormGrid, FormModal, Note, toast } from '../../components/ui';
import { PAYOUT_METHOD } from '../../utils/payouts';
import type { MobileProvider } from '../../types/models';

/** Registers a seller's mobile money number. It stays unusable until another administrator verifies it. */
export function DestinationModal({ sellerId: preset, onClose, onDone }: { sellerId?: string; onClose: () => void; onDone: () => void }) {
  const sellers = useLoad(async () => (preset ? [] : (await staffApi.all()).filter(s => s.role === 'SELLER' && s.active)), [preset]);
  const [sellerId, setSellerId] = useState(preset ?? '');
  const [provider, setProvider] = useState<MobileProvider>('MTN_MOMO');
  const [phone, setPhone] = useState('');
  const [accountName, setAccountName] = useState('');
  const [note, setNote] = useState('');
  const { busy, error, run } = useAction();
  async function submit() {
    const added = await run(() => payoutsApi.addDestination({ sellerId, provider, phone, accountName, note: note || undefined }));
    if (!added) return;
    if (added.code_sent) toast('Number added. A code was sent to it by SMS: enter it to verify the number.');
    else toast(`Number added, but the code could not be sent: ${added.code_error ?? 'try again'}. Use "Send a new code" when verifying.`, 'error');
    onDone();
  }
  return (
    <FormModal title="Add payout number" submitLabel="Add Number" busy={busy} error={error} onSubmit={submit} onClose={onClose}>
      {!preset && (
        <Field label="Seller" required>
          <select className="input" value={sellerId} onChange={e => setSellerId(e.target.value)} required>
            <option value="">Choose a seller…</option>
            {(sellers.data ?? []).map(s => <option key={s.id} value={s.id}>{s.full_name}</option>)}
          </select>
        </Field>
      )}
      <Field label="Provider" required group>
        <ChoiceTiles name="provider" value={provider} onChange={setProvider} columns={1}
          options={(['MTN_MOMO', 'ORANGE_MONEY'] as MobileProvider[]).map(p => ({ value: p, label: PAYOUT_METHOD[p], description: `A ${PAYOUT_METHOD[p]} number registered in the seller's name.` }))} />
      </Field>
      <FormGrid>
        <Field label="Mobile number" required hint="e.g. +237 6XX XX XX XX"><input className="input" value={phone} onChange={e => setPhone(e.target.value)} required maxLength={30} inputMode="tel" /></Field>
        <Field label="Account holder name" required hint="As registered with the provider"><input className="input" value={accountName} onChange={e => setAccountName(e.target.value)} required maxLength={150} /></Field>
      </FormGrid>
      <Field label="Note"><input className="input" value={note} onChange={e => setNote(e.target.value)} maxLength={255} placeholder="Optional" /></Field>
      <Note>A code is sent to this number by SMS. The seller reads it out and you enter it to verify the number. Verifying a new number replaces the seller's current one for that provider.</Note>
    </FormModal>
  );
}
