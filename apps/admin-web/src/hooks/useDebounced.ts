import { useEffect, useState } from 'react';

/** The value, updated only after it stops changing for `ms` (search boxes that query the server). */
export function useDebounced<T>(value: T, ms = 300): T {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setDebounced(value), ms);
    return () => clearTimeout(t);
  }, [value, ms]);
  return debounced;
}

/** Text typed into a search box, with its debounced copy for the server: {@code [typed, setTyped, forTheServer]}. */
export function useDebouncedState(initial = '', ms = 300): [string, (v: string) => void, string] {
  const [value, setValue] = useState(initial);
  return [value, setValue, useDebounced(value, ms)];
}
