import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ArrowUpRight, Calculator, Check, HandCoins, Info, ListOrdered, RotateCcw } from 'lucide-react';
import { cashApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { useLoad } from '../../hooks/useLoad';
import { usePagedQuery } from '../../hooks/usePagedQuery';
import { DataTable, ErrorNote, Facts, LabelPill, Loading, Note, PageHeader, Pager, Panel, ReasonModal, StatCard, StatRow, type ReasonText } from '../../components/ui';
import { dateTime, signedXaf, xaf } from '../../utils/format';
import { ENTRY_TYPE, REVIEW_STATUS } from '../../utils/cash';
import { OutflowsTable, RemittancesTable } from './CashTables';
import { useCashActions } from './useCashActions';

type Prompt = ReasonText & { decision: 'ACCEPT' | 'RECOUNT' };
const SOURCE_LINK: Record<string, (id: string) => string> = { SALE: id => `/sales/${id}`, REFUND: id => `/sales/refunds/${id}` };

/** One shift's cash ledger, counts and custody, with the administrator's variance review. */
export function ShiftDetailPage() {
  const { id } = useParams();
  const { data: s, error, loading, reload } = useLoad(() => cashApi.shift(id!), [id]);
  const ledger = usePagedQuery((page, size) => cashApi.shiftLedger(id!, { page, size }), [id]);
  const actions = useCashActions(() => { reload(); ledger.reload(); });
  const review = useAction();
  const [prompt, setPrompt] = useState<Prompt | null>(null);

  if (loading && !s) return <Loading />;
  if (!s) return <ErrorNote error={error} retry={reload} />;
  const variance = s.counted_cash == null ? null : Number(s.counted_cash) - Number(s.expected_cash);
  const reviewable = actions.admin && s.review_status === 'PENDING_REVIEW';

  async function submitReview(note: string) {
    if (prompt && await review.run(() => cashApi.review(s!.id, prompt.decision, note),
      prompt.decision === 'ACCEPT' ? 'Variance accepted.' : 'Recount requested. The seller will see it in the app.') !== undefined) { setPrompt(null); reload(); }
  }

  return (
    <>
      <PageHeader title={`${s.seller_name}'s cash shift`} badge={s.status === 'OPEN' ? <LabelPill map={{ OPEN: ['Open', 'green'] }} value="OPEN" />
        : s.review_status ? <LabelPill map={REVIEW_STATUS} value={s.review_status} /> : undefined}
        subtitle={`Opened ${dateTime(s.opened_at)}${s.closed_at ? ` · closed ${dateTime(s.closed_at)}` : ''}`}
        crumbs={[['Sales', '/sales'], ['Cash Management', '/sales/shifts'], ['Shift']]}
        actions={reviewable && <>
          <button className="btn" onClick={() => setPrompt({ decision: 'RECOUNT', title: 'Request a recount', label: 'What should the seller check?', confirm: 'Request Recount' })}><RotateCcw size={16} />Request Recount</button>
          <button className="btn gold" onClick={() => setPrompt({ decision: 'ACCEPT', title: 'Accept variance', label: 'Review note', confirm: 'Accept Variance' })}><Check size={16} />Accept Variance</button>
        </>} />
      {s.review_status === 'PENDING_REVIEW' && <Note tone="warn">Counted cash differs from expected by {signedXaf(variance ?? 0)}. {s.variance_reason ? `Seller's reason: “${s.variance_reason}”.` : ''}</Note>}
      {s.review_status === 'RECOUNT_REQUESTED' && <Note>Waiting for the seller to recount{s.review_note ? `: “${s.review_note}”` : ''}.</Note>}

      <StatRow five>
        <StatCard tone="blue" icon={<Calculator size={20} />} value={xaf(s.opening_float)} label="Opening Float" />
        <StatCard tone="green" icon={<ListOrdered size={20} />} value={xaf(s.cash_sales_xaf ?? 0)} label={`Cash Sales (${s.sale_count})`} />
        <StatCard tone="purple" icon={<ArrowUpRight size={20} />} value={xaf(Number(s.refunds_paid_xaf ?? 0) + Number(s.outflows_xaf ?? 0))} label="Refunds & Outflows" />
        <StatCard tone="amber" icon={<HandCoins size={20} />} value={xaf(s.remitted_xaf ?? 0)} label="Remitted" />
        <StatCard tone="navy" icon={<Info size={20} />} value={xaf(s.expected_cash)} label={s.status === 'OPEN' ? 'Expected in Drawer' : 'Expected at Close'} />
      </StatRow>

      <div className="grid-2">
        <Panel title="Reconciliation" icon={<Calculator size={19} />}>
          <Facts items={[
            ['Opening float', xaf(s.opening_float)], ['+ Cash sales', xaf(s.cash_sales_xaf ?? 0)], ['− Refunds paid', xaf(s.refunds_paid_xaf ?? 0)],
            ['− Approved outflows', xaf(s.outflows_xaf ?? 0)], ['− Remittances', xaf(s.remitted_xaf ?? 0)], ['= Expected cash', <b>{xaf(s.expected_cash)}</b>],
            ['Counted cash', s.counted_cash == null ? 'Not counted yet' : xaf(s.counted_cash)],
            ['Variance', variance == null ? '—' : <span className={variance ? 'warn' : ''}>{signedXaf(variance)}</span>],
            ...(s.reviewed_at ? [['Review', `${s.reviewed_by_name ?? 'Recount'} ${dateTime(s.reviewed_at)}${s.review_note ? ` · ${s.review_note}` : ''}`] as [string, string]] : []),
          ]} />
        </Panel>
        <Panel title={`Cash Counts (${s.counts.length})`} icon={<ListOrdered size={19} />}>
          {s.counts.length === 0 ? <p className="muted">Counted when the seller closes the shift.</p> : (
            <DataTable rows={s.counts} rowKey={c => c.id} columns={[
              { header: 'Count', cell: c => <b>{c.kind === 'INITIAL' ? 'At closing' : 'Recount'}</b> },
              { header: 'When', cell: c => dateTime(c.created_at) },
              { header: 'Counted', align: 'right', cell: c => xaf(c.counted_xaf) },
              { header: 'Variance', align: 'right', cell: c => <span className={Number(c.variance_xaf) ? 'warn' : ''}>{signedXaf(Number(c.variance_xaf))}</span> },
              { header: 'Reason', cell: c => c.reason ?? '—' },
            ]} />
          )}
        </Panel>
      </div>

      <Panel title="Cash Ledger" icon={<ListOrdered size={19} />}>
        <DataTable rows={ledger.items} rowKey={e => e.id} columns={[
          { header: '#', cell: e => e.seq },
          { header: 'When', cell: e => dateTime(e.created_at) },
          { header: 'Movement', cell: e => <LabelPill map={ENTRY_TYPE} value={e.entry_type} /> },
          { header: 'Source', cell: e => (e.source_reference && e.source_type && SOURCE_LINK[e.source_type]
            ? <Link className="link" to={SOURCE_LINK[e.source_type](e.source_id!)}>{e.source_reference}</Link> : e.source_reference ?? e.note ?? '—') },
          { header: 'By', cell: e => e.actor_name ?? '—' },
          { header: 'Amount', align: 'right', cell: e => <b className={Number(e.amount_xaf) < 0 ? 'warn' : ''}>{signedXaf(Number(e.amount_xaf))}</b> },
          { header: 'Balance', align: 'right', cell: e => xaf(e.balance_after_xaf) },
        ]} />
        <Pager {...ledger.pager} />
        <p className="muted small">Newest first. Ledger entries are permanent. Corrections are made with new entries, never by editing old ones.</p>
      </Panel>

      {s.outflows.length > 0 && <Panel title={`Outflows (${s.outflows.length})`} icon={<ArrowUpRight size={19} />}><OutflowsTable rows={s.outflows} actions={actions.outflowActions} /></Panel>}
      {s.remittances.length > 0 && <Panel title={`Remittances (${s.remittances.length})`} icon={<HandCoins size={19} />}><RemittancesTable rows={s.remittances} actions={actions.remittanceActions} /></Panel>}

      {actions.modals}
      {prompt && <ReasonModal prompt={prompt} busy={review.busy} error={review.error} onSubmit={submitReview} onClose={() => { setPrompt(null); review.setError(''); }} />}
    </>
  );
}
