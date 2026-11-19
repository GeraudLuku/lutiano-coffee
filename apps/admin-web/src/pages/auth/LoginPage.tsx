import { useEffect, useState, type FormEvent } from 'react';
import { Eye, EyeOff, Lock, Mail, ShieldCheck } from 'lucide-react';
import { authApi } from '../../api/services';
import { useAuth } from '../../auth/AuthContext';
import { Logo } from '../../components/ui';
import { errorMessage } from '../../utils/errors';
import type { MfaChallenge } from '../../types/models';

export function LoginPage() {
  const { signIn, confirmCode } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [show, setShow] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [challenge, setChallenge] = useState<MfaChallenge | null>(null);
  const [code, setCode] = useState('');
  const [wait, setWait] = useState(0);

  useEffect(() => {
    if (wait <= 0) return;
    const t = setTimeout(() => setWait(w => w - 1), 1000);
    return () => clearTimeout(t);
  }, [wait]);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true); setError('');
    try {
      const c = await signIn(email, password);
      if (c) { setChallenge(c); setWait(c.resendAfterSeconds); }
    } catch (x) { setError(errorMessage(x)); } finally { setBusy(false); }
  }

  async function verify(e: FormEvent) {
    e.preventDefault();
    setBusy(true); setError('');
    try { await confirmCode(challenge!.challenge, code); } catch (x) { setError(errorMessage(x)); } finally { setBusy(false); }
  }

  async function resend() {
    setError('');
    try { setWait((await authApi.resendMfa(challenge!.challenge)).resendAfterSeconds); setCode(''); } catch (x) { setError(errorMessage(x)); }
  }

  const back = () => { setChallenge(null); setCode(''); setError(''); setPassword(''); };

  return (
    <div className="login">
      <div className="login-brand"><Logo size={64} /><b>LUTIANO</b><small>Coffee Business OS</small></div>
      {challenge ? (
        <form onSubmit={verify} className="login-card">
          <h1>Confirm it's you</h1>
          <p>We sent a 6-digit code by SMS to {challenge.phone}.{challenge.enrolment ? ' Entering it confirms this phone for two-step sign-in.' : ''}</p>
          <label className="input-icon"><ShieldCheck size={17} aria-hidden="true" />
            <input inputMode="numeric" autoComplete="one-time-code" pattern="[0-9]{6}" maxLength={6} placeholder="6-digit code" aria-label="SMS code" autoFocus
              value={code} onChange={e => setCode(e.target.value.replace(/\D/g, ''))} required />
          </label>
          {error && <div className="error-note" role="alert">{error}</div>}
          <button className="btn gold block" disabled={busy || code.length !== 6}>{busy ? 'Checking…' : 'Verify and Sign In'}</button>
          <div className="login-links">
            <button type="button" className="link-btn" disabled={wait > 0} onClick={resend}>{wait > 0 ? `Send a new code in ${wait}s` : 'Send a new code'}</button>
            <button type="button" className="link-btn" onClick={back}>Back</button>
          </div>
        </form>
      ) : (
        <form onSubmit={submit} className="login-card">
          <h1>Welcome back</h1>
          <p>Sign in to the Admin Portal</p>
          <label className="input-icon"><Mail size={17} aria-hidden="true" />
            <input type="email" autoComplete="username" placeholder="Staff email" aria-label="Email" value={email} onChange={e => setEmail(e.target.value)} required />
          </label>
          <label className="input-icon"><Lock size={17} aria-hidden="true" />
            <input type={show ? 'text' : 'password'} autoComplete="current-password" placeholder="Password" aria-label="Password" value={password} onChange={e => setPassword(e.target.value)} required />
            <button type="button" className="icon-btn" onClick={() => setShow(s => !s)} aria-label={show ? 'Hide password' : 'Show password'}>{show ? <EyeOff size={17} /> : <Eye size={17} />}</button>
          </label>
          {error && <div className="error-note" role="alert">{error}</div>}
          <button className="btn gold block" disabled={busy}>{busy ? 'Signing in…' : 'Sign In'}</button>
          <small className="muted">Forgot your password? Ask an administrator to reset your access.</small>
        </form>
      )}
    </div>
  );
}
