import { Link, useSearchParams } from 'react-router-dom';
import { BadgePercent, Download, Package, Plus, Receipt, Star, Wallet, X } from 'lucide-react';
import { operationsApi, salesApi, staffApi } from '../../api/services';
import { useAuth } from '../../auth/AuthContext';
import { useLoad } from '../../hooks/useLoad';
import { usePagedQuery } from '../../hooks/usePagedQuery';
import { useDebouncedState } from '../../hooks/useDebounced';
import { useAction } from '../../hooks/useAction';
import { fetchAll } from '../../api/pages';
import { ErrorNote, FilterBar, PageHeader, Pager, Panel, SearchInput, StatCard, StatRow, ListState } from '../../components/ui';
import { SalesTable } from '../../components/domain';
import { dateTime, doualaDate, num, xaf } from '../../utils/format';
import { saveFile, toCsv } from '../../utils/files';
import { TYPE_LABEL } from '../../utils/labels';
import { PAYMENT_LABEL, paymentLabel } from '../../utils/payment';

export function SalesListPage() {
  const [params, setParams] = useSearchParams();
  const from = params.get('from') ?? doualaDate(-29);
  const to = params.get('to') ?? doualaDate();
  const operationId = params.get('operation') ?? '';
  const sellerId = params.get('seller') ?? '';
  const channel = params.get('channel') ?? '';
  const paymentMethod = params.get('payment') ?? '';
  const { can } = useAuth();
  const setParam = (k: string, v: string) => { const p = new URLSearchParams(params); if (v) p.set(k, v); else p.delete(k); setParams(p, { replace: true }); };

  const ops = useLoad(() => operationsApi.all(), []);
  const sellers = useLoad(() => (can('ADMIN') ? staffApi.all() : Promise.resolve([])), []);
  const [q, setQ, search] = useDebouncedState('');
  const filters = { from, to, operationId, sellerId, channel, paymentMethod, q: search };
  const { data, items, summary, pager, error, loading, reload } = usePagedQuery((page, size) => salesApi.list({ ...filters, page, size }),
    [from, to, operationId, sellerId, channel, paymentMethod, search]);
  const exporting = useAction();
  const sellerName = sellers.data?.find(s => s.id === sellerId)?.full_name ?? items.find(s => s.seller_id === sellerId)?.seller_name ?? 'Selected seller';
  const chips: [string, string][] = [
    ...(sellerId ? [['seller', `Seller: ${sellerName}`] as [string, string]] : []),
    ...(channel ? [['channel', `Channel: ${TYPE_LABEL[channel as keyof typeof TYPE_LABEL] ?? channel}`] as [string, string]] : []),
    ...(paymentMethod ? [['payment', `Payment: ${paymentLabel(paymentMethod)}`] as [string, string]] : []),
  ];
  const count = Number(summary?.sales ?? 0), revenue = Number(summary?.revenue_xaf ?? 0), units = Number(summary?.units ?? 0);
  const admin = can('ADMIN');

  const exportCsv = () => exporting.run(async () => saveFile(toCsv(
    (await fetchAll((page, size) => salesApi.list({ ...filters, page, size }))).map(r => ({ ...r, created_at: dateTime(r.created_at), channel: r.operation_type ? TYPE_LABEL[r.operation_type] : '' })),
    [['reference', 'Reference'], ['created_at', 'Date & time (Douala)'], ['operation_name', 'Sales location'], ['channel', 'Channel'],
      ['seller_name', 'Seller'], ['item_count', 'Items'], ['total_xaf', 'Amount (XAF)'], ['payment_method', 'Payment']],
  ), `lutiano-sales-${from}-to-${to}.csv`));

  return (
    <>
      <PageHeader title="Sales Transactions" subtitle="All sales across fixed POS, mobile baristas and market agents." crumbs={[['Sales'], ['Transactions']]}
        actions={<>
          <button className="btn" onClick={exportCsv} disabled={!count || exporting.busy}><Download size={16} />{exporting.busy ? 'Exporting…' : 'Export CSV'}</button>
          {admin && <Link className="btn gold" to="/sales/record"><Plus size={16} />Record Sale</Link>}
        </>} />
      <FilterBar labelled>
        <label><span>From</span><input className="input" type="date" value={from} max={to} onChange={e => setParam('from', e.target.value)} /></label>
        <label><span>To</span><input className="input" type="date" value={to} min={from} onChange={e => setParam('to', e.target.value)} /></label>
        <label><span>Sales Location</span>
          <select className="input" value={operationId} onChange={e => setParam('operation', e.target.value)}>
            <option value="">All Locations</option>{(ops.data ?? []).map(o => <option key={o.id} value={o.id}>{o.name}</option>)}
          </select>
        </label>
        <label><span>Channel</span>
          <select className="input" value={channel} onChange={e => setParam('channel', e.target.value)}>
            <option value="">All Channels</option>{Object.entries(TYPE_LABEL).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
          </select>
        </label>
        <label><span>Payment</span>
          <select className="input" value={paymentMethod} onChange={e => setParam('payment', e.target.value)}>
            <option value="">All Methods</option>{Object.entries(PAYMENT_LABEL).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
          </select>
        </label>
        <label><span>Search</span><SearchInput value={q} onChange={setQ} placeholder="Reference, seller…" /></label>
      </FilterBar>
      {chips.length > 0 && (
        <div className="filter-chips" aria-label="Active filters">
          {chips.map(([k, label]) => <button key={k} onClick={() => setParam(k, '')} aria-label={`Remove filter ${label}`}>{label}<X size={13} /></button>)}
        </div>
      )}
      <ErrorNote error={error || exporting.error} retry={reload} />
      <StatRow five>
        <StatCard tone="green" icon={<Receipt size={20} />} value={num(count)} label="Total Sales" />
        <StatCard tone="purple" icon={<Wallet size={20} />} value={xaf(revenue)} label="Total Revenue" />
        <StatCard tone="blue" icon={<Package size={20} />} value={num(units)} label="Items Sold" />
        {admin && <StatCard tone="amber" icon={<Star size={20} />} value={num(Number(summary?.points_issued ?? 0))} label="Points Issued" />}
        {admin && <StatCard tone="green" icon={<BadgePercent size={20} />} value={xaf(Number(summary?.commission_xaf ?? 0))} label="Commission" />}
      </StatRow>
      <Panel>
        <ListState loading={loading} data={data} items={items} empty={{ title: 'No sales in this period', text: 'Change the date range or filters.' }}><SalesTable rows={items} /></ListState>
        <Pager {...pager} />
      </Panel>
    </>
  );
}
