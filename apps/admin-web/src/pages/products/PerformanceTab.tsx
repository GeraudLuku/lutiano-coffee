import { useState } from 'react';
import { Link } from 'react-router-dom';
import { BarChart3, MapPin, Package, Receipt, Wallet } from 'lucide-react';
import { productsApi } from '../../api/services';
import { useLoad } from '../../hooks/useLoad';
import { DataTable, ErrorNote, Loading, Panel, StatCard, StatRow } from '../../components/ui';
import { SalesChart } from '../../components/charts/SalesChart';
import { num, xaf } from '../../utils/format';

const PERIODS: [number, string][] = [[7, 'Last 7 days'], [30, 'Last 30 days'], [90, 'Last 90 days']];

/** Units and revenue of a product over a period: by day, by location and (for a parent) by variant. */
export function PerformanceTab({ productId, parent }: { productId: string; parent: boolean }) {
  const [days, setDays] = useState(30);
  const { data, error, loading, reload } = useLoad(() => productsApi.performance(productId, days), [productId, days]);
  return (
    <>
      <div className="toolbar-end">
        <select className="input" aria-label="Period" value={days} onChange={e => setDays(Number(e.target.value))}>{PERIODS.map(([d, l]) => <option key={d} value={d}>{l}</option>)}</select>
      </div>
      <ErrorNote error={error} retry={reload} />
      {loading && !data ? <Loading /> : data && (
        <>
          <StatRow>
            <StatCard tone="purple" icon={<Package size={20} />} value={num(data.totals.units)} label="Units Sold" />
            <StatCard tone="amber" icon={<Wallet size={20} />} value={xaf(data.totals.revenue_xaf)} label="Revenue (incl. add-ons)" />
            <StatCard tone="blue" icon={<Receipt size={20} />} value={num(data.totals.transactions)} label="Transactions" />
          </StatRow>
          <Panel title="Sales Trend" icon={<BarChart3 size={19} />}>
            {data.totals.units === 0 ? <p className="muted pad">No sales in this period.</p> : <SalesChart data={data.byDay.map(d => ({ date: d.date, sales: d.units, revenueXaf: d.revenueXaf }))} />}
          </Panel>
          <div className="grid-2">
            <Panel title="Top Locations" icon={<MapPin size={19} />}>
              <DataTable rows={data.byLocation} rowKey={r => r.operation_id} columns={[
                { header: 'Location', cell: r => <Link className="link" to={`/locations/${r.operation_id}`}>{r.operation_name}</Link> },
                { header: 'Units', align: 'right', cell: r => num(r.units) }, { header: 'Revenue', align: 'right', cell: r => xaf(r.revenue_xaf) },
              ]} />
            </Panel>
            {parent && (
              <Panel title="By Variant" icon={<Package size={19} />}>
                <DataTable rows={data.byVariant} rowKey={r => r.product_id} columns={[
                  { header: 'Variant', cell: r => <Link className="link" to={`/products/${r.product_id}`}>{r.name}</Link> },
                  { header: 'Units', align: 'right', cell: r => num(r.units) }, { header: 'Revenue', align: 'right', cell: r => xaf(r.revenue_xaf) },
                ]} />
              </Panel>
            )}
          </div>
        </>
      )}
    </>
  );
}
