import { useState } from 'react';
import { Download, Receipt, TrendingUp, Wallet, BarChart3 } from 'lucide-react';
import { operationsApi, reportsApi, staffApi } from '../../api/services';
import { useLoad } from '../../hooks/useLoad';
import { useAction } from '../../hooks/useAction';
import { DataTable, ErrorNote, FilterBar, Loading, Panel, SelectFilter, StatCard, StatRow, Tabs } from '../../components/ui';
import { SalesChart } from '../../components/charts/SalesChart';
import { deltaPct, num, xaf } from '../../utils/format';
import { saveFile, toCsv } from '../../utils/files';
import { TYPE_LABEL } from '../../utils/labels';
import type { ReportRange } from './ReportPeriod';

const GROUPS = [['operation', 'By Operation'], ['product', 'By Product'], ['seller', 'By Seller'], ['channel', 'By Channel'], ['day', 'Daily']] as const;
type Group = (typeof GROUPS)[number][0];

/** Sales reports (design 13.2): sales grouped by operation, product, seller, channel or day, with share and growth. */
export function SalesReport({ range }: { range: ReportRange }) {
  const [group, setGroup] = useState<Group>('operation');
  const [operationId, setOperationId] = useState('');
  const [channel, setChannel] = useState('');
  const [sellerId, setSellerId] = useState('');
  const ops = useLoad(() => operationsApi.all(), []);
  const sellers = useLoad(async () => (await staffApi.all()).filter(s => s.role === 'SELLER'), []);
  const { data, error, loading, reload } = useLoad(() => reportsApi.sales({ ...range, groupBy: group, operationId, channel, sellerId }), [range.from, range.to, group, operationId, channel, sellerId]);
  const exporting = useAction();
  const label = (l: string) => (group === 'channel' ? TYPE_LABEL[l as keyof typeof TYPE_LABEL] ?? l : l);
  return (
    <>
      <Tabs value={group} onChange={setGroup} tabs={GROUPS.map(([k, l]) => [k, l] as [Group, string])} />
      <FilterBar>
        <SelectFilter label="Operation" value={operationId} onChange={setOperationId} allLabel="All Operations" options={(ops.data ?? []).map(o => [o.id, o.name] as [string, string])} />
        <SelectFilter label="Channel" value={channel} onChange={setChannel} allLabel="All Channels" options={Object.entries(TYPE_LABEL)} />
        <SelectFilter label="Seller" value={sellerId} onChange={setSellerId} allLabel="All Sellers" options={(sellers.data ?? []).map(s => [s.id, s.full_name] as [string, string])} />
        <button className="btn" disabled={!data?.rows.length || exporting.busy} onClick={() => exporting.run(async () => saveFile(toCsv(data!.rows.map(r => ({ ...r, label: label(r.label) })),
          [['label', GROUPS.find(g => g[0] === group)![1].replace('By ', '')], ['units', 'Units'], ['transactions', 'Transactions'], ['revenue_xaf', 'Sales (XAF)'], ['share_pct', 'Share %']]), `lutiano-sales-by-${group}.csv`))}><Download size={16} />Export Report</button>
      </FilterBar>
      <ErrorNote error={error || exporting.error} retry={reload} />
      {loading && !data ? <Loading /> : data && (
        <>
          <StatRow>
            <StatCard tone="green" icon={<Wallet size={20} />} value={xaf(data.totals.revenue_xaf)} label="Total Sales" />
            <StatCard tone="purple" icon={<Receipt size={20} />} value={num(data.totals.transactions)} label="Transactions" />
            <StatCard tone="amber" icon={<Wallet size={20} />} value={xaf(data.avg_order_xaf)} label="Avg. Order Value" />
            <StatCard tone="blue" icon={<TrendingUp size={20} />} value={`${deltaPct(data.totals.revenue_xaf, data.previous.revenue_xaf)?.toFixed(1) ?? '—'}%`} label="Sales Growth" />
          </StatRow>
          <div className="grid-2">
            <Panel title={`Sales ${GROUPS.find(g => g[0] === group)![1].toLowerCase()}`}>
              <DataTable rows={data.rows} rowKey={r => r.label} columns={[
                { header: GROUPS.find(g => g[0] === group)![1].replace('By ', ''), cell: r => label(r.label) },
                { header: 'Units', align: 'right', cell: r => num(r.units) },
                { header: 'Transactions', align: 'right', cell: r => num(r.transactions) },
                { header: 'Sales (XAF)', align: 'right', cell: r => <b>{xaf(r.revenue_xaf)}</b> },
                { header: 'Share', align: 'right', cell: r => `${r.share_pct}%` },
              ]} />
            </Panel>
            <Panel title="Sales Trend (Daily)" icon={<BarChart3 size={19} />}>
              {data.totals.transactions === 0 ? <p className="muted pad">No sales for these filters.</p> : <SalesChart data={data.byDay.map(d => ({ date: d.date, sales: d.transactions, revenueXaf: d.revenue_xaf }))} />}
            </Panel>
          </div>
        </>
      )}
    </>
  );
}
