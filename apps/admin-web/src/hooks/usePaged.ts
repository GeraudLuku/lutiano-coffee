import { useEffect, useState } from 'react';

/** Client-side pagination over an already-filtered list. */
export function usePaged<T>(rows: T[], size = 5) {
  const [page, setPage] = useState(1);
  const pages = Math.max(1, Math.ceil(rows.length / size));
  useEffect(() => { if (page > pages) setPage(1); }, [page, pages]);
  return { items: rows.slice((page - 1) * size, page * size), pager: { page, pages, total: rows.length, size, onPage: setPage } };
}
