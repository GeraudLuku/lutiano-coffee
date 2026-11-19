import { ArrowDownRight, ArrowUpRight, BadgePercent, BarChart3, PieChart, Receipt, Users, Wallet } from 'lucide-react';
import { reportsApi } from '../../api/services';
import { useLoad } from '../../hooks/useLoad';
import { ErrorNote, Loading, Panel, StatCard, StatRow } from '../../components/ui';
import { SalesChart } from '../../components/charts/SalesChart';
import { Donut } from '../../components/charts/Donut';
import { deltaPct, num, xaf } from '../../utils/format';
import { CHANNEL_COLOR, TYPE_LABEL } from '../../utils/labels';
import type { OperationType } from '../../types/models';
import type { ReportRange } from './ReportPeriod';

const CHANNELS: OperationType[] = ['FIXED_POS', 'MOBILE', 'MARKET_AGENT'];

const Growth = ({ now, before }: { now: number; before: number }) => {
  const d = deltaPct(now, before);
  return d === null ? <span className="muted">no previous data</span> : <span className={d >= 0 ? 'up' : 'down'}>{d >= 0 ? <ArrowUpRight size={14} /> : <ArrowDownRight size={14} />}{Math.abs(d).toFixed(1)}% vs previous period</span>;
};

/** Reports overview (design 13.1): headline figures, sales performance, channel mix and key insights. */
export function OverviewReport({ range }: { range: ReportRange }) {
  const { data, error, loading, reload } = useLoad(() => reportsApi.overview(range), [range.from, range.to]);
  if (loading && !data) return <Loading />;
  if (!data) return <ErrorNote error={error} retry={reload} />;
  const c = data.current, p = data.previous;
  return (
    <>
      <StatRow>
        <StatCard tone="green" icon={<Wallet size={20} />} value={xaf(c.revenue_xaf)} label="Total Sales" />
        <StatCard tone="purple" icon={<Receipt size={20} />} value={num(c.transactions)} label="Total Transactions" />
        <StatCard tone="amber" icon={<Users size={20} />} value={num(c.sellers)} label="Active Sellers" />
        <StatCard tone="blue" icon={<BadgePercent size={20} />} value={`${data.avg_commission_pct}%`} label="Avg. Commission" />
      </StatRow>
      <div className="grid-2">
        <Panel title="Sales Performance" icon={<BarChart3 size={19} />}>
          {c.transactions === 0 ? <p className="muted pad">No sales in this period.</p> : <SalesChart data={data.byDay.map(d => ({ date: d.date, sales: d.transactions, revenueXaf: d.revenue_xaf }))} />}
        </Panel>
        <Panel title="Sales by Channel" icon={<PieChart size={19} />}>
          <Donut centre={num(c.transactions)} label="Sales" segments={CHANNELS.map(t => ({ label: TYPE_LABEL[t], color: CHANNEL_COLOR[t], value: Number(data.byChannel.find(x => x.label === t)?.transactions ?? 0) }))} />
        </Panel>
      </div>
      <Panel title="Key Insights">
        <ul className="insights">
          <li><b>{deltaPct(c.revenue_xaf, p.revenue_xaf)?.toFixed(1) ?? '—'}%</b><span>Sales growth</span><Growth now={c.revenue_xaf} before={p.revenue_xaf} /></li>
          <li><b>{deltaPct(c.transactions, p.transactions)?.toFixed(1) ?? '—'}%</b><span>Transactions growth</span><Growth now={c.transactions} before={p.transactions} /></li>
          <li><b>{data.topProduct?.label ?? '—'}</b><span>Top product</span>{data.topProduct && <small>{num(data.topProduct.units)} units sold</small>}</li>
          <li><b>{xaf(data.refunded_xaf)}</b><span>Refunded</span><small>{c.revenue_xaf ? ((data.refunded_xaf / c.revenue_xaf) * 100).toFixed(1) : '0.0'}% of sales</small></li>
        </ul>
      </Panel>
    </>
  );
}
