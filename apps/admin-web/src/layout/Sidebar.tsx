import { Link, NavLink, useLocation } from 'react-router-dom';
import { ArrowRight, ChevronDown } from 'lucide-react';
import { businessApi } from '../api/services';
import { useAuth } from '../auth/AuthContext';
import { useLoad } from '../hooks/useLoad';
import { Logo } from '../components/ui';
import { NAVIGATION } from './navigation';

export function Sidebar() {
  const { can } = useAuth();
  const { pathname } = useLocation();
  const business = useLoad(() => businessApi.get(), []);
  return (
    <aside className="sidebar">
      <div className="brand"><Logo size={46} /><b>LUTIANO</b><small>Coffee Business OS</small>{can('ADMIN') && <span className="brand-chip">Admin Portal</span>}</div>
      <nav aria-label="Main">
        {NAVIGATION.filter(n => can(...n.roles)).map(n => {
          const active = n.to === '/' ? pathname === '/' : pathname.startsWith(n.to);
          return (
            <div key={n.to}>
              <NavLink to={n.to} end={n.to === '/'} className={active ? 'nav-item active' : 'nav-item'}>
                <n.icon size={19} />{n.label}{n.children && <ChevronDown size={15} className="chev" />}
              </NavLink>
              {n.children && active && (
                <div className="subnav">
                  {n.children.filter(c => !c.roles || can(...c.roles)).map(c => (
                    <NavLink key={c.to} to={c.to} end className={({ isActive }) => (isActive ? 'active' : '')}>{c.label}</NavLink>
                  ))}
                </div>
              )}
            </div>
          );
        })}
      </nav>
      {business.data?.description && (
        <Link to="/products" className="promo">
          <Logo size={26} />
          <b>{business.data.description}</b>
          <span>View Products <ArrowRight size={14} /></span>
        </Link>
      )}
    </aside>
  );
}
