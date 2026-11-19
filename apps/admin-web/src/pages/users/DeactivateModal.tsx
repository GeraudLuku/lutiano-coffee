import { useState } from 'react';
import { staffApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { Field, FormModal, Note } from '../../components/ui';

/** Deactivation needs a reason. It signs the person out everywhere; history and outstanding records are kept. */
export function DeactivateModal({ userId, name, seller, onClose, onDone }: { userId: string; name: string; seller: boolean; onClose: () => void; onDone: () => void }) {
  const [reason, setReason] = useState('');
  const { busy, error, run } = useAction();
  async function submit() {
    if (await run(() => staffApi.deactivate(userId, reason), `${name} deactivated.`)) onDone();
  }
  return (
    <FormModal title={`Deactivate ${name}?`} submitLabel="Deactivate" busy={busy} error={error} onSubmit={submit} onClose={onClose}>
      <p>{name} will be signed out of every device and unable to sign in. Their sales, stock and audit history are kept, and you can reactivate the account later.</p>
      {seller && <Note>A seller with an open cash shift cannot be deactivated until the shift is counted and closed.</Note>}
      <Field label="Reason" required><textarea className="input" rows={3} value={reason} onChange={e => setReason(e.target.value)} required maxLength={500} /></Field>
    </FormModal>
  );
}
