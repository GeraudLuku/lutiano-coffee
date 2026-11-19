import { useCallback, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ChevronDown, LogOut, UserCog } from 'lucide-react';
import { useAuth } from '../auth/AuthContext';
import { useOutsideClick } from '../hooks/useOutsideClick';
import { Avatar } from '../components/ui';
import { ROLE_LABEL } from '../utils/labels';

export function UserMenu() {
  const { session, signOut } = useAuth();
  const nav = useNavigate();
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);
  useOutsideClick(ref, useCallback(() => setOpen(false), []));
  const user = session!.user;
  return (
    <div className="user-menu" ref={ref}>
      <button onClick={() => setOpen(o => !o)} aria-haspopup="menu" aria-expanded={open}>
        <Avatar name={user.full_name} url={user.image_url} size={40} />
        <span className="who"><b>{user.full_name}</b><small>{ROLE_LABEL[user.role]}</small></span>
        <ChevronDown size={16} />
      </button>
      {open && (
        <div className="dropdown" role="menu">
          <button role="menuitem" onClick={() => { nav('/settings?tab=account'); setOpen(false); }}><UserCog size={16} />My account</button>
          <button role="menuitem" onClick={signOut}><LogOut size={16} />Sign out</button>
        </div>
      )}
    </div>
  );
}
