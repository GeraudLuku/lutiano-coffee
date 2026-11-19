import { Link } from 'react-router-dom';
import { Award, Coins, Scale, Wallet } from 'lucide-react';
import { earningsApi } from '../../api/services';
import { useLoad } from '../../hooks/useLoad';
import { DataTable, Empty, ErrorNote, Kpi, KpiRow, Loading, Panel } from '../../components/ui';
import { num, xaf } from '../../utils/format';
import { signedXaf } from '../../utils/earnings';
import { StatementsTable } from './StatementsTable';

/** The current month so far (live): what sellers have earned and where commission comes from. */
export function OverviewTab() {
  const { data, error, loading, reload } = useLoad(() => earningsApi.overview(), []);
  if (loading && !data) return <Loading />;
  if (!data) return <ErrorNote error={error} retry={reload} />;
  const t = data.period.totals;
  const month = new Date(`${data.period.starts_on.slice(0, 10)}T12:00:00Z`).toLocaleDateString('en-GB', { month: 'long', year: 'numeric' });
  return (
    <>
      <KpiRow>
        <Kpi tone="gold" icon={<Coins size={22} />} value={xaf(t.commission_xaf)} label={`Commissions · ${month}`} />
        <Kpi tone="purple" icon={<Award size={22} />} value={xaf(t.bonus_xaf)} label="Target bonuses (reached so far)" />
        <Kpi tone="blue" icon={<Scale size={22} />} value={signedXaf(t.adjustments_xaf)} label="Adjustments" />
        <Kpi tone="green" icon={<Wallet size={22} />} value={xaf(t.payable_xaf)} label={`Payable so far (${num(t.sellers)} sellers)`} to={`/commissions/periods/${data.period.id}`} />
      </KpiRow>
      <div className="grid-form wide-left">
        <Panel title="Top Earners This Month" action={<Link className="link" to={`/commissions/periods/${data.period.id}`}>All sellers</Link>}>
          {data.topEarners.length ? <StatementsTable rows={data.topEarners} /> : <Empty title="No earnings yet this month" />}
        </Panel>
        <Panel title="Commission by Product">
          {data.topProducts.length ? (
            <DataTable rows={data.topProducts} rowKey={p => p.product_name} columns={[
              { header: 'Product', cell: p => <b>{p.product_name}</b> },
              { header: 'Units', align: 'right', cell: p => num(p.units) },
              { header: 'Commission', align: 'right', cell: p => xaf(p.commission_xaf) },
            ]} />
          ) : <Empty title="No commission yet" />}
        </Panel>
      </div>
    </>
  );
}
