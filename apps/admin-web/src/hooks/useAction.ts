import { useState } from 'react';
import { errorMessage } from '../utils/errors';
import { toast } from '../utils/toast';

/** Runs a mutation, tracks busy/error state and shows a success toast. */
export function useAction() {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  async function run<T>(fn: () => Promise<T>, success?: string): Promise<T | undefined> {
    setBusy(true); setError('');
    try {
      const result = await fn();
      if (success) toast(success);
      return result;
    } catch (e) {
      setError(errorMessage(e));
      return undefined;
    } finally { setBusy(false); }
  }
  return { busy, error, setError, run };
}
