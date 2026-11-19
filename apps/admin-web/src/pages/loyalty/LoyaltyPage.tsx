import { useSearchParams } from 'react-router-dom';
import { PageHeader, Tabs } from '../../components/ui';
import { OverviewTab } from './OverviewTab';
import { MembersTab } from './MembersTab';
import { RulesTab } from './RulesTab';
import { RewardsTab } from './RewardsTab';
import { RedemptionsTab } from './RedemptionsTab';
import { TiersTab } from './TiersTab';
import { AdjustmentsTab } from './AdjustmentsTab';
import { ActivityTab } from './ActivityTab';
import { SettingsTab } from './SettingsTab';

const TABS = [
  ['overview', 'Overview'], ['members', 'Members'], ['rules', 'Earning Rules'], ['rewards', 'Rewards'], ['redemptions', 'Redemptions'],
  ['tiers', 'Tiers'], ['adjustments', 'Adjustments'], ['activity', 'Points Ledger'], ['settings', 'Configurations'],
] as const;
type Tab = (typeof TABS)[number][0];

/** A10 Loyalty Program (design boards 8.1–8.4). */
export function LoyaltyPage() {
  const [params, setParams] = useSearchParams();
  const tab = (TABS.some(t => t[0] === params.get('tab')) ? params.get('tab') : 'overview') as Tab;
  const setTab = (t: Tab) => setParams({ tab: t }, { replace: true });
  return (
    <>
      <PageHeader title="Loyalty Program" subtitle="Manage the loyalty programme, earning rules, rewards and member activity." crumbs={[['Loyalty']]} />
      <Tabs value={tab} onChange={setTab} tabs={TABS.map(([k, l]) => [k, l] as [Tab, string])} />
      {tab === 'overview' && <OverviewTab />}
      {tab === 'members' && <MembersTab />}
      {tab === 'rules' && <RulesTab onEditDefault={() => setTab('settings')} />}
      {tab === 'rewards' && <RewardsTab />}
      {tab === 'redemptions' && <RedemptionsTab />}
      {tab === 'tiers' && <TiersTab />}
      {tab === 'adjustments' && <AdjustmentsTab />}
      {tab === 'activity' && <ActivityTab />}
      {tab === 'settings' && <SettingsTab />}
    </>
  );
}
