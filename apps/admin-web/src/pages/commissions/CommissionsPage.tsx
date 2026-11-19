import { useSearchParams } from 'react-router-dom';
import { PageHeader, Tabs } from '../../components/ui';
import { OverviewTab } from './OverviewTab';
import { RulesTab } from './RulesTab';
import { EarningsTab } from './EarningsTab';
import { AdjustmentsTab } from './AdjustmentsTab';
import { CampaignsTab } from './CampaignsTab';

const TABS = [['overview', 'Overview'], ['rules', 'Commission Rules'], ['earnings', 'Earnings'], ['campaigns', 'Campaigns'], ['adjustments', 'Adjustments']] as const;
type Tab = (typeof TABS)[number][0];

/** A11 Commissions & Targets. */
export function CommissionsPage() {
  const [params, setParams] = useSearchParams();
  const tab = (TABS.some(t => t[0] === params.get('tab')) ? params.get('tab') : 'overview') as Tab;
  return (
    <>
      <PageHeader title="Commissions & Targets" subtitle="Commission rates per product and seller role, seller targets and monthly earnings." crumbs={[['Commissions']]} />
      <Tabs value={tab} onChange={t => setParams({ tab: t }, { replace: true })} tabs={TABS.map(([k, l]) => [k, l] as [Tab, string])} />
      {tab === 'overview' && <OverviewTab />}
      {tab === 'rules' && <RulesTab />}
      {tab === 'earnings' && <EarningsTab />}
      {tab === 'campaigns' && <CampaignsTab />}
      {tab === 'adjustments' && <AdjustmentsTab />}
    </>
  );
}
