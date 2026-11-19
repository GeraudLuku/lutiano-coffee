import { useSearchParams } from 'react-router-dom';
import { PageHeader, Tabs } from '../../components/ui';
import { useAuth } from '../../auth/AuthContext';
import { AnnouncementsTab } from './AnnouncementsTab';
import { DeliveryLogTab } from './DeliveryLogTab';
import { InboxTab } from './InboxTab';
import { SupportTab } from './SupportTab';
import { SettingsTab } from './SettingsTab';
import { TemplatesTab } from './TemplatesTab';

const TABS = [['inbox', 'Inbox'], ['log', 'Delivery Log'], ['templates', 'Messages'], ['announcements', 'Announcements'], ['support', 'Support'], ['settings', 'Settings']] as const;
type Tab = (typeof TABS)[number][0];

/** A14 Notifications: what was sent, the texts, and who receives approval alerts. Opened from the bell in the header. */
export function NotificationsPage() {
  const [params, setParams] = useSearchParams();
  const { can } = useAuth();
  const tabs = TABS.filter(t => t[0] === 'inbox' || can('ADMIN'));
  const tab = (tabs.some(t => t[0] === params.get('tab')) ? params.get('tab') : 'inbox') as Tab;
  return (
    <>
      <PageHeader title="Notifications" subtitle="Your messages, and for administrators the SMS delivery log, message texts, announcements to sellers and approval alerts." crumbs={[['Notifications']]} />
      <Tabs value={tab} onChange={t => setParams({ tab: t }, { replace: true })} tabs={tabs.map(([k, l]) => [k, l] as [Tab, string])} />
      {tab === 'inbox' && <InboxTab />}
      {tab === 'log' && <DeliveryLogTab />}
      {tab === 'templates' && <TemplatesTab />}
      {tab === 'announcements' && <AnnouncementsTab />}
      {tab === 'support' && <SupportTab />}
      {tab === 'settings' && <SettingsTab />}
    </>
  );
}
