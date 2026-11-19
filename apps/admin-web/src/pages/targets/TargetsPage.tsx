import { useSearchParams } from 'react-router-dom';
import { PageHeader, Tabs } from '../../components/ui';
import { TargetsTab } from '../commissions/TargetsTab';
import { BonusTab } from './BonusTab';
import { TargetsOverview } from './TargetsOverview';

const TABS = [['overview', 'Overview'], ['targets', 'Sales Targets'], ['bonus', 'Bonus Configuration']] as const;
type Tab = (typeof TABS)[number][0];

/** Targets & Bonuses (design 11.1 to 11.3). Commission disputes (11.4) are not part of this release. */
export function TargetsPage() {
  const [params, setParams] = useSearchParams();
  const tab = (TABS.some(t => t[0] === params.get('tab')) ? params.get('tab') : 'overview') as Tab;
  return (
    <>
      <PageHeader title="Targets & Bonuses" subtitle="Track target performance and the bonuses sellers earn for reaching them." crumbs={[['Targets & Bonuses']]} />
      <Tabs value={tab} onChange={t => setParams({ tab: t }, { replace: true })} tabs={TABS.map(([k, l]) => [k, l] as [Tab, string])} />
      {tab === 'overview' && <TargetsOverview />}
      {tab === 'targets' && <TargetsTab />}
      {tab === 'bonus' && <BonusTab />}
    </>
  );
}
