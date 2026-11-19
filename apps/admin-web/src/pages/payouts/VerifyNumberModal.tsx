import { useState } from 'react';
import { payoutsApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { Field, FormModal, Note } from '../../components/ui';
import { PAYOUT_METHOD } from '../../utils/payouts';
import type { PayoutDestination } from '../../types/models';

/** Verifies a payout number with the code that was sent to it by SMS: the seller reads the code out. */
export function VerifyNumberModal({ destination: d, onClose, onDone }: { destination: PayoutDestination; onClose: () => void; onDone: () => void }) {
  const [code, setCode] = useState('');
  const { busy, error, run, setError } = useAction();
  const submit = async () => { if (await run(() => payoutsApi.verifyDestination(d.id, code), `${d.phone} verified.`)) onDone(); };
  async function resend() {
    setError('');
    if (await run(() => payoutsApi.resendCode(d.id), 'A new code was sent.')) setCode('');
  }
  return (
    <FormModal title="Verify payout number" submitLabel="Verify Number" busy={busy} error={error} onSubmit={submit} onClose={onClose}>
      <Note>A 6-digit code was sent by SMS to {PAYOUT_METHOD[d.provider]} number <b>{d.phone}</b>. Ask {d.seller_name} to read it out, then enter it here. It is valid for 5 minutes.</Note>
      <Field label="Code from the SMS" required>
        <input className="input" inputMode="numeric" pattern="[0-9]{6}" maxLength={6} value={code} onChange={e => setCode(e.target.value.replace(/\D/g, ''))} required autoFocus />
      </Field>
      <button type="button" className="link-btn" disabled={busy} onClick={resend}>Send a new code</button>
    </FormModal>
  );
}
