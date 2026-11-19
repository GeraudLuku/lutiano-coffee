import { useState, type ReactNode } from 'react';
import { Check, HandCoins, X } from 'lucide-react';
import { cashApi } from '../../api/services';
import { useAuth } from '../../auth/AuthContext';
import { useAction } from '../../hooks/useAction';
import { ReasonModal, type ReasonText } from '../../components/ui';
import type { CashOutflow, CashRemittance } from '../../types/models';
import { AcknowledgeModal } from './AcknowledgeModal';

type Prompt = ReasonText & { run: (note: string) => Promise<unknown>; success: string };

/**
 * Row actions shared by the Cash Management tabs and the shift detail: administrators approve or reject outflows;
 * a handover is acknowledged by its named recipient, a bank deposit by an administrator.
 */
export function useCashActions(onChanged: () => void) {
  const { session, can } = useAuth();
  const admin = can('ADMIN');
  const action = useAction();
  const [prompt, setPrompt] = useState<Prompt | null>(null);
  const [ack, setAck] = useState<CashRemittance | null>(null);

  const canAcknowledge = (r: CashRemittance) =>
    r.status === 'OUTSTANDING' && (r.method === 'HANDOVER' ? r.recipient_id === session?.user.id : admin);

  const outflowActions = (o: CashOutflow): ReactNode => (admin && o.status === 'REQUESTED' ? (
    <div className="row-actions">
      <button className="btn sm" onClick={() => setPrompt({ title: `Reject ${o.reference}`, label: 'Reason', confirm: 'Reject', success: 'Outflow rejected.', run: n => cashApi.rejectOutflow(o.id, n) })}><X size={14} />Reject</button>
      <button className="btn sm gold" onClick={() => setPrompt({ title: `Approve ${o.reference}`, label: 'Note (optional)', confirm: 'Approve', required: false,
        success: 'Outflow approved. The cash has left the drawer.', run: n => cashApi.approveOutflow(o.id, n || undefined) })}><Check size={14} />Approve</button>
    </div>
  ) : null);

  const remittanceActions = (r: CashRemittance): ReactNode => (canAcknowledge(r)
    ? <button className="btn sm gold" onClick={() => setAck(r)}><HandCoins size={14} />Acknowledge</button>
    : r.status === 'OUTSTANDING' ? <small className="muted">{r.method === 'HANDOVER' ? `Waiting for ${r.recipient_name}` : 'Waiting for an administrator'}</small> : null);

  const close = () => { setPrompt(null); setAck(null); action.setError(''); };
  const modals = (
    <>
      {prompt && <ReasonModal prompt={prompt} busy={action.busy} error={action.error} onClose={close}
        onSubmit={async note => { if (await action.run(() => prompt.run(note), prompt.success) !== undefined) { setPrompt(null); onChanged(); } }} />}
      {ack && <AcknowledgeModal remittance={ack} onClose={close} onDone={() => { setAck(null); onChanged(); }} />}
    </>
  );
  return { outflowActions, remittanceActions, modals, admin };
}
