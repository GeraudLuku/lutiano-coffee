/** Case-insensitive "does any of these fields contain the query". */
export const matches = (query: string, ...fields: (string | null | undefined)[]) =>
  !query.trim() || fields.some(f => (f ?? '').toLowerCase().includes(query.trim().toLowerCase()));
