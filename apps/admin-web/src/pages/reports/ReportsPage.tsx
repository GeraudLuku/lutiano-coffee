import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { PageHeader, Tabs } from '../../components/ui';
import { OverviewTab as LoyaltyOverview } from '../loyalty/OverviewTab';
import { OverviewReport } from './OverviewReport';
import { ReportBuilder } from './ReportBuilder';
import { defaultRange, ReportPeriod, type ReportRange } from './ReportPeriod';
import { SalesReport } from './SalesReport';

const TABS = [['overview', 'Overview'], ['sales', 'Sales Reports'], ['loyalty', 'Loyalty & Customers'], ['builder', 'Report Builder']] as const;
type Tab = (typeof TABS)[number][0];

/** Reports & Analytics (design boards 13.1 to 13.4). */
export function ReportsPage() {
  const [params, setParams] = useSearchParams();
  const tab = (TABS.some(t => t[0] === params.get('tab')) ? params.get('tab') : 'overview') as Tab;
  const [range, setRange] = useState<ReportRange>(defaultRange);
  return (
    <>
      <PageHeader title="Reports & Analytics" subtitle="Business performance: sales, sellers, loyalty and custom reports." crumbs={[['Reports']]}
        actions={tab !== 'loyalty' && <ReportPeriod range={range} onChange={setRange} />} />
      <Tabs value={tab} onChange={t => setParams({ tab: t }, { replace: true })} tabs={TABS.map(([k, l]) => [k, l] as [Tab, string])} />
      {tab === 'overview' && <OverviewReport range={range} />}
      {tab === 'sales' && <SalesReport range={range} />}
      {tab === 'loyalty' && <LoyaltyOverview />}
      {tab === 'builder' && <ReportBuilder range={range} />}
    </>
  );
}
