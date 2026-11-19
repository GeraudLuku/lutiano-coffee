import { useState } from 'react';
import { staffApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { Field, FormModal } from '../../components/ui';

export function ResetAccessModal({ userId, name, onClose }: { userId: string; name: string; onClose: () => void }) {
  const [password, setPassword] = useState('');
  const { busy, error, run } = useAction();
  async function submit() {
    if (await run(() => staffApi.resetPassword(userId, password), 'Temporary password set.')) onClose();
  }
  return (
    <FormModal title="Reset access" submitLabel="Set Password" busy={busy} error={error} onSubmit={submit} onClose={onClose}>
      <p>Set a new temporary password for <b>{name}</b>. Share it with them securely.</p>
      <Field label="Temporary password" required hint="At least 10 characters.">
        <input className="input" type="password" autoComplete="new-password" value={password} onChange={e => setPassword(e.target.value)} minLength={10} maxLength={72} required />
      </Field>
    </FormModal>
  );
}
