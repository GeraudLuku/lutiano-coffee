import { useState } from 'react';
import { salesApi, refundsApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { useLoad } from '../../hooks/useLoad';
import { ChoiceTiles, Field, FormGrid, FormModal, Note } from '../../components/ui';
import { dateTime, xaf } from '../../utils/format';
import { FUND_SOURCE } from '../../utils/refunds';
import type { RefundDetail } from '../../types/models';

/**
 * Records a cash refund paid from an open cash shift (it must hold enough cash) or from a documented authorised
 * advance (unused cash is returned). The recipient must acknowledge receipt. Without a fund the refund stays approved but unpaid.
 */
export function PayRefundModal({ refund, onClose, onDone }: { refund: RefundDetail; onClose: () => void; onDone: () => void }) {
  const amount = Number(refund.amount_xaf);
  const shifts = useLoad(() => salesApi.openShifts(), []);
  const [source, setSource] = useState<'SHIFT' | 'ADVANCE'>('SHIFT');
  const [shiftId, setShiftId] = useState('');
  const [advanceReference, setAdvanceReference] = useState('');
  const [advance, setAdvance] = useState('');
  const [recipient, setRecipient] = useState(refund.customer_name ?? '');
  const [ack, setAck] = useState(false);
  const [note, setNote] = useState('');
  const { busy, error, run } = useAction();

  const open = shifts.data ?? [];
  const funded = open.filter(s => Number(s.expected_cash) >= amount);
  const advanceXaf = Number(advance) || 0;
  const unused = advanceXaf - amount;

  async function submit() {
    const ok = await run(() => refundsApi.pay(refund.id, {
      fundSource: source, recipientName: recipient, recipientAcknowledged: ack, note: note || undefined,
      ...(source === 'SHIFT' ? { shiftId } : { advanceReference, advanceAmountXaf: advanceXaf, advanceReturnedXaf: Math.max(unused, 0) }),
    }), `Refund ${refund.reference} paid.`);
    if (ok) onDone();
  }

  return (
    <FormModal title={`Record cash refund · ${xaf(amount)}`} submitLabel="Record Payment" busy={busy} error={error} onSubmit={submit} onClose={onClose}>
      <Field label="Refund fund" required group>
        <ChoiceTiles name="fund" value={source} onChange={setSource} options={[
          { value: 'SHIFT', label: FUND_SOURCE.SHIFT, description: 'Cash handed out of a seller’s open cash shift. Reduces that shift’s expected cash.' },
          { value: 'ADVANCE', label: FUND_SOURCE.ADVANCE, description: 'Cash advanced for this refund under a documented authorisation. Unused cash is returned.' },
        ]} />
      </Field>
      {source === 'SHIFT' ? (
        <Field label="Cash shift" required hint={shifts.loading ? 'Loading open shifts…' : undefined}>
          <select className="input" value={shiftId} onChange={e => setShiftId(e.target.value)} required>
            <option value="">Choose an open shift…</option>
            {open.map(s => (
              <option key={s.id} value={s.id} disabled={Number(s.expected_cash) < amount}>
                {s.seller_name} · opened {dateTime(s.opened_at)} · {xaf(s.expected_cash)} in drawer{Number(s.expected_cash) < amount ? ' (not enough)' : ''}
              </option>
            ))}
          </select>
        </Field>
      ) : (
        <FormGrid>
          <Field label="Advance reference" required hint="Authorisation or voucher number">
            <input className="input" value={advanceReference} onChange={e => setAdvanceReference(e.target.value)} required maxLength={80} />
          </Field>
          <Field label="Advance amount (XAF)" required hint={advanceXaf > 0 ? (unused >= 0 ? `Unused cash to return: ${xaf(unused)}` : 'The advance must cover the refund.') : undefined}>
            <input className="input" type="number" min={amount} step={1} value={advance} onChange={e => setAdvance(e.target.value)} required />
          </Field>
        </FormGrid>
      )}
      {source === 'SHIFT' && !shifts.loading && funded.length === 0 && (
        <Note tone="warn">No open cash shift holds {xaf(amount)}. Use an authorised advance, or leave the refund approved but unpaid until cash is available.</Note>
      )}
      <Field label="Cash handed to (recipient)" required><input className="input" value={recipient} onChange={e => setRecipient(e.target.value)} required maxLength={150} /></Field>
      <label className="check-row">
        <input type="checkbox" checked={ack} onChange={e => setAck(e.target.checked)} required />
        <span>{recipient || 'The recipient'} confirms receiving {xaf(amount)} in cash{source === 'ADVANCE' && unused > 0 ? `, and ${xaf(unused)} of unused advance was returned` : ''}.</span>
      </label>
      <Field label="Note"><textarea className="input" rows={2} value={note} onChange={e => setNote(e.target.value)} maxLength={500} /></Field>
      <Note>Add a signed slip or photo as evidence on the refund after recording the payment.</Note>
    </FormModal>
  );
}
