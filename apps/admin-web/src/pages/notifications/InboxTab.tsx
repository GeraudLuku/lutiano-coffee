import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Bell, Boxes, CheckCheck, CreditCard, Percent, ShoppingCart, ShieldCheck, type LucideIcon } from 'lucide-react';
import { notificationsApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { usePagedQuery } from '../../hooks/usePagedQuery';
import { ErrorNote, Pager, Panel, ListState } from '../../components/ui';
import { dateTime, num } from '../../utils/format';
import { notificationLink } from '../../utils/notifications';

const TOPICS: [string, string, LucideIcon][] = [['', 'All Notifications', Bell], ['SALES', 'Sales', ShoppingCart], ['PAYMENTS', 'Payments', CreditCard],
  ['INVENTORY', 'Inventory', Boxes], ['COMMISSIONS', 'Commissions', Percent], ['SECURITY', 'Security', ShieldCheck]];
const ICON = Object.fromEntries(TOPICS.map(([k, , i]) => [k, i])) as Record<string, LucideIcon>;

/** The signed-in person's own messages, grouped by topic. Opening one marks it read; "Mark all as read" clears the badge. */
export function InboxTab() {
  const nav = useNavigate();
  const [topic, setTopic] = useState('');
  const { data, items, summary, pager, error, loading, reload } = usePagedQuery((page, size) => notificationsApi.mine({ topic, page, size }), [topic]);
  const action = useAction();
  const unread = summary?.unread ?? 0;
  const counts = summary?.topics ?? {};
  const total = Object.values(counts).reduce((n, c) => n + c, 0);
  const open = async (m: (typeof items)[number]) => {
    if (!m.read_at) await action.run(() => notificationsApi.markRead(m.id));
    const to = notificationLink(m.related_type, m.related_id);
    if (to) nav(to); else reload();
  };
  return (
    <div className="inbox-layout">
      <Panel title="Categories">
        <ul className="topic-list">
          {TOPICS.filter(([k]) => k === '' || counts[k]).map(([k, label, Icon]) => (
            <li key={k}><button className={topic === k ? 'active' : ''} onClick={() => setTopic(k)}><Icon size={17} /><span>{label}</span><em>{num(k === '' ? total : counts[k] ?? 0)}</em></button></li>
          ))}
        </ul>
      </Panel>
      <Panel title={`Inbox${unread ? ` · ${unread} unread` : ''}`}
        action={<button className="btn sm" disabled={!unread || action.busy} onClick={() => action.run(() => notificationsApi.markRead(), 'All messages marked as read.').then(r => r && reload())}><CheckCheck size={15} />Mark all as read</button>}>
        <ErrorNote error={error || action.error} retry={reload} />
        <ListState loading={loading} data={data} items={items} empty={{ title: 'No messages', text: 'Messages about your work, such as approvals and stock alerts, appear here.' }}>
          <ul className="inbox">{items.map(m => {
            const Icon = ICON[m.topic] ?? Bell;
            return (
              <li key={m.id} className={m.read_at ? '' : 'unread'}>
                <button onClick={() => open(m)}>
                  <span className={`inbox-icon ${m.topic.toLowerCase()}`}><Icon size={17} /></span>
                  <span className="inbox-text"><b>{m.event_name}</b><span>{m.body}</span></span>
                  <small>{dateTime(m.created_at)}</small>
                </button>
              </li>
            );
          })}</ul>
        </ListState>
        <Pager {...pager} />
      </Panel>
    </div>
  );
}
