import { useCallback, useEffect, useState } from 'react';
import { errorMessage } from '../utils/errors';

/** Loads data on mount and whenever `deps` change; exposes loading/error state and a reload function. */
export function useLoad<T>(fn: () => Promise<T>, deps: unknown[]) {
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const reload = useCallback(async () => {
    setLoading(true);
    try { setData(await fn()); setError(''); } catch (e) { setError(errorMessage(e)); } finally { setLoading(false); }
  // The caller's own dependency list decides when to reload; the loader function is deliberately not a dependency.
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);
  useEffect(() => { reload(); }, [reload]);
  return { data, error, loading, reload };
}
