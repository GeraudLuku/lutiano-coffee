import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { Banknote, CheckCircle2, Download, Smartphone, Wallet } from 'lucide-react';
import { operationsApi, paymentsApi } from '../../api/services';
import { useLoad } from '../../hooks/useLoad';
import { usePagedQuery } from '../../hooks/usePagedQuery';
import { useDebouncedState } from '../../hooks/useDebounced';
import { useAction } from '../../hooks/useAction';
import { fetchAll } from '../../api/pages';
import { DataTable, ErrorNote, FilterBar, PageHeader, Pager, Panel, SearchInput, StatCard, StatRow, TitleCell, ListState } from '../../components/ui';
import { dateTime, doualaDate, num, xaf } from '../../utils/format';
import { saveFile, toCsv } from '../../utils/files';
import { PAYMENT_STATUS } from '../../utils/paymentStatus';
import { MethodPill, PaymentStatusPill } from './PaymentBadges';

/** A07 payment list: every payment recorded against a sale, separate from the sale itself. */
export function PaymentsListPage() {
  const nav = useNavigate();
  const [params, setParams] = useSearchParams();
  const from = params.get('from') ?? doualaDate(-29), to = params.get('to') ?? doualaDate();
  const method = params.get('method') ?? '', status = params.get('status') ?? '', operationId = params.get('operation') ?? '';
  const setParam = (k: string, v: string) => { const p = new URLSearchParams(params); if (v) p.set(k, v); else p.delete(k); setParams(p, { replace: true }); };
  const methods = useLoad(() => paymentsApi.methods(), []);
  const ops = useLoad(() => operationsApi.all(), []);
  const [q, setQ, search] = useDebouncedState('');
  const filters = { from, to, method, status, operationId, q: search };
  const { data, items, summary, pager, error, loading, reload } = usePagedQuery((page, size) => paymentsApi.list({ ...filters, page, size }),
    [from, to, method, status, operationId, search]);
  const exporting = useAction();
  const total = data?.total ?? 0;

  const exportCsv = () => exporting.run(async () => saveFile(toCsv((await fetchAll((page, size) => paymentsApi.list({ ...filters, page, size }))).map(p => ({ ...p, created_at: dateTime(p.created_at), status: PAYMENT_STATUS[p.status][0] })),
    [['reference', 'Payment'], ['created_at', 'Date & time (Douala)'], ['sale_reference', 'Sale'], ['operation_name', 'Location'], ['seller_name', 'Seller'],
      ['method_label', 'Method'], ['amount_xaf', 'Amount (XAF)'], ['status', 'Status'], ['provider_reference', 'Provider reference']]), `lutiano-payments-${from}-to-${to}.csv`));

  return (
    <>
      <PageHeader title="Payments" subtitle="Payments recorded against sales. Cash is confirmed by the seller; Mobile Money arrives with its provider integration."
        crumbs={[['Payments']]} actions={<button className="btn" onClick={exportCsv} disabled={!total || exporting.busy}><Download size={16} />{exporting.busy ? 'Exporting…' : 'Export CSV'}</button>} />
      <FilterBar labelled>
        <label><span>From</span><input className="input" type="date" value={from} max={to} onChange={e => setParam('from', e.target.value)} /></label>
        <label><span>To</span><input className="input" type="date" value={to} min={from} onChange={e => setParam('to', e.target.value)} /></label>
        <label><span>Method</span>
          <select className="input" value={method} onChange={e => setParam('method', e.target.value)}>
            <option value="">All Methods</option>
            {(methods.data ?? []).map(m => <option key={m.code} value={m.code}>{m.label}{m.available ? '' : ' (not available yet)'}</option>)}
          </select>
        </label>
        <label><span>Status</span>
          <select className="input" value={status} onChange={e => setParam('status', e.target.value)}>
            <option value="">All Statuses</option>{Object.entries(PAYMENT_STATUS).map(([k, [l]]) => <option key={k} value={k}>{l}</option>)}
          </select>
        </label>
        <label><span>Sales Location</span>
          <select className="input" value={operationId} onChange={e => setParam('operation', e.target.value)}>
            <option value="">All Locations</option>{(ops.data ?? []).map(o => <option key={o.id} value={o.id}>{o.name}</option>)}
          </select>
        </label>
        <label><span>Search</span><SearchInput value={q} onChange={setQ} placeholder="Payment, sale, seller…" /></label>
      </FilterBar>
      <ErrorNote error={error || exporting.error} retry={reload} />
      <StatRow>
        <StatCard tone="green" icon={<CheckCircle2 size={20} />} value={xaf(summary?.confirmed_xaf ?? 0)} label={`Confirmed (${num(summary?.confirmed_count ?? 0)} payments)`} />
        <StatCard tone="blue" icon={<Banknote size={20} />} value={xaf(summary?.cash_xaf ?? 0)} label="Cash" />
        <StatCard tone="amber" icon={<Smartphone size={20} />} value={xaf(summary?.mobile_money_xaf ?? 0)} label="Mobile Money (soon)" />
        <StatCard tone="purple" icon={<Wallet size={20} />} value={num(summary?.pending_count ?? 0)} label="Pending" />
      </StatRow>
      <Panel>
        <ListState loading={loading} data={data} items={items} empty={{ title: 'No payments in this period' }}>
          <DataTable rows={items} rowKey={p => p.id} onRowClick={p => nav(`/payments/${p.id}`)} columns={[
            { header: 'Payment', cell: p => <TitleCell title={p.reference} sub={dateTime(p.created_at)} /> },
            { header: 'Sale', cell: p => <Link className="link" to={`/sales/${p.sale_id}`} onClick={e => e.stopPropagation()}>{p.sale_reference}</Link> },
            { header: 'Location / Seller', cell: p => <TitleCell title={p.operation_name} sub={p.seller_name} /> },
            { header: 'Method', cell: p => <MethodPill code={p.method} label={p.method_label} /> },
            { header: 'Amount', align: 'right', cell: p => <b>{xaf(p.amount_xaf)}</b> },
            { header: 'Status', cell: p => <PaymentStatusPill status={p.status} /> },
          ]} />
        </ListState>
        <Pager {...pager} />
      </Panel>
    </>
  );
}
