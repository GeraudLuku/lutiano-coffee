import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { payoutsApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { useLoad } from '../../hooks/useLoad';
import { fetchAll } from '../../api/pages';
import { ChoiceTiles, Field, FormGrid, FormModal, Note } from '../../components/ui';
import { xaf } from '../../utils/format';
import { PAYOUT_METHOD, periodLabel } from '../../utils/payouts';
import type { PayoutMethod } from '../../types/models';

/**
 * Records a payout the company makes (or made) outside LUTIANO: against one finalised period of the seller, for all or
 * part of what is left. Mobile money needs the seller's verified number. Above the approval threshold another
 * administrator approves it before it can be paid.
 */
export function RecordPayoutModal({ sellerId: preset, onClose, onDone }: { sellerId?: string; onClose: () => void; onDone: () => void }) {
  const nav = useNavigate();
  const sellers = useLoad(() => (preset ? Promise.resolve([]) : fetchAll((page, size) => payoutsApi.balances({ owedOnly: true, page, size }))), [preset]);
  const [sellerId, setSellerId] = useState(preset ?? '');
  const seller = useLoad(() => (sellerId ? payoutsApi.balance(sellerId) : Promise.resolve(null)), [sellerId]);
  const periods = useLoad(() => (sellerId ? fetchAll((page, size) => payoutsApi.periods(sellerId, { openOnly: true, page, size })) : Promise.resolve([])), [sellerId]);
  const settings = useLoad(() => payoutsApi.settings(), []);
  const [periodId, setPeriodId] = useState('');
  const [amount, setAmount] = useState('');
  const [method, setMethod] = useState<PayoutMethod>('CASH');
  const [note, setNote] = useState('');
  const { busy, error, setError, run } = useAction();

  const open = periods.data ?? [];
  const period = open.find(p => p.period_id === periodId);
  // Open on the oldest period with something left, paying all of it unless changed.
  useEffect(() => {
    const oldest = [...open].sort((a, b) => a.starts_on.localeCompare(b.starts_on))[0];
    setPeriodId(oldest?.period_id ?? '');
  }, [open.length, sellerId]); // eslint-disable-line react-hooks/exhaustive-deps
  useEffect(() => { setAmount(period ? String(period.remaining_xaf) : ''); }, [period?.period_id]); // eslint-disable-line react-hooks/exhaustive-deps

  const verified = (seller.data?.destinations ?? []).find(d => d.provider === method && d.status === 'VERIFIED');
  const pending = (seller.data?.destinations ?? []).find(d => d.provider === method && d.status === 'PENDING');
  const value = Number(amount);
  const threshold = settings.data?.approvalThresholdXaf ?? 0;
  const needsApproval = value > threshold;
  const blocked = method !== 'CASH' && !verified;

  async function submit() {
    if (!period) { setError('Choose a period.'); return; }
    if (blocked) { setError(`The seller needs a verified ${PAYOUT_METHOD[method]} number first.`); return; }
    if (!Number.isInteger(value) || value <= 0) { setError('Enter a whole amount greater than zero.'); return; }
    if (value > period.remaining_xaf) { setError(`At most ${xaf(period.remaining_xaf)} is left to pay for this period.`); return; }
    const saved = await run(() => payoutsApi.record({ sellerId, periodId, amountXaf: value, method, note: note || undefined }),
      needsApproval ? 'Payout recorded. Another administrator must approve it.' : 'Payout recorded.');
    if (saved) { onDone(); nav(`/payouts/${saved.id}`); }
  }

  return (
    <FormModal title="Record payout" submitLabel="Record Payout" busy={busy} error={error} onSubmit={submit} onClose={onClose}>
      {!preset && (
        <Field label="Seller" required>
          <select className="input" value={sellerId} onChange={e => setSellerId(e.target.value)} required>
            <option value="">Choose a seller…</option>
            {(sellers.data ?? []).map(s => <option key={s.seller_id} value={s.seller_id}>{s.full_name} · {xaf(s.outstanding_xaf)} owed</option>)}
          </select>
        </Field>
      )}
      {preset && seller.data && <Note>{seller.data.full_name} is owed <b>{xaf(seller.data.outstanding_xaf)}</b> in total.</Note>}
      {sellerId && (
        <>
          <Field label="Earning period" required hint={periods.loading ? 'Loading periods…' : open.length ? 'Only finalised periods with an amount left appear here.' : 'Nothing is left to pay for this seller.'}>
            <select className="input" value={periodId} onChange={e => setPeriodId(e.target.value)} required>
              {open.map(p => <option key={p.period_id} value={p.period_id}>{periodLabel(p.starts_on, p.ends_on)} · {xaf(p.remaining_xaf)} left of {xaf(p.payable_xaf)}</option>)}
            </select>
          </Field>
          <FormGrid>
            <Field label="Amount (XAF)" required hint={period ? `Up to ${xaf(period.remaining_xaf)}. Pay part now and the rest later if needed.` : undefined}>
              <input className="input" type="number" min={1} max={period?.remaining_xaf} step={1} value={amount} onChange={e => setAmount(e.target.value)} required />
            </Field>
          </FormGrid>
          <Field label="Paid by" required group>
            <ChoiceTiles name="method" value={method} onChange={setMethod} columns={3} options={(Object.keys(PAYOUT_METHOD) as PayoutMethod[]).map(m => ({ value: m, label: PAYOUT_METHOD[m], description: m === 'CASH' ? 'Handed over in person.' : 'Sent to their verified number.' }))} />
          </Field>
          {method !== 'CASH' && (verified
            ? <Note tone="ok">Will be sent to {verified.account_name} · {verified.phone} (verified).</Note>
            : <Note tone="warn">{pending
              ? `The seller's ${PAYOUT_METHOD[method]} number ${pending.phone} is awaiting verification. Another administrator must verify it first (Payouts → Payout Numbers).`
              : `The seller has no ${PAYOUT_METHOD[method]} number yet. Add and verify one under Payouts → Payout Numbers.`}</Note>)}
          <Field label="Note"><input className="input" value={note} onChange={e => setNote(e.target.value)} maxLength={500} placeholder="Optional" /></Field>
          {period && value > 0 && (needsApproval
            ? <Note>{threshold === 0 ? 'Every payout needs approval:' : `This payout is above the ${xaf(threshold)} approval threshold, so`} another administrator must approve it before it is paid.</Note>
            : <Note tone="ok">At or below the {xaf(threshold)} approval threshold: no second approval is needed.</Note>)}
        </>
      )}
    </FormModal>
  );
}
