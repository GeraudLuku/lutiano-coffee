import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { ChevronRight, Home } from 'lucide-react';

/** Breadcrumb entries: [label, link?]; the last entry is usually the current page without a link. */
export type Crumb = [string, string?];

export function PageHeader({ title, subtitle, crumbs, actions, badge }: {
  title?: ReactNode; subtitle?: ReactNode; crumbs?: Crumb[]; actions?: ReactNode; badge?: ReactNode;
}) {
  return (
    <div className="page-header">
      <div>
        {crumbs && (
          <nav className="crumbs" aria-label="Breadcrumb">
            <Link to="/" aria-label="Dashboard"><Home size={14} /></Link>
            {crumbs.map(([label, to], i) => (
              <span key={i}><ChevronRight size={13} />{to ? <Link to={to}>{label}</Link> : <b>{label}</b>}</span>
            ))}
          </nav>
        )}
        {title && <h1>{title}{badge}</h1>}
        {subtitle && <p>{subtitle}</p>}
      </div>
      {actions && <div className="header-actions">{actions}</div>}
    </div>
  );
}
