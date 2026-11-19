import { Link } from 'react-router-dom';
import { Award, Gauge, PieChart, Target, Users } from 'lucide-react';
import { ErrorNote, Loading, Panel, StatCard, StatRow, DataTable } from '../../components/ui';
import { Donut } from '../../components/charts/Donut';
import { num, xaf } from '../../utils/format';
import { targetAmount } from '../../utils/earnings';
import type { SellerTarget } from '../../types/models';
import { useAllTargets } from './useAllTargets';

/** In progress, achieved, or ended without reaching the goal. */
const statusOf = (t: SellerTarget) => (t.reached ? 'Achieved' : t.state === 'ENDED' ? 'Not achieved' : t.state === 'UPCOMING' ? 'Upcoming' : 'In progress');
const COLORS: Record<string, string> = { Achieved: '#12805c', 'In progress': '#1f4fd1', Upcoming: '#748197', 'Not achieved': '#c0392b' };

/** Target overview (design 11.1): headline figures, targets by status and the best performers. */
export function TargetsOverview() {
  const { data, error, loading, reload } = useAllTargets();
  if (loading && !data) return <Loading />;
  if (!data) return <ErrorNote error={error} retry={reload} />;
  const avg = data.length ? data.reduce((n, t) => n + Math.min(t.progress_pct, 200), 0) / data.length : 0;
  const bonuses = data.reduce((n, t) => n + t.bonus_earned, 0);
  const sellers = new Set(data.map(t => t.seller_id)).size;
  const counts = Object.keys(COLORS).map(k => ({ label: k, color: COLORS[k]!, value: data.filter(t => statusOf(t) === k).length }));
  const top = [...data].sort((a, b) => b.progress_pct - a.progress_pct).slice(0, 5);
  return (
    <>
      <StatRow>
        <StatCard tone="green" icon={<Target size={20} />} value={num(data.length)} label="Active Targets" />
        <StatCard tone="purple" icon={<Gauge size={20} />} value={`${avg.toFixed(1)}%`} label="Avg. Achievement" />
        <StatCard tone="amber" icon={<Award size={20} />} value={xaf(bonuses)} label="Bonuses Earned" />
        <StatCard tone="blue" icon={<Users size={20} />} value={num(sellers)} label="Sellers with Targets" />
      </StatRow>
      <div className="grid-2">
        <Panel title="Targets by Status" icon={<PieChart size={19} />}>
          {data.length === 0 ? <p className="muted pad">No targets yet. Create one under Sales Targets.</p> : <Donut centre={num(data.length)} label="Targets" segments={counts} />}
        </Panel>
        <Panel title="Top Performers (vs target)" action={<Link className="link" to="/targets?tab=targets">View All</Link>}>
          <DataTable rows={top} rowKey={t => t.id} columns={[
            { header: 'Seller', cell: t => <Link className="link" to={`/users/${t.seller_id}?tab=earnings`}>{t.seller_name}</Link> },
            { header: 'Target', cell: t => t.name },
            { header: 'Goal', align: 'right', cell: t => targetAmount(t, t.target_value) },
            { header: 'Achieved', align: 'right', cell: t => targetAmount(t, t.achieved) },
            { header: '%', align: 'right', cell: t => <b>{t.progress_pct.toFixed(1)}%</b> },
            { header: 'Bonus', align: 'right', cell: t => xaf(t.bonus_earned) },
          ]} />
        </Panel>
      </div>
    </>
  );
}
