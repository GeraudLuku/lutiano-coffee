import { useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Ban, Banknote, Boxes, Check, FileText, History, Info, Paperclip, Receipt, Upload, X } from 'lucide-react';
import { refundsApi } from '../../api/services';
import { useAuth } from '../../auth/AuthContext';
import { useAction } from '../../hooks/useAction';
import { useLoad } from '../../hooks/useLoad';
import { ErrorNote, Facts, Loading, Note, PageHeader, Panel, ReasonModal, type ReasonText } from '../../components/ui';
import { dateTime, xaf } from '../../utils/format';
import { saveFile } from '../../utils/files';
import { ADJUSTMENT_STATUS } from '../../utils/inventoryLabels';
import { FUND_SOURCE, REFUND_EVENT, REFUND_REASON } from '../../utils/refunds';
import type { RefundDetail } from '../../types/models';
import { PayRefundModal } from './PayRefundModal';
import { RefundStatusPill } from './RefundStatusPill';
import { StockReturnModal } from './StockReturnModal';

type Prompt = ReasonText & { run: (note: string) => Promise<unknown>; success: string };

/** A08 refund detail: request → approval → cash payment, with evidence, returned stock and history. */
export function RefundDetailPage() {
  const { id } = useParams();
  const { session } = useAuth();
  const { data: r, error, loading, reload } = useLoad(() => refundsApi.get(id!), [id]);
  const settings = useLoad(() => refundsApi.settings(), []);
  const action = useAction();
  const [prompt, setPrompt] = useState<Prompt | null>(null);
  const [modal, setModal] = useState<'' | 'pay' | 'stock'>('');

  if (loading && !r) return <Loading />;
  if (!r) return <ErrorNote error={error} retry={reload} />;
  const secondApprover = settings.data?.secondApprover ?? true;
  const ownRequest = r.requested_by === session!.user.id;
  const mustOther = secondApprover && ownRequest;
  const decidable = r.status === 'REQUESTED', unpaid = r.status === 'APPROVED';
  const canReturn = (r.status === 'APPROVED' || r.status === 'PAID') && r.returnable.some(p => p.tracked && p.returnable > 0);

  async function submitPrompt(note: string) {
    if (prompt && await action.run(() => prompt.run(note), prompt.success) !== undefined) { setPrompt(null); reload(); }
  }
  const close = () => { setModal(''); setPrompt(null); action.setError(''); };
  const done = () => { setModal(''); reload(); };

  return (
    <>
      <PageHeader title={r.reference} badge={<RefundStatusPill status={r.status} />} subtitle={`Requested ${dateTime(r.requested_at)} by ${r.requested_by_name}`}
        crumbs={[['Sales Transactions', '/sales'], ['Refunds', '/sales/refunds'], [r.reference]]}
        actions={<>
          {decidable && <>
            <button className="btn" disabled={mustOther} title={mustOther ? 'Another administrator must decide a refund you requested.' : undefined}
              onClick={() => setPrompt({ title: 'Reject refund', label: 'Reason for rejection', confirm: 'Reject Refund', success: 'Refund rejected.', run: n => refundsApi.reject(r.id, n) })}>
              <X size={16} />Reject</button>
            <button className="btn gold" disabled={mustOther} title={mustOther ? 'Another administrator must approve a refund you requested.' : undefined}
              onClick={() => setPrompt({ title: 'Approve refund', label: 'Note (optional)', confirm: 'Approve Refund', required: false, success: 'Refund approved. Record the cash payment when it is handed over.', run: n => refundsApi.approve(r.id, n || undefined) })}>
              <Check size={16} />Approve</button>
          </>}
          {unpaid && <button className="btn gold" onClick={() => setModal('pay')}><Banknote size={16} />Record Cash Refund</button>}
          {canReturn && <button className="btn" onClick={() => setModal('stock')}><Boxes size={16} />Record Returned Stock</button>}
          {(decidable || unpaid) && (
            <button className="btn danger" onClick={() => setPrompt({ title: 'Cancel refund', label: 'Why is this refund being cancelled?', confirm: 'Cancel Refund', success: 'Refund cancelled.', run: n => refundsApi.cancel(r.id, n) })}>
              <Ban size={16} />Cancel</button>
          )}
        </>} />
      <ErrorNote error={action.error && !prompt ? action.error : ''} />
      {decidable && mustOther && <Note>You requested this refund, so another administrator must approve or reject it.</Note>}
      {unpaid && <Note tone="warn">Approved but unpaid. Record the cash refund once it has been handed over from a cash shift or an authorised advance.</Note>}

      <div className="grid-2">
        <Panel title="Refund Information" icon={<Info size={19} />}>
          <Facts items={[
            ['Amount', <b>{xaf(r.amount_xaf)}</b>], ['Type', r.refund_type === 'FULL' ? 'Full refund' : 'Partial refund'],
            ['Reason', REFUND_REASON[r.reason]], ['Note', r.note ?? '—'],
            ['Customer', r.customer_name ? `${r.customer_name}${r.customer_phone ? ` · ${r.customer_phone}` : ''}` : r.customer_phone ?? 'Anonymous'],
            ['Requested', `${dateTime(r.requested_at)} by ${r.requested_by_name}`],
            ['Decision', r.decided_at ? `${r.status === 'REJECTED' ? 'Rejected' : 'Approved'} ${dateTime(r.decided_at)} by ${r.decided_by_name}${r.decision_note ? `: ${r.decision_note}` : ''}` : 'Waiting for approval'],
          ]} />
        </Panel>
        <Panel title="Original Sale" icon={<Receipt size={19} />}>
          <Facts items={[
            ['Sale', <Link className="link" to={`/sales/${r.sale_id}`}>{r.sale_reference}</Link>], ['Sale date', dateTime(r.sale_created_at)],
            ['Sale total', xaf(r.sale_total_xaf)], ['Still refundable', xaf(r.sale_refundable_xaf)],
            ['Sales location', <Link className="link" to={`/locations/${r.operation_id}`}>{r.operation_name} ({r.operation_code})</Link>],
            ['Seller', r.seller_name],
          ]} />
        </Panel>
      </div>

      <div className="grid-2">
        <Panel title="Cash Payment" icon={<Banknote size={19} />}>
          {r.status === 'PAID' ? (
            <Facts items={[
              ['Fund', r.fund_source ? FUND_SOURCE[r.fund_source] : '—'],
              ...(r.fund_source === 'SHIFT'
                ? [['Cash shift', r.shift_seller_name ? `${r.shift_seller_name}'s shift` : '—']] as [string, string][]
                : [['Advance reference', r.advance_reference ?? '—'], ['Advance amount', r.advance_amount_xaf == null ? '—' : xaf(r.advance_amount_xaf)],
                   ['Unused cash returned', r.advance_returned_xaf == null ? '—' : xaf(r.advance_returned_xaf)]] as [string, string][]),
              ['Handed to', r.recipient_name ?? '—'], ['Recipient acknowledged', r.recipient_acknowledged ? 'Yes' : 'No'],
              ['Recorded', r.paid_at ? `${dateTime(r.paid_at)} by ${r.paid_by_name}` : '—'], ['Note', r.payment_note ?? '—'],
            ]} />
          ) : <p className="muted">{unpaid ? 'Not paid yet.' : decidable ? 'Payment can be recorded after approval.' : 'No payment: this refund was not approved.'}</p>}
        </Panel>
        <Panel title="Returned Stock" icon={<Boxes size={19} />}>
          <p className="muted small">A refund never puts stock back by itself. Goods handed back are recorded separately and need approval.</p>
          {r.stock_returns.length === 0 ? <p className="muted">No returned stock recorded.</p> : (
            <ul className="check-list">{r.stock_returns.map(s => (
              <li key={s.id}><span>{s.quantity_delta} × {s.product_name}<small className="block">{dateTime(s.created_at)}</small></span>
                <span className={`pill pill-${ADJUSTMENT_STATUS[s.status][1]}`}>{ADJUSTMENT_STATUS[s.status][0]}</span></li>
            ))}</ul>
          )}
          {r.stock_returns.length > 0 && <Link className="link" to="/inventory?tab=approvals">Open inventory approvals</Link>}
        </Panel>
      </div>

      <div className="grid-2">
        <EvidencePanel refund={r} onChanged={reload} />
        <Panel title="History" icon={<History size={19} />}>
          <ul className="timeline compact">{r.events.map(e => (
            <li key={e.id} className={(REFUND_EVENT[e.event_type] ?? [e.event_type, ''])[1]}>
              <span className="when">{dateTime(e.created_at)}</span>
              <div><b>{(REFUND_EVENT[e.event_type] ?? [e.event_type])[0]}</b><small>{[e.actor_name, e.details].filter(Boolean).join(' · ')}</small></div>
            </li>
          ))}</ul>
        </Panel>
      </div>

      {prompt && <ReasonModal prompt={prompt} busy={action.busy} error={action.error} onSubmit={submitPrompt} onClose={close} />}
      {modal === 'pay' && <PayRefundModal refund={r} onClose={close} onDone={done} />}
      {modal === 'stock' && <StockReturnModal refund={r} secondApprover={secondApprover} onClose={close} onDone={done} />}
    </>
  );
}

