/** Saves a Blob or text as a file in the browser. */
export function saveFile(data: Blob | string, filename: string, type = 'text/csv') {
  const blob = typeof data === 'string' ? new Blob([data], { type }) : data;
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url; a.download = filename; a.click();
  setTimeout(() => URL.revokeObjectURL(url), 2000);
}

export function toCsv<T extends object>(rows: T[], columns: [keyof T & string, string][]) {
  const esc = (v: unknown) => `"${String(v ?? '').replace(/"/g, '""')}"`;
  return [columns.map(c => esc(c[1])).join(','), ...rows.map(r => columns.map(c => esc((r as Record<string, unknown>)[c[0]])).join(','))].join('\n');
}
