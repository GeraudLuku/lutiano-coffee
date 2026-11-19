import { useSearchParams } from 'react-router-dom';
import { CheckCircle2, Clock, HandCoins, Hourglass, Smartphone } from 'lucide-react';
import { payoutsApi } from '../../api/services';
import { useLoad } from '../../hooks/useLoad';
import { ErrorNote, PageHeader, StatCard, StatRow, Tabs } from '../../components/ui';
import { num, xaf } from '../../utils/format';
import { BalancesTab } from './BalancesTab';
import { DestinationsTab } from './DestinationsTab';
import { PayoutsList } from './PayoutsList';
import { SettingsTab } from './SettingsTab';

const TABS = [['balances', 'Balances'], ['payouts', 'Payouts'], ['numbers', 'Payout Numbers'], ['settings', 'Approval Rule']] as const;
type Tab = (typeof TABS)[number][0];

/** A12 Payouts: what sellers are owed, payouts recorded outside LUTIANO, their numbers and the approval rule. */
export function PayoutsPage() {
  const [params, setParams] = useSearchParams();
  const tab = (TABS.some(t => t[0] === params.get('tab')) ? params.get('tab') : 'balances') as Tab;
  const overview = useLoad(() => payoutsApi.overview(), []);
  const o = overview.data;
  return (
    <>
      <PageHeader title="Payouts" subtitle="Record, approve and evidence what sellers are paid. Payments are made outside LUTIANO." crumbs={[['Payouts']]} />
      <ErrorNote error={overview.error} retry={overview.reload} />
      <StatRow five>
        <StatCard tone="amber" icon={<HandCoins size={20} />} value={xaf(o?.outstanding_xaf ?? 0)} label={`Outstanding · ${num(o?.sellers_with_balance ?? 0)} sellers`} />
        <StatCard tone="purple" icon={<Clock size={20} />} value={num((o?.approval_count ?? 0) + (o?.reversal_count ?? 0))} label={`To approve · ${num(o?.approval_count ?? 0)} payouts (${xaf(o?.approval_xaf ?? 0)}), ${num(o?.reversal_count ?? 0)} reversals`} />
        <StatCard tone="blue" icon={<Hourglass size={20} />} value={num(o?.to_pay_count ?? 0)} label={`Approved, to pay · ${xaf(o?.to_pay_xaf ?? 0)}`} />
        <StatCard tone="green" icon={<CheckCircle2 size={20} />} value={xaf(o?.paid_month_xaf ?? 0)} label="Paid this month" />
        <StatCard tone="blue" icon={<Smartphone size={20} />} value={num(o?.destinations_pending ?? 0)} label="Numbers to verify" />
      </StatRow>
      <Tabs value={tab} onChange={t => setParams({ tab: t }, { replace: true })} tabs={TABS.map(([k, l]) => [k, l] as [Tab, string])} />
      {tab === 'balances' && <BalancesTab onChanged={overview.reload} />}
      {tab === 'payouts' && <PayoutsList />}
      {tab === 'numbers' && <DestinationsTab onChanged={overview.reload} />}
      {tab === 'settings' && <SettingsTab onChanged={overview.reload} />}
    </>
  );
}