function EvidencePanel({ refund: r, onChanged }: { refund: RefundDetail; onChanged: () => void }) {
  const input = useRef<HTMLInputElement>(null);
  const action = useAction();
  const upload = async (file?: File) => {
    if (file && await action.run(() => refundsApi.addEvidence(r.id, file), 'Evidence added.') !== undefined) onChanged();
    if (input.current) input.current.value = '';
  };
  return (
    <Panel title={`Evidence (${r.evidence.length})`} icon={<Paperclip size={19} />}
      action={<button className="btn sm" disabled={action.busy} onClick={() => input.current?.click()}><Upload size={15} />{action.busy ? 'Uploading…' : 'Add file'}</button>}>
      <input ref={input} type="file" hidden accept="application/pdf,image/jpeg,image/png,image/webp" onChange={e => upload(e.target.files?.[0])} />
      <ErrorNote error={action.error} />
      {r.evidence.length === 0 ? <p className="muted">Signed refund slip, photo of the product, or the advance authorisation (PDF, JPEG, PNG or WebP, up to 10 MB).</p> : (
        <ul className="check-list">{r.evidence.map(f => (
          <li key={f.id}>
            <span><FileText size={15} aria-hidden="true" /> {f.file_name}<small className="block">{dateTime(f.uploaded_at)}{f.uploaded_by_name ? ` · ${f.uploaded_by_name}` : ''}</small></span>
            <button className="btn sm" onClick={() => action.run(async () => saveFile(await refundsApi.evidenceFile(r.id, f.id), f.file_name))}>Download</button>
          </li>
        ))}</ul>
      )}
    </Panel>
  );
}
