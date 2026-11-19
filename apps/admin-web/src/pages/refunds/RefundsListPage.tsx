import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { CheckCircle2, Clock, Download, Layers, Plus, RotateCcw, XCircle } from 'lucide-react';
import { operationsApi, refundsApi } from '../../api/services';
import { useLoad } from '../../hooks/useLoad';
import { usePagedQuery } from '../../hooks/usePagedQuery';
import { useDebouncedState } from '../../hooks/useDebounced';
import { useAction } from '../../hooks/useAction';
import { fetchAll } from '../../api/pages';
import { DataTable, ErrorNote, FilterBar, PageHeader, Pager, Panel, SearchInput, StatCard, StatRow, TitleCell, ListState } from '../../components/ui';
import { dateTime, doualaDate, num, xaf } from '../../utils/format';
import { saveFile, toCsv } from '../../utils/files';
import { REFUND_REASON, REFUND_STATUS } from '../../utils/refunds';
import type { RefundStatus } from '../../types/models';
import { CreateRefundModal } from './CreateRefundModal';
import { RefundStatusPill } from './RefundStatusPill';

/** A08 Refund Management (design 5.4): request queue and history. */
export function RefundsListPage() {
  const nav = useNavigate();
  const [params, setParams] = useSearchParams();
  const from = params.get('from') ?? doualaDate(-29), to = params.get('to') ?? doualaDate();
  const status = params.get('status') ?? '', operationId = params.get('operation') ?? '';
  const setParam = (k: string, v: string) => { const p = new URLSearchParams(params); if (v) p.set(k, v); else p.delete(k); setParams(p, { replace: true }); };
  const ops = useLoad(() => operationsApi.all(), []);
  const [q, setQ, search] = useDebouncedState('');
  const [creating, setCreating] = useState(false);
  const filters = { from, to, status, operationId, q: search };
  const { data, items, summary, pager, error, loading, reload } = usePagedQuery((page, size) => refundsApi.list({ ...filters, page, size }),
    [from, to, status, operationId, search]);
  const exporting = useAction();
  const total = data?.total ?? 0;

  const exportCsv = () => exporting.run(async () => saveFile(toCsv((await fetchAll((page, size) => refundsApi.list({ ...filters, page, size }))).map(r => ({ ...r, requested_at: dateTime(r.requested_at), status: REFUND_STATUS[r.status][0], reason: REFUND_REASON[r.reason] })),
    [['reference', 'Refund'], ['requested_at', 'Requested (Douala)'], ['sale_reference', 'Original sale'], ['operation_name', 'Location'], ['customer_name', 'Customer'],
      ['amount_xaf', 'Amount (XAF)'], ['refund_type', 'Type'], ['reason', 'Reason'], ['status', 'Status']]), `lutiano-refunds-${from}-to-${to}.csv`));

  return (
    <>
      <PageHeader title="Refunds" subtitle="Manage and track sale refunds. Approval and cash payment are separate steps."
        crumbs={[['Sales Transactions', '/sales'], ['Refunds']]}
        actions={<>
          <button className="btn" onClick={exportCsv} disabled={!total || exporting.busy}><Download size={16} />{exporting.busy ? 'Exporting…' : 'Export CSV'}</button>
          <button className="btn gold" onClick={() => setCreating(true)}><Plus size={16} />Create Refund</button>
        </>} />
      <StatRow five>
        <StatCard tone="amber" icon={<RotateCcw size={20} />} value={num(summary?.refunds ?? 0)} label="Total Refunds" />
        <StatCard tone="purple" icon={<Layers size={20} />} value={xaf(summary?.refunded_xaf ?? 0)} label="Total Refunded" />
        <StatCard tone="green" icon={<CheckCircle2 size={20} />} value={num(summary?.paid ?? 0)} label="Completed" />
        <StatCard tone="blue" icon={<Clock size={20} />} value={num(summary?.pending ?? 0)} label="Pending (approval or payment)" />
        <StatCard tone="red" icon={<XCircle size={20} />} value={num(summary?.rejected ?? 0)} label="Rejected" />
      </StatRow>
      <FilterBar labelled>
        <label><span>From</span><input className="input" type="date" value={from} max={to} onChange={e => setParam('from', e.target.value)} /></label>
        <label><span>To</span><input className="input" type="date" value={to} min={from} onChange={e => setParam('to', e.target.value)} /></label>
        <label><span>Sales Location</span>
          <select className="input" value={operationId} onChange={e => setParam('operation', e.target.value)}>
            <option value="">All Locations</option>{(ops.data ?? []).map(o => <option key={o.id} value={o.id}>{o.name}</option>)}
          </select>
        </label>
        <label><span>Refund Status</span>
          <select className="input" value={status} onChange={e => setParam('status', e.target.value)}>
            <option value="">All Statuses</option>{(Object.keys(REFUND_STATUS) as RefundStatus[]).map(s => <option key={s} value={s}>{REFUND_STATUS[s][0]}</option>)}
          </select>
        </label>
        <label><span>Search</span><SearchInput value={q} onChange={setQ} placeholder="Refund, sale or customer…" /></label>
      </FilterBar>
      <ErrorNote error={error || exporting.error} retry={reload} />
      <Panel>
        <ListState loading={loading} data={data} items={items} empty={{ title: 'No refunds in this period' }}>
          <DataTable rows={items} rowKey={r => r.id} onRowClick={r => nav(`/sales/refunds/${r.id}`)} columns={[
            { header: 'Refund Ref.', cell: r => <b>{r.reference}</b> },
            { header: 'Original Sale', cell: r => <Link className="link" to={`/sales/${r.sale_id}`} onClick={e => e.stopPropagation()}>{r.sale_reference}</Link> },
            { header: 'Customer', cell: r => <TitleCell title={r.customer_name ?? 'Anonymous'} sub={r.customer_phone ?? r.operation_name} /> },
            { header: 'Amount', align: 'right', cell: r => <TitleCell title={xaf(r.amount_xaf)} sub={r.refund_type === 'FULL' ? 'Full' : 'Partial'} /> },
            { header: 'Reason', cell: r => REFUND_REASON[r.reason] },
            { header: 'Status', cell: r => <RefundStatusPill status={r.status} /> },
            { header: 'Date', cell: r => dateTime(r.requested_at) },
          ]} />
        </ListState>
        <Pager {...pager} />
      </Panel>
      {creating && <CreateRefundModal onClose={() => setCreating(false)} onDone={r => nav(`/sales/refunds/${r.id}`)} />}
    </>
  );
}
