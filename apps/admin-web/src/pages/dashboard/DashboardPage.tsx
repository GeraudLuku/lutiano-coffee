import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { Activity, ArrowRight, BarChart3, Boxes, Clock, HandCoins, Handshake, Package, PieChart, ShoppingCart, Star, Store, TriangleAlert, Users, Wallet } from 'lucide-react';
import { dashboardApi, type DashboardRange } from '../../api/services';
import { useAuth } from '../../auth/AuthContext';
import { useLoad } from '../../hooks/useLoad';
import { ErrorNote, Kpi, KpiRow, Loading, Panel, StatCard, StatRow } from '../../components/ui';
import { SalesChart } from '../../components/charts/SalesChart';
import { Donut } from '../../components/charts/Donut';
import { PagedActivity } from '../../components/domain';
import { date, deltaPct, num, xaf } from '../../utils/format';
import { CHANNEL_COLOR, TYPE_LABEL } from '../../utils/labels';
import { paymentLabel } from '../../utils/payment';
import type { OperationType } from '../../types/models';
import { QuickActions } from './QuickActions';
import { InventoryAlerts } from './InventoryAlerts';
import { RecentTransactions } from './RecentTransactions';
import { AttentionList } from './AttentionList';
import { PeriodPicker } from './PeriodPicker';
import { TopProducts } from './TopProducts';
import { TopSellers } from './TopSellers';
import { PaymentMethods } from './PaymentMethods';
import { InactiveSellers } from './InactiveSellers';
import { rangeDates, rangeFromParams, salesLink } from './period';

const CHANNELS: OperationType[] = ['FIXED_POS', 'MOBILE', 'MARKET_AGENT'];

/** A01 Dashboard. The period lives in the URL so links and the back button keep it; every figure links to its records. */
export function DashboardPage() {
  const { session } = useAuth();
  const nav = useNavigate();
  const [params, setParams] = useSearchParams();
  const range = rangeFromParams(params);
  const key = 'from' in range ? `${range.from}_${range.to}` : `d${range.days}`;
  const { data, error, loading, reload } = useLoad(() => dashboardApi.get(range), [key]);
  const finance = data?.finance;
  const firstName = session!.user.full_name.split(' ')[0];
  const dates = data ? { from: data.from, to: data.to } : rangeDates(range);
  const setRange = (r: DashboardRange) => setParams('from' in r ? { from: r.from, to: r.to } : { days: String(r.days) });

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Dashboard</h1>
          <p className="welcome">Welcome back, {firstName}!</p>
          {data && <p>{date(`${data.from}T12:00:00Z`)} – {date(`${data.to}T12:00:00Z`)} · compared with the previous {data.days} {data.days === 1 ? 'day' : 'days'}</p>}
        </div>
        <div className="header-actions"><PeriodPicker range={range} onChange={setRange} /></div>
      </div>
      <ErrorNote error={error} retry={reload} />
      {loading && !data ? <Loading /> : data && (
        <>
          <KpiRow>
            <Kpi tone="navy" icon={<ShoppingCart />} value={num(data.current.sales)} label="Total Sales" to={salesLink(dates)}
              delta={deltaPct(data.current.sales, data.previous.sales)}
              foot={<><span>Items sold</span><b>{num(data.current.items)}</b></>} />
            <Kpi tone="gold" icon={<Wallet />} value={xaf(data.current.revenue_xaf)} label={finance ? 'Payments Received' : 'Total Revenue'} to={salesLink(dates)}
              delta={deltaPct(data.current.revenue_xaf, data.previous.revenue_xaf)}
              foot={<span>{data.paymentMix.map(r => `${paymentLabel(r.payment_method)} ${xaf(r.revenue_xaf)}`).join(' · ') || 'No payments yet'}</span>} />
            {finance ? (
              <>
                <Kpi tone="teal" icon={<Users />} value={num(finance.customers)} label="Customers" to="/loyalty?tab=members"
                  foot={<><span>New customers</span><b>{num(finance.new_customers)}</b></>} />
                <Kpi tone="purple" icon={<Star />} value={num(finance.points_earned)} label="Loyalty Points Earned" to="/loyalty"
                  foot={<><span>Points redeemed</span><b>{num(finance.points_redeemed)}</b></>} />
              </>
            ) : (
              <>
                <Kpi tone="blue" icon={<Package />} value={num(data.current.items)} label="Items Sold" to={salesLink(dates)} delta={deltaPct(data.current.items, data.previous.items)} />
                <Kpi tone="purple" icon={<Store />} value={num(data.activeOperations)} label="Active Sales Locations" to="/locations" />
              </>
            )}
          </KpiRow>

          <StatRow five>
            <StatCard tone="blue" icon={<Boxes size={20} />} value={xaf(data.inventoryValueXaf)} label="Inventory Value (at selling price)" />
            <StatCard tone="red" icon={<TriangleAlert size={20} />} value={num(data.lowStockItems)} label="Low Stock Items" />
            {finance && (
              <>
                <StatCard tone="green" icon={<Handshake size={20} />} value={xaf(finance.commissions_xaf)} label="Commissions (Earned)" />
                <StatCard tone="purple" icon={<HandCoins size={20} />} value={xaf(finance.payouts_paid_xaf)} label="Payouts (Paid)" />
                <StatCard tone="amber" icon={<Clock size={20} />} value={xaf(finance.payouts_outstanding_xaf)} label="Payouts (Outstanding)" />
              </>
            )}
          </StatRow>

          <div className="grid-dash">
            <Panel title="Sales & Revenue Trend" icon={<BarChart3 size={19} />} action={<Link className="link" to={salesLink(dates)}>View Sales <ArrowRight size={15} /></Link>}>
              {data.current.sales === 0 ? <p className="muted pad">No sales in this period yet.</p>
                : <SalesChart data={data.salesByDay} onSelect={d => nav(salesLink({ from: d, to: d }))} />}
            </Panel>
            <Panel title="Sales by Channel" icon={<PieChart size={19} />}>
              <Donut centre={num(data.current.sales)} label="Sales" segments={CHANNELS.map(t => ({
                label: TYPE_LABEL[t], color: CHANNEL_COLOR[t], value: Number(data.salesByChannel.find(c => c.channel === t)?.sales ?? 0),
                to: salesLink(dates, { channel: t }),
              }))} />
            </Panel>
            <PaymentMethods rows={data.paymentMix} dates={dates} />
          </div>

          <div className="grid-dash-bottom">
            <RecentTransactions />
            <InventoryAlerts />
            <QuickActions />
          </div>

          <div className="grid-3">
            <TopProducts rows={data.topProducts} totalRevenue={Number(data.current.revenue_xaf)} />
            <TopSellers rows={data.topSellers} dates={dates} />
            <Panel title="Needs Attention" icon={<Activity size={19} />}>
              <AttentionList attention={data.attention} openShifts={data.openShifts} inactiveSellers={data.inactiveSellers.length} />
            </Panel>
          </div>

          <div className="grid-2">
            <Panel title="Recent Activities" icon={<Clock size={19} />} action={<Link className="link" to="/settings?tab=audit">View All <ArrowRight size={15} /></Link>}>
              <PagedActivity showModule />
            </Panel>
            <div id="inactive-sellers"><InactiveSellers rows={data.inactiveSellers} days={data.inactivityDays} /></div>
          </div>
        </>
      )}
    </>
  );
}
