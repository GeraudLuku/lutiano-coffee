import { useState } from 'react';
import { loyaltyApi, operationsApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { useLoad } from '../../hooks/useLoad';
import { Field, FormModal, Note } from '../../components/ui';
import { pts } from '../../utils/loyalty';
import type { CustomerDetail } from '../../types/models';

/** Redeems a reward for a member after they explicitly confirm. The backend rechecks balance, conditions and stock. */
export function RedeemModal({ customer: c, onClose, onDone }: { customer: CustomerDetail; onClose: () => void; onDone: () => void }) {
  const rewards = useLoad(() => loyaltyApi.rewards(), []);
  const ops = useLoad(() => operationsApi.all(), []);
  const [rewardId, setRewardId] = useState('');
  const [operationId, setOperationId] = useState('');
  const [confirmed, setConfirmed] = useState(false);
  const { busy, error, run } = useAction();
  const active = (rewards.data ?? []).filter(r => r.active);
  const reward = active.find(r => r.id === rewardId);

  async function submit() {
    const ok = await run(() => loyaltyApi.redeem({ customerId: c.id, rewardId, operationId: operationId || null, customerConfirmed: confirmed }),
      `${reward?.name ?? 'Reward'} redeemed.`);
    if (ok) onDone();
  }

  return (
    <FormModal title={`Redeem a reward · ${pts(c.points_balance)} available`} submitLabel="Redeem" busy={busy} error={error} onSubmit={submit} onClose={onClose}>
      <Field label="Reward" required>
        <select className="input" value={rewardId} onChange={e => setRewardId(e.target.value)} required>
          <option value="">Choose a reward…</option>
          {active.map(r => <option key={r.id} value={r.id} disabled={r.point_cost > c.points_balance}>{r.name} — {pts(r.point_cost)}{r.point_cost > c.points_balance ? ' (not enough points)' : ''}</option>)}
        </select>
      </Field>
      {reward?.requires_stock && (
        <Field label="Take the item from" required hint="One unit is deducted from this location's stock.">
          <select className="input" value={operationId} onChange={e => setOperationId(e.target.value)}>
            <option value="">Central stock</option>{(ops.data ?? []).filter(o => o.active).map(o => <option key={o.id} value={o.id}>{o.name} ({o.code})</option>)}
          </select>
        </Field>
      )}
      {reward && <Note>{pts(reward.point_cost)} will be spent from the earliest-expiring points. Balance after: {pts(c.points_balance - reward.point_cost)}.</Note>}
      <label className="check-row">
        <input type="checkbox" checked={confirmed} onChange={e => setConfirmed(e.target.checked)} required />
        <span>{c.full_name} confirmed they want to spend {reward ? pts(reward.point_cost) : 'these points'} on this reward.</span>
      </label>
    </FormModal>
  );
}
