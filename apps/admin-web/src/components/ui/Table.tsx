import type { ReactNode } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { num } from '../../utils/format';

export interface Column<T> {
  header: ReactNode;
  cell: (row: T) => ReactNode;
  align?: 'right';
  className?: string;
}

/** Generic table: columns describe headers and cells; rows can be clickable. */
export function DataTable<T>({ rows, columns, rowKey, onRowClick }: {
  rows: T[]; columns: Column<T>[]; rowKey: (row: T) => string; onRowClick?: (row: T) => void;
}) {
  return (
    <div className="table-wrap">
      <table>
        <thead><tr>{columns.map((c, i) => <th key={i} className={c.align === 'right' ? 'num' : undefined}>{c.header}</th>)}</tr></thead>
        <tbody>
          {rows.map(r => (
            <tr key={rowKey(r)} className={onRowClick ? 'clickable' : undefined} onClick={onRowClick ? () => onRowClick(r) : undefined}>
              {columns.map((c, i) => <td key={i} className={[c.align === 'right' ? 'num' : '', c.className ?? ''].join(' ').trim() || undefined}>{c.cell(r)}</td>)}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

/** {@code truncated}: the text search matched more records than it can return, so the list asks the user to narrow it. */
export function Pager({ page, pages, total, size, onPage, truncated }: { page: number; pages: number; total: number; size: number; onPage: (p: number) => void; truncated?: boolean }) {
  if (total === 0) return null;
  const first = (page - 1) * size + 1, last = Math.min(total, page * size);
  const visible = Array.from({ length: pages }, (_, i) => i + 1).filter(p => p === 1 || p === pages || Math.abs(p - page) <= 1);
  return (
    <div className="pager">
      <span>Showing {first}–{last} of {num(total)}{truncated ? ' · more matches exist, refine your search' : ''}</span>
      <div>
        <button disabled={page <= 1} onClick={() => onPage(page - 1)} aria-label="Previous page"><ChevronLeft size={16} /></button>
        {visible.map((p, i) => (
          <span key={p}>{i > 0 && p - visible[i - 1]! > 1 && <em>…</em>}<button className={p === page ? 'active' : ''} aria-current={p === page} onClick={() => onPage(p)}>{p}</button></span>
        ))}
        <button disabled={page >= pages} onClick={() => onPage(page + 1)} aria-label="Next page"><ChevronRight size={16} /></button>
      </div>
    </div>
  );
}

/** Two-cell layout used in tables: leading visual + title + subtitle. */
export function TitleCell({ lead, title, sub }: { lead?: ReactNode; title: ReactNode; sub?: ReactNode }) {
  return <div className="who-cell">{lead}<div><b>{title}</b>{sub && <small>{sub}</small>}</div></div>;
}
