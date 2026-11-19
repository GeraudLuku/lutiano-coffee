import { useState } from 'react';
import { loyaltyApi, productsApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { useLoad } from '../../hooks/useLoad';
import { Field, FormGrid, FormModal, Note, Switch } from '../../components/ui';
import { REWARD_TYPE } from '../../utils/loyalty';
import type { Reward, RewardType } from '../../types/models';

export function RewardModal({ reward, onClose, onDone }: { reward?: Reward; onClose: () => void; onDone: () => void }) {
  const products = useLoad(() => productsApi.all('sellable'), []);
  const [name, setName] = useState(reward?.name ?? '');
  const [type, setType] = useState<RewardType>(reward?.reward_type ?? 'FREE_PRODUCT');
  const [description, setDescription] = useState(reward?.description ?? '');
  const [cost, setCost] = useState(String(reward?.point_cost ?? ''));
  const [productId, setProductId] = useState(reward?.product_id ?? '');
  const [requiresStock, setRequiresStock] = useState(reward?.requires_stock ?? true);
  const [discount, setDiscount] = useState(String(reward?.discount_pct ?? 10));
  const [minPurchases, setMinPurchases] = useState(String(reward?.min_purchases ?? 0));
  const [minSpend, setMinSpend] = useState(String(reward?.min_spend_xaf ?? 0));
  const [active, setActive] = useState(reward?.active ?? true);
  const { busy, error, run } = useAction();
  const voucher = type === 'DISCOUNT_VOUCHER';
  const product = (products.data ?? []).find(p => p.id === productId);

  async function submit() {
    const body = {
      name, rewardType: type, description, pointCost: Number(cost), productId: voucher ? null : productId || null,
      requiresStock: !voucher && !!productId && requiresStock, discountPct: voucher ? Number(discount) : null,
      minPurchases: Number(minPurchases) || 0, minSpendXaf: Number(minSpend) || 0, active,
    };
    const ok = await run(() => (reward ? loyaltyApi.updateReward(reward.id, body) : loyaltyApi.createReward(body)), reward ? 'Reward updated.' : 'Reward created.');
    if (ok) onDone();
  }

  return (
    <FormModal title={reward ? 'Edit reward' : 'Create reward'} submitLabel={reward ? 'Save Reward' : 'Create Reward'} busy={busy} error={error} onSubmit={submit} onClose={onClose}>
      <FormGrid>
        <Field label="Reward name" required><input className="input" value={name} onChange={e => setName(e.target.value)} required maxLength={100} placeholder="e.g. Free Cappuccino (250ml)" /></Field>
        <Field label="Type" required>
          <select className="input" value={type} onChange={e => setType(e.target.value as RewardType)}>
            {(Object.keys(REWARD_TYPE) as RewardType[]).map(t => <option key={t} value={t}>{REWARD_TYPE[t][0]}</option>)}
          </select>
        </Field>
        <Field label="Point cost" required><input className="input" type="number" min={1} value={cost} onChange={e => setCost(e.target.value)} required /></Field>
        {voucher
          ? <Field label="Discount (%)" required><input className="input" type="number" min={1} max={100} value={discount} onChange={e => setDiscount(e.target.value)} required /></Field>
          : (
            <Field label="Product given" required={type !== 'MERCHANDISE'}>
              <select className="input" value={productId} onChange={e => setProductId(e.target.value)} required={type !== 'MERCHANDISE'}>
                <option value="">{type === 'MERCHANDISE' ? 'Not a catalogue product' : 'Choose…'}</option>
                {(products.data ?? []).filter(p => p.kind !== 'PARENT').map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
              </select>
            </Field>
          )}
      </FormGrid>
      {!voucher && productId && (
        <Field label="Stock" group hint={product && !product.track_stock ? 'This product is not stock-tracked.' : 'One unit leaves stock when the reward is redeemed (reward stock).'}>
          <Switch checked={requiresStock && !!product?.track_stock} disabled={!product?.track_stock} onChange={setRequiresStock} label="Deduct from stock on redemption" />
        </Field>
      )}
      <Field label="Description"><textarea className="input" rows={2} value={description} onChange={e => setDescription(e.target.value)} maxLength={500} /></Field>
      <FormGrid>
        <Field label="Minimum purchases" hint="Completed purchases needed first"><input className="input" type="number" min={0} value={minPurchases} onChange={e => setMinPurchases(e.target.value)} /></Field>
        <Field label="Minimum lifetime spend (XAF)"><input className="input" type="number" min={0} value={minSpend} onChange={e => setMinSpend(e.target.value)} /></Field>
      </FormGrid>
      <Field label="Status" group><Switch checked={active} onChange={setActive} label={active ? 'Active: customers can redeem it' : 'Inactive'} /></Field>
      {voucher && <Note>Discount vouchers are recorded as redeemed now; applying the discount at checkout comes with the promotions work.</Note>}
    </FormModal>
  );
}
