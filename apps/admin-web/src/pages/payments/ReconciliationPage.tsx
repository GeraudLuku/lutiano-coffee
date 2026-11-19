import { Link, useSearchParams } from 'react-router-dom';
import { AlertTriangle, Banknote, CheckCircle2, Receipt, Scale, Smartphone } from 'lucide-react';
import { paymentsApi } from '../../api/services';
import { useLoad } from '../../hooks/useLoad';
import { DataTable, Empty, ErrorNote, FilterBar, Loading, Note, PageHeader, Panel, Pill, StatCard, StatRow, TitleCell } from '../../components/ui';
import { date, dateTime, doualaDate, num, xaf } from '../../utils/format';

/** A07 reconciliation: each day's sales against confirmed payments, plus sales whose payments don't add up. */
export function ReconciliationPage() {
  const [params, setParams] = useSearchParams();
  const from = params.get('from') ?? doualaDate(-6), to = params.get('to') ?? doualaDate();
  const setParam = (k: string, v: string) => { const p = new URLSearchParams(params); p.set(k, v); setParams(p, { replace: true }); };
  const { data: r, error, loading, reload } = useLoad(() => paymentsApi.reconciliation(from, to), [from, to]);

  return (
    <>
      <PageHeader title="Reconciliation" subtitle="Daily sales compared with confirmed payments (Douala business days)." crumbs={[['Payments', '/payments'], ['Reconciliation']]} />
      <FilterBar labelled>
        <label><span>From</span><input className="input" type="date" value={from} max={to} onChange={e => setParam('from', e.target.value)} /></label>
        <label><span>To</span><input className="input" type="date" value={to} min={from} onChange={e => setParam('to', e.target.value)} /></label>
      </FilterBar>
      <ErrorNote error={error} retry={reload} />
      {loading && !r ? <Loading /> : r && (
        <>
          {r.balanced
            ? <Note tone="ok" icon={<CheckCircle2 size={16} />}>Balanced: every sale in this period has confirmed payments matching its total.</Note>
            : <Note tone="warn" icon={<AlertTriangle size={16} />}>{num(r.exceptions.length)} sale{r.exceptions.length === 1 ? '' : 's'} need attention: payments do not match the sale total.</Note>}
          <StatRow>
            <StatCard tone="blue" icon={<Receipt size={20} />} value={xaf(r.salesXaf)} label="Sales" />
            <StatCard tone="green" icon={<Scale size={20} />} value={xaf(r.paymentsXaf)} label="Confirmed payments" />
            <StatCard tone="purple" icon={<Banknote size={20} />} value={xaf(r.cashXaf)} label="Cash" />
            <StatCard tone="amber" icon={<Smartphone size={20} />} value={xaf(r.mobileMoneyXaf)} label="Mobile Money (soon)" />
          </StatRow>
          <Panel title="Daily Summary">
            {r.days.length === 0 ? <Empty title="No sales in this period" /> : (
              <DataTable rows={r.days} rowKey={d => d.day} columns={[
                { header: 'Day', cell: d => <b>{date(d.day)}</b> },
                { header: 'Sales', align: 'right', cell: d => <TitleCell title={xaf(d.sales_xaf)} sub={`${num(d.sales)} sales`} /> },
                { header: 'Payments', align: 'right', cell: d => <TitleCell title={xaf(d.payments_xaf)} sub={`${num(d.payments)} confirmed`} /> },
                { header: 'Cash', align: 'right', cell: d => xaf(d.cash_xaf) },
                { header: 'Mobile Money', align: 'right', cell: d => <span className="muted">{xaf(d.mobile_money_xaf)}</span> },
                { header: 'Difference', align: 'right', cell: d => d.difference_xaf === 0 ? <Pill tone="green">Balanced</Pill> : <Pill tone="red">{xaf(d.difference_xaf)}</Pill> },
              ]} />
            )}
          </Panel>
          {r.exceptions.length > 0 && (
            <Panel title="Exceptions" icon={<AlertTriangle size={19} />}>
              <DataTable rows={r.exceptions} rowKey={x => x.sale_id} columns={[
                { header: 'Sale', cell: x => <TitleCell title={<Link className="link" to={`/sales/${x.sale_id}`}>{x.sale_reference}</Link>} sub={dateTime(x.created_at)} /> },
                { header: 'Location / Seller', cell: x => <TitleCell title={x.operation_name} sub={x.seller_name} /> },
                { header: 'Sale total', align: 'right', cell: x => xaf(x.total_xaf) },
                { header: 'Confirmed paid', align: 'right', cell: x => <TitleCell title={xaf(x.paid_xaf)} sub={x.payment_methods ?? 'No confirmed payment'} /> },
                { header: 'Gap', align: 'right', cell: x => <Pill tone="red">{xaf(x.total_xaf - x.paid_xaf)}</Pill> },
              ]} />
            </Panel>
          )}
        </>
      )}
    </>
  );
}
