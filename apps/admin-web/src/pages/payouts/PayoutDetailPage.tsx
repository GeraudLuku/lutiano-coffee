import { useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Ban, Check, FileText, HandCoins, History, Info, Paperclip, Undo2, Upload, User, X } from 'lucide-react';
import { payoutsApi } from '../../api/services';
import { useAuth } from '../../auth/AuthContext';
import { useAction } from '../../hooks/useAction';
import { useLoad } from '../../hooks/useLoad';
import { ErrorNote, Facts, Loading, Note, PageHeader, Panel, ReasonModal, type ReasonText } from '../../components/ui';
import { dateTime, xaf } from '../../utils/format';
import { saveFile } from '../../utils/files';
import { PAYOUT_EVENT, PAYOUT_METHOD, periodLabel } from '../../utils/payouts';
import { SELLER_ROLE_LABEL } from '../../utils/labels';
import type { PayoutDetail } from '../../types/models';
import { PayoutStatusPill } from './PayoutPills';
import { PayPayoutModal } from './PayPayoutModal';

type Prompt = ReasonText & { run: (note: string) => Promise<unknown>; success: string };

/** A payout: approval, payment confirmation, evidence, reversal and history. */
export function PayoutDetailPage() {
  const { id } = useParams();
  const { session } = useAuth();
  const { data: p, error, loading, reload } = useLoad(() => payoutsApi.get(id!), [id]);
  const action = useAction();
  const [prompt, setPrompt] = useState<Prompt | null>(null);
  const [paying, setPaying] = useState(false);

  if (loading && !p) return <Loading />;
  if (!p) return <ErrorNote error={error} retry={reload} />;
  const me = session!.user.id;
  const own = p.requested_by === me, ownReversal = p.reversal_requested_by === me;
  const approving = p.status === 'PENDING_APPROVAL', toPay = p.status === 'APPROVED', paid = p.status === 'PAID', reversing = p.status === 'REVERSAL_PENDING';
  const mobile = p.method !== 'CASH';

  async function submitPrompt(note: string) {
    if (prompt && await action.run(() => prompt.run(note), prompt.success) !== undefined) { setPrompt(null); reload(); }
  }
  const close = () => { setPrompt(null); action.setError(''); };
  const ask = (x: Prompt) => setPrompt(x);

  return (
    <>
      <PageHeader title={p.reference} badge={<PayoutStatusPill status={p.status} />} subtitle={`Recorded ${dateTime(p.requested_at)} by ${p.requested_by_name}`}
        crumbs={[['Payouts', '/payouts'], [p.reference]]}
        actions={<>
          {approving && <>
            <button className="btn" disabled={own} title={own ? 'Another administrator must decide a payout you recorded.' : undefined}
              onClick={() => ask({ title: 'Reject payout', label: 'Reason for rejection', confirm: 'Reject Payout', success: 'Payout rejected.', run: n => payoutsApi.reject(p.id, n) })}><X size={16} />Reject</button>
            <button className="btn gold" disabled={own} title={own ? 'Another administrator must approve a payout you recorded.' : undefined}
              onClick={() => ask({ title: 'Approve payout', label: 'Note (optional)', confirm: 'Approve Payout', required: false, success: 'Payout approved. Mark it as paid once the money is handed over.', run: n => payoutsApi.approve(p.id, n || undefined) })}><Check size={16} />Approve</button>
          </>}
          {toPay && <button className="btn gold" onClick={() => setPaying(true)}><HandCoins size={16} />Mark as Paid</button>}
          {paid && <button className="btn" onClick={() => ask({ title: 'Request reversal', label: 'Why must this payout be corrected?', confirm: 'Request Reversal',
            success: p.requires_approval ? 'Reversal requested. Another administrator must approve it.' : 'Payout reversed. The amount is payable again.', run: n => payoutsApi.requestReversal(p.id, n) })}><Undo2 size={16} />Reverse Payout</button>}
          {reversing && <>
            <button className="btn" disabled={ownReversal} title={ownReversal ? 'Another administrator must decide a reversal you requested.' : undefined}
              onClick={() => ask({ title: 'Keep payout', label: 'Why is the reversal refused?', confirm: 'Keep Payout', success: 'Reversal refused; the payout stays paid.', run: n => payoutsApi.rejectReversal(p.id, n) })}><X size={16} />Refuse Reversal</button>
            <button className="btn gold" disabled={ownReversal} title={ownReversal ? 'Another administrator must approve a reversal you requested.' : undefined}
              onClick={() => ask({ title: 'Approve reversal', label: 'Note (optional)', confirm: 'Reverse Payout', required: false, success: 'Payout reversed. The amount is payable again.', run: n => payoutsApi.approveReversal(p.id, n || undefined) })}><Check size={16} />Approve Reversal</button>
          </>}
          {(approving || toPay) && <button className="btn danger" onClick={() => ask({ title: 'Cancel payout', label: 'Why is this payout cancelled? (optional)', confirm: 'Cancel Payout', required: false, success: 'Payout cancelled. The amount is payable again.', run: n => payoutsApi.cancel(p.id, n || undefined) })}><Ban size={16} />Cancel</button>}
        </>} />
      <ErrorNote error={action.error && !prompt ? action.error : ''} />
      {approving && own && <Note>You recorded this payout, so another administrator must approve or reject it.</Note>}
      {toPay && <Note tone="warn">Approved, not paid yet. Pay the seller outside LUTIANO, then mark it as paid{mobile ? ' with the provider reference' : ''}.</Note>}
      {reversing && <Note tone="warn">A reversal was requested{p.reversal_requested_by_name ? ` by ${p.reversal_requested_by_name}` : ''}: “{p.reversal_reason}”. {ownReversal ? 'Another administrator must decide it.' : 'Approve it to make the amount payable again.'}</Note>}

      <div className="grid-2">
        <Panel title="Payout Information" icon={<Info size={19} />}>
          <Facts items={[
            ['Amount', <b>{xaf(p.amount_xaf)}</b>], ['Method', PAYOUT_METHOD[p.method]],
            ['Earning period', periodLabel(p.period_starts_on, p.period_ends_on)], ['Note', p.note ?? '—'],
            ['Approval', p.requires_approval ? 'Required (above the approval threshold)' : 'Not required (at or below the threshold)'],
            ['Decision', p.decided_at ? `${p.status === 'REJECTED' ? 'Rejected' : 'Approved'} ${dateTime(p.decided_at)} by ${p.decided_by_name}${p.decision_note ? `: ${p.decision_note}` : ''}` : approving ? 'Waiting for approval' : '—'],
          ]} />
        </Panel>
        <Panel title="Seller" icon={<User size={19} />}>
          <Facts items={[
            ['Seller', <Link className="link" to={`/users/${p.seller_id}?tab=earnings`}>{p.seller_name}</Link>], ['Role', p.seller_role ? SELLER_ROLE_LABEL[p.seller_role] : '—'],
            ...(mobile ? [['Paid to', `${p.destination_name} · ${p.destination_phone}`], ['Provider reference', p.provider_reference ?? 'Not paid yet']] as [string, string][] : []),
            ['Paid', p.paid_at ? `${dateTime(p.paid_at)} by ${p.paid_by_name}${p.payment_note ? `: ${p.payment_note}` : ''}` : 'Not paid yet'],
            ...(p.reversed_at ? [['Reversed', `${dateTime(p.reversed_at)} by ${p.reversed_by_name}: ${p.reversal_reason}`]] as [string, string][] : []),
          ]} />
        </Panel>
      </div>

      <div className="grid-2">
        <EvidencePanel payout={p} onChanged={reload} />
        <Panel title="History" icon={<History size={19} />}>
          <ul className="timeline compact">{p.events.map(e => (
            <li key={e.id} className={(PAYOUT_EVENT[e.event_type] ?? [e.event_type, ''])[1]}>
              <span className="when">{dateTime(e.created_at)}</span>
              <div><b>{(PAYOUT_EVENT[e.event_type] ?? [e.event_type])[0]}</b><small>{[e.actor_name, e.details].filter(Boolean).join(' · ')}</small></div>
            </li>
          ))}</ul>
        </Panel>
      </div>

      {prompt && <ReasonModal prompt={prompt} busy={action.busy} error={action.error} onSubmit={submitPrompt} onClose={close} />}
      {paying && <PayPayoutModal payout={p} onClose={() => setPaying(false)} onDone={() => { setPaying(false); reload(); }} />}
    </>
  );
}

