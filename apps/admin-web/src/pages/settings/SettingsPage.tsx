import { useSearchParams } from 'react-router-dom';
import { useAuth } from '../../auth/AuthContext';
import { PageHeader, Tabs } from '../../components/ui';
import { GeneralTab } from './GeneralTab';
import { RolesTab } from './RolesTab';
import { AuditTab } from './AuditTab';
import { AccountTab } from './AccountTab';
import { BusinessRulesTab, IntegrationsTab, NotificationsPointer, SecurityTab } from './RulesIntegrationsTabs';

type Tab = 'general' | 'rules' | 'integrations' | 'security' | 'notifications' | 'roles' | 'audit' | 'account';

export function SettingsPage() {
  const { can } = useAuth();
  const [params, setParams] = useSearchParams();
  const tab = (params.get('tab') as Tab) || 'general';
  const admin = can('ADMIN');
  const tabs: [Tab, string][] = [['general', 'General'], ...(admin ? [['rules', 'Business Rules'], ['integrations', 'Integrations'], ['security', 'Security'], ['notifications', 'Notifications']] as [Tab, string][] : []),
    ['roles', 'Roles & Permissions'], ['audit', 'Audit Logs'], ['account', 'My Account']];
  return (
    <>
      <PageHeader title="System Settings" subtitle="Business configuration, roles and permissions, audit history and your account." crumbs={[['Settings']]} />
      <Tabs value={tab} onChange={t => setParams({ tab: t })} tabs={tabs} />
      {tab === 'general' && <GeneralTab />}
      {tab === 'rules' && admin && <BusinessRulesTab />}
      {tab === 'integrations' && admin && <IntegrationsTab />}
      {tab === 'security' && admin && <SecurityTab />}
      {tab === 'notifications' && admin && <NotificationsPointer />}
      {tab === 'roles' && <RolesTab canSeeCounts={can('ADMIN')} />}
      {tab === 'audit' && <AuditTab />}
      {tab === 'account' && <AccountTab />}
    </>
  );
}
