import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import { loadSession, saveSession, SIGNED_OUT_EVENT } from '../api/client';
import { authApi } from '../api/services';
import type { MfaChallenge, Role, Session } from '../types/models';

interface AuthValue {
  session: Session | null;
  /** Resolves with a challenge when an SMS code is needed, otherwise the user is signed in. */
  signIn: (email: string, password: string) => Promise<MfaChallenge | null>;
  confirmCode: (challenge: string, code: string) => Promise<void>;
  signOut: () => void;
  /** True when the signed-in user has one of the roles. UI hint only; the server enforces access. */
  can: (...roles: Role[]) => boolean;
}

const AuthContext = createContext<AuthValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(() => loadSession());
  useEffect(() => {
    const out = () => setSession(null);
    window.addEventListener(SIGNED_OUT_EVENT, out);
    return () => window.removeEventListener(SIGNED_OUT_EVENT, out);
  }, []);
  function accept(s: Session) {
    if (s.user.role === 'SELLER') {
      saveSession(null);
      throw new Error('Seller accounts use the LUTIANO Seller app, not the Admin Portal.');
    }
    setSession(s);
  }
  const value: AuthValue = {
    session,
    async signIn(email, password) {
      const s = await authApi.login(email, password);
      if ('mfaRequired' in s) return s;
      accept(s);
      return null;
    },
    async confirmCode(challenge, code) { accept(await authApi.verifyMfa(challenge, code)); },
    signOut() { saveSession(null); setSession(null); },
    can: (...roles) => !!session && roles.includes(session.user.role),
  };
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}
