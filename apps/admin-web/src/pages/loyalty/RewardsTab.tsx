import { useState } from 'react';
import { Gift, Pencil, Plus } from 'lucide-react';
import { loyaltyApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { useLoad } from '../../hooks/useLoad';
import { ErrorNote, LabelPill, Panel, Switch, ListState } from '../../components/ui';
import { ProductImage } from '../../components/domain';
import { num, xaf } from '../../utils/format';
import { REWARD_TYPE, pts } from '../../utils/loyalty';
import type { Reward } from '../../types/models';
import { RewardModal } from './RewardModal';

/** Design 8.3 Rewards Management: reward cards with an active toggle. */
export function RewardsTab() {
  const { data, error, loading, reload } = useLoad(() => loyaltyApi.rewards(), []);
  const [editing, setEditing] = useState<Reward | 'new' | null>(null);
  const action = useAction();

  const toggle = (r: Reward, active: boolean) => action.run(() => loyaltyApi.updateReward(r.id, {
    name: r.name, rewardType: r.reward_type, description: r.description ?? '', pointCost: r.point_cost, productId: r.product_id, requiresStock: r.requires_stock,
    discountPct: r.discount_pct, minPurchases: r.min_purchases, minSpendXaf: r.min_spend_xaf, active,
  }), active ? 'Reward activated.' : 'Reward deactivated.').then(reload);

  return (
    <Panel title="Rewards" action={<button className="btn gold sm" onClick={() => setEditing('new')}><Plus size={15} />Create Reward</button>}>
      <ErrorNote error={error || action.error} retry={reload} />
      <ListState loading={loading} data={data} items={data ?? []} empty={{ title: 'No rewards yet' }}>
        <div className="reward-grid">
          {(data ?? []).map(r => (
            <article key={r.id} className={`reward-card ${r.active ? '' : 'inactive'}`}>
              <div className="reward-img">{r.image_url ? <ProductImage url={r.image_url} /> : r.reward_type === 'DISCOUNT_VOUCHER' ? <span className="voucher">{r.discount_pct}%</span> : <Gift size={40} aria-hidden="true" />}</div>
              <h4>{r.name}</h4>
              <LabelPill map={REWARD_TYPE} value={r.reward_type} />
              <p className="muted small">
                {[r.product_name, r.requires_stock ? 'uses stock' : null, r.min_purchases ? `${r.min_purchases}+ purchases` : null,
                  r.min_spend_xaf ? `${xaf(r.min_spend_xaf)}+ spent` : null, `${num(r.redemptions)} redeemed`].filter(Boolean).join(' · ')}
              </p>
              <footer>
                <b>{pts(r.point_cost)}</b>
                <Switch checked={r.active} onChange={v => toggle(r, v)} disabled={action.busy} label={r.active ? 'Active' : 'Inactive'} />
                <button className="icon-btn" aria-label={`Edit ${r.name}`} onClick={() => setEditing(r)}><Pencil size={15} /></button>
              </footer>
            </article>
          ))}
        </div>
      </ListState>
      {editing && <RewardModal reward={editing === 'new' ? undefined : editing} onClose={() => setEditing(null)} onDone={() => { setEditing(null); reload(); }} />}
    </Panel>
  );
}
