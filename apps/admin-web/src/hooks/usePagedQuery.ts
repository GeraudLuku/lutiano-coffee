import { useCallback, useEffect, useRef, useState } from 'react';
import { errorMessage } from '../utils/errors';
import type { Page } from '../types/models';

/** Rows per page for every paginated list (matches the API default). */
export const DEFAULT_PAGE_SIZE = 5;

/**
 * Server-side pagination: loads page `page` of `size` rows from `fetchPage` and goes back to page 1 whenever `deps`
 * (filters, search) change. Returns the page, its rows, the Pager props and reload.
 */
export function usePagedQuery<T, S = Record<string, number>>(fetchPage: (page: number, size: number) => Promise<Page<T, S>>, deps: unknown[], size = DEFAULT_PAGE_SIZE) {
  const [page, setPage] = useState(1);
  const [data, setData] = useState<Page<T, S> | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const depsKey = JSON.stringify(deps);
  const lastKey = useRef(depsKey);
  const current = lastKey.current === depsKey ? page : 1;
  const reload = useCallback(async () => {
    setLoading(true);
    try { setData(await fetchPage(current, size)); setError(''); } catch (e) { setError(errorMessage(e)); } finally { setLoading(false); }
  // The caller's own dependency list decides when to reload; the loader function is deliberately not a dependency.
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [depsKey, current, size]);
  useEffect(() => {
    if (lastKey.current !== depsKey) { lastKey.current = depsKey; setPage(1); }
    reload();
  }, [reload, depsKey]);
  return {
    data, error, loading, reload,
    items: data?.items ?? [],
    summary: data?.summary,
    pager: { page: data?.page ?? current, pages: data?.pages ?? 1, total: data?.total ?? 0, size, onPage: setPage, truncated: data?.truncated },
  };
}
