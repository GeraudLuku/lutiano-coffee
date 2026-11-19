import { Link } from 'react-router-dom';
import { Gift, Layers, Star, Users } from 'lucide-react';
import { loyaltyApi } from '../../api/services';
import { useLoad } from '../../hooks/useLoad';
import { usePagedQuery } from '../../hooks/usePagedQuery';
import { Empty, ErrorNote, Kpi, KpiRow, Loading, Pager, Panel } from '../../components/ui';
import { Donut } from '../../components/charts/Donut';
import { LinesChart } from '../../components/charts/LinesChart';
import { doualaDate, num, xaf } from '../../utils/format';
import { REWARD_TYPE, pts } from '../../utils/loyalty';
import { LedgerTable } from './LedgerTable';

const COLORS = ['#F5B800', '#1F4FD1', '#6B2FA3', '#12805C'];

/** Every day between from and to, with zeros where nothing happened. */
function fillDays(from: string, to: string, rows: { day: string; earned: number; redeemed: number }[]) {
  const byDay = new Map(rows.map(r => [String(r.day).slice(0, 10), r]));
  const out: { date: string; earned: number; redeemed: number }[] = [];
  for (let d = new Date(`${from}T12:00:00Z`); d <= new Date(`${to}T12:00:00Z`); d.setUTCDate(d.getUTCDate() + 1)) {
    const k = d.toISOString().slice(0, 10), r = byDay.get(k);
    out.push({ date: k, earned: Number(r?.earned ?? 0), redeemed: Number(r?.redeemed ?? 0) });
  }
  return out;
}

/** Design 8.1 Loyalty Overview (last 30 days). */
export function OverviewTab() {
  const { data: o, error, loading, reload } = useLoad(() => loyaltyApi.overview(doualaDate(-29), doualaDate()), []);
  const recent = usePagedQuery((page, size) => loyaltyApi.ledger({ page, size }), []);
  if (loading && !o) return <Loading />;
  if (!o) return <ErrorNote error={error} retry={reload} />;
  const liability = Number(o.points_balance) * Number(o.point_value_xaf);
  return (
    <>
      <KpiRow>
        <Kpi tone="purple" icon={<Users size={22} />} value={num(o.members)} label={`Loyalty Members (${num(o.active_members)} active in 30 days)`} to="/loyalty?tab=members" />
        <Kpi tone="gold" icon={<Star size={22} />} value={pts(o.points_earned)} label="Points Earned (30 days)" />
        <Kpi tone="green" icon={<Gift size={22} />} value={pts(o.points_redeemed)} label={`Points Redeemed (${num(o.redemptions)} rewards)`} to="/loyalty?tab=redemptions" />
        <Kpi tone="blue" icon={<Layers size={22} />} value={pts(o.points_balance)} label={`Current Liability (≈ ${xaf(liability)})`} />
      </KpiRow>
      <div className="grid-dash loyalty">
        <Panel title="Points Activity (Last 30 Days)">
          <LinesChart data={fillDays(o.from, o.to, o.daily)} unit="pts" label="Points earned and redeemed by day"
            series={[{ key: 'earned', label: 'Points Earned', color: '#F5B800' }, { key: 'redeemed', label: 'Points Redeemed', color: '#6B2FA3' }]} />
        </Panel>
        <Panel title="Members by Tier">
          {o.membersByTier.every(t => !Number(t.members)) ? <Empty title="No members yet" /> : (
            <Donut centre={num(o.members)} label="Members"
              segments={o.membersByTier.map(t => ({ label: `${t.name} (${num(t.members)})`, value: Number(t.members), color: t.color, to: `/loyalty?tab=members&tier=${t.code}` }))} />
          )}
        </Panel>
        <Panel title="Redemptions by Reward Type">
          {o.redemptionsByType.length === 0 ? <Empty title="No redemptions in this period" /> : (
            <Donut centre={pts(o.redemptionsByType.reduce((s, r) => s + Number(r.points), 0))} label="Redeemed"
              segments={o.redemptionsByType.map((r, i) => ({ label: `${REWARD_TYPE[r.reward_type][0]} (${num(r.redemptions)})`, value: Number(r.points), color: COLORS[i % COLORS.length] }))} />
          )}
        </Panel>
      </div>
      <Panel title="Recent Loyalty Activity" action={<Link className="link" to="/loyalty?tab=activity">View all</Link>}>
        <ErrorNote error={recent.error} retry={recent.reload} />
        {recent.items.length ? <LedgerTable rows={recent.items} withCustomer /> : !recent.loading && <Empty title="No loyalty activity yet" />}
        <Pager {...recent.pager} />
      </Panel>
    </>
  );
}