function EvidencePanel({ payout: p, onChanged }: { payout: PayoutDetail; onChanged: () => void }) {
  const input = useRef<HTMLInputElement>(null);
  const action = useAction();
  const closed = p.status === 'REJECTED' || p.status === 'CANCELLED';
  const upload = async (file?: File) => {
    if (file && await action.run(() => payoutsApi.addEvidence(p.id, file), 'Evidence added.') !== undefined) onChanged();
    if (input.current) input.current.value = '';
  };
  return (
    <Panel title={`Evidence (${p.evidence.length})`} icon={<Paperclip size={19} />}
      action={!closed && <button className="btn sm" disabled={action.busy} onClick={() => input.current?.click()}><Upload size={15} />{action.busy ? 'Uploading…' : 'Add file'}</button>}>
      <input ref={input} type="file" hidden accept="application/pdf,image/jpeg,image/png,image/webp" onChange={e => upload(e.target.files?.[0])} />
      <ErrorNote error={action.error} />
      {p.evidence.length === 0 ? <p className="muted">Transfer confirmation screenshot or signed receipt (PDF, JPEG, PNG or WebP, up to 10 MB).</p> : (
        <ul className="check-list">{p.evidence.map(f => (
          <li key={f.id}>
            <span><FileText size={15} aria-hidden="true" /> {f.file_name}<small className="block">{dateTime(f.uploaded_at)}{f.uploaded_by_name ? ` · ${f.uploaded_by_name}` : ''}</small></span>
            <button className="btn sm" onClick={() => action.run(async () => saveFile(await payoutsApi.evidenceFile(p.id, f.id), f.file_name))}>Download</button>
          </li>
        ))}</ul>
      )}
    </Panel>
  );
}
