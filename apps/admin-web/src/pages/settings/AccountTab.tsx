import { useState, type FormEvent } from 'react';
import { KeyRound, User } from 'lucide-react';
import { authApi } from '../../api/services';
import { useAuth } from '../../auth/AuthContext';
import { useAction } from '../../hooks/useAction';
import { ErrorNote, Facts, Field, Panel } from '../../components/ui';
import { RolePill } from '../../components/domain';

export function AccountTab() {
  const { session } = useAuth();
  const user = session!.user;
  const [current, setCurrent] = useState('');
  const [next, setNext] = useState('');
  const [confirm, setConfirm] = useState('');
  const { busy, error, setError, run } = useAction();

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (next !== confirm) { setError('The new passwords do not match.'); return; }
    if (await run(() => authApi.changePassword(current, next), 'Password changed.')) { setCurrent(''); setNext(''); setConfirm(''); }
  }

  return (
    <div className="grid-2">
      <Panel title="My Profile" icon={<User size={19} />}>
        <Facts items={[['Name', user.full_name], ['Email', user.email], ['Phone', user.phone ?? '—'], ['Role', <RolePill role={user.role} />]]} />
        <p className="muted small">Ask an administrator to change your name, email or role.</p>
      </Panel>
      <Panel title="Change Password" icon={<KeyRound size={19} />}>
        <form className="stack" onSubmit={submit}>
          <Field label="Current password" required><input className="input" type="password" autoComplete="current-password" value={current} onChange={e => setCurrent(e.target.value)} required /></Field>
          <Field label="New password" required hint="At least 10 characters."><input className="input" type="password" autoComplete="new-password" minLength={10} maxLength={72} value={next} onChange={e => setNext(e.target.value)} required /></Field>
          <Field label="Confirm new password" required><input className="input" type="password" autoComplete="new-password" value={confirm} onChange={e => setConfirm(e.target.value)} required /></Field>
          <ErrorNote error={error} />
          <div><button className="btn gold" disabled={busy}>{busy ? 'Saving…' : 'Change Password'}</button></div>
        </form>
      </Panel>
    </div>
  );
}
