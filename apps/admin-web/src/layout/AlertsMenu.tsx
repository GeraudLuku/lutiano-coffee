import { useCallback, useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Bell } from 'lucide-react';
import { dashboardApi, INBOX_CHANGED, notificationsApi } from '../api/services';
import { useOutsideClick } from '../hooks/useOutsideClick';
import { dateTime } from '../utils/format';
import { notificationLink } from '../utils/notifications';
import type { Attention, InboxMessage } from '../types/models';

const ITEMS: [keyof Attention, string, string][] = [
  ['pendingApprovals', 'Inventory waiting for approval or dispatch', '/inventory?tab=approvals'],
  ['inTransit', 'Stock transfers in transit', '/inventory?tab=transfers'],
  ['discrepancies', 'Receipt discrepancies (30 days)', '/inventory?tab=discrepancies'],
  ['cashVariances', 'Cash shift variances (30 days)', '/sales/shifts'],
  ['lowStockCentral', 'Low central stock', '/inventory'],
  ['lowStockOperations', 'Low stock at locations', '/inventory?tab=locations'],
];
const RECENT = 5;

/**
 * Bell menu: the person's latest messages (unread first marked, dismissed once opened) and the work that needs attention
 * (counters from the dashboard, which clear themselves when the work is done). Refreshed on navigation.
 */
export function AlertsMenu() {
  const nav = useNavigate();
  const { pathname } = useLocation();
  const [att, setAtt] = useState<Attention | null>(null);
  const [recent, setRecent] = useState<InboxMessage[]>([]);
  const [unread, setUnread] = useState(0);
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);
  useOutsideClick(ref, useCallback(() => setOpen(false), []));
  const load = useCallback(() => {
    dashboardApi.get({ days: 30 }).then(d => setAtt(d.attention)).catch(() => setAtt(null));
    notificationsApi.mine({ page: 1, size: RECENT }).then(p => { setRecent(p.items); setUnread(p.summary?.unread ?? 0); }).catch(() => { setRecent([]); setUnread(0); });
  }, []);
  useEffect(load, [load, pathname]);
  // Refresh when messages are read elsewhere (the Inbox page) and every minute for new ones.
  useEffect(() => {
    window.addEventListener(INBOX_CHANGED, load);
    const timer = setInterval(load, 60_000);
    return () => { window.removeEventListener(INBOX_CHANGED, load); clearInterval(timer); };
  }, [load]);
  const active = att ? ITEMS.filter(([k]) => att[k] > 0) : [];
  // The badge counts unread messages only. "Needs attention" items are work queues that clear when the work is done, so they stay in the menu without keeping the badge lit.
  const badge = unread;
  const go = (to: string) => { nav(to); setOpen(false); };
  const read = async (m: InboxMessage) => {
    if (!m.read_at) { await notificationsApi.markRead(m.id).catch(() => undefined); load(); }
    go(notificationLink(m.related_type, m.related_id) ?? '/notifications');
  };
  return (
    <div className="notif" ref={ref}>
      <button className="icon-btn" aria-label={`Notifications: ${badge} unread`} aria-expanded={open} onClick={() => setOpen(o => !o)}>
        <Bell size={21} />{badge > 0 && <span className="badge">{badge}</span>}
      </button>
      {open && (
        <div className="dropdown notif-list">
          <h4>Recent messages{unread > 0 ? ` · ${unread} unread` : ''}</h4>
          {recent.length === 0 ? <p className="muted">No messages yet.</p>
            : recent.map(m => (
              <button key={m.id} className={`notif-msg${m.read_at ? '' : ' unread'}`} onClick={() => read(m)}>
                <b>{m.event_name}</b>{m.body}<small>{dateTime(m.created_at)}</small>
              </button>))}
          {unread > 0 && <button className="notif-all" onClick={async () => { await notificationsApi.markRead().catch(() => undefined); load(); }}>Mark all as read</button>}
          <h4 className="sub">Needs attention</h4>
          {active.length === 0 ? <p className="muted">Nothing needs attention right now.</p>
            : active.map(([k, label, to]) => <button key={k} onClick={() => go(to)}><b>{att![k]}</b>{label}</button>)}
          <button className="notif-all" onClick={() => go('/notifications')}>View all notifications</button>
        </div>
      )}
    </div>
  );
}
