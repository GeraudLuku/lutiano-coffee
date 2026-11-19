import type { ReactNode } from 'react';
import { Inbox } from 'lucide-react';

export function Empty({ title, text, action }: { title: string; text?: string; action?: ReactNode }) {
  return <div className="empty"><Inbox size={30} /><b>{title}</b>{text && <span>{text}</span>}{action}</div>;
}
export function Loading() {
  return <div className="loading" role="status"><span /> Loading…</div>;
}
/**
 * The body of a paged list: a spinner on the first load, an empty state when nothing matched, otherwise the table or cards.
 * Pass what {@code usePagedQuery} returned; the children are only shown once there are rows.
 */
export function ListState({ loading, data, items, empty, children }: {
  loading: boolean; data: unknown; items: unknown[]; empty: { title: string; text?: string; action?: ReactNode }; children: ReactNode;
}) {
  if (loading && !data) return <Loading />;
  return items.length === 0 ? <Empty {...empty} /> : <>{children}</>;
}
export function ErrorNote({ error, retry }: { error?: string; retry?: () => void }) {
  if (!error) return null;
  return <div className="error-note" role="alert">{error}{retry && <button className="link" onClick={retry}>Try again</button>}</div>;
}
export function Note({ tone = 'info', icon, children }: { tone?: 'info' | 'ok' | 'warn'; icon?: ReactNode; children: ReactNode }) {
  return <p className={`note ${tone}`}>{icon}<span>{children}</span></p>;
}
