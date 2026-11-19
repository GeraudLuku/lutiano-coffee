import type { Page } from '../types/models';

const MAX_PAGE = 200;

/**
 * Every row of a paginated endpoint, fetched page by page (largest page size). For dropdowns and CSV exports;
 * screens that show lists page on the server instead.
 */
export async function fetchAll<T>(fetchPage: (page: number, size: number) => Promise<Page<T>>, limit = 10_000): Promise<T[]> {
  const out: T[] = [];
  for (let page = 1; ; page++) {
    const p = await fetchPage(page, MAX_PAGE);
    out.push(...p.items);
    if (page >= p.pages || out.length >= limit) return out;
  }
}
