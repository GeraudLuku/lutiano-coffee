import { useEffect, useRef, useState } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import { Menu, X } from 'lucide-react';
import { useSearchHighlight } from '../hooks/useSearchHighlight';
import { Sidebar } from './Sidebar';
import { GlobalSearch } from './GlobalSearch';
import { AlertsMenu } from './AlertsMenu';
import { UserMenu } from './UserMenu';

export function AppLayout() {
  const [navOpen, setNavOpen] = useState(false);
  const { pathname } = useLocation();
  const content = useRef<HTMLElement>(null);
  const highlight = useSearchHighlight(content);
  useEffect(() => { setNavOpen(false); window.scrollTo(0, 0); }, [pathname]);
  return (
    <div className={`shell ${navOpen ? 'nav-open' : ''}`}>
      <Sidebar />
      <div className="scrim" onClick={() => setNavOpen(false)} />
      <div className="main">
        <header className="topbar">
          <button className="icon-btn menu-btn" onClick={() => setNavOpen(true)} aria-label="Open navigation"><Menu size={20} /></button>
          <GlobalSearch />
          <AlertsMenu />
          <UserMenu />
        </header>
        <main className="content" ref={content}><Outlet /></main>
        {highlight.term && (
          <button className="highlight-pill" onClick={highlight.dismiss} aria-label="Clear search highlights">
            Highlighting “{highlight.term}” <X size={14} />
          </button>
        )}
      </div>
    </div>
  );
}
