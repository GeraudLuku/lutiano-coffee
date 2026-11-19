import type { ReactNode } from 'react';

export function Panel({ title, icon, action, children, className = '' }: {
  title?: ReactNode; icon?: ReactNode; action?: ReactNode; children: ReactNode; className?: string;
}) {
  return (
    <section className={`panel ${className}`}>
      {title && <header className="panel-head"><h3>{icon}{title}</h3>{action}</header>}
      {children}
    </section>
  );
}

/** Label/value list used by every "Information" card. */
export function Facts({ items, withIcons }: { items: [ReactNode, ReactNode][]; withIcons?: boolean }) {
  return (
    <dl className={`facts ${withIcons ? 'icons' : ''}`}>
      {items.map(([k, v], i) => <div key={i} className="fact"><dt>{k}</dt><dd>{v}</dd></div>)}
    </dl>
  );
}
