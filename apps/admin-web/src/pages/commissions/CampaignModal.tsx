import { useState } from 'react';
import { Plus, Trash2 } from 'lucide-react';
import { campaignsApi, operationsApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { useLoad } from '../../hooks/useLoad';
import { Field, FormGrid, FormModal, Note } from '../../components/ui';
import { doualaDate } from '../../utils/format';

interface TierRow { threshold: string; bonus: string }

/** Sets up a campaign: its days, who it reaches, and the tiers (sales reached, bonus paid). */
export function CampaignModal({ onClose, onDone }: { onClose: () => void; onDone: () => void }) {
  const locations = useLoad(() => operationsApi.all(), []);
  const [f, setF] = useState({ name: '', description: '', startsOn: doualaDate(), endsOn: doualaDate(30), operationId: '' });
  const [tiers, setTiers] = useState<TierRow[]>([{ threshold: '', bonus: '' }]);
  const { busy, error, run } = useAction();
  const set = (k: keyof typeof f, v: string) => setF(p => ({ ...p, [k]: v }));
  const setTier = (i: number, k: keyof TierRow, v: string) => setTiers(t => t.map((x, j) => (j === i ? { ...x, [k]: v } : x)));

  async function submit() {
    const body = { ...f, operationId: f.operationId || null, tiers: tiers.map(t => ({ thresholdXaf: Number(t.threshold), bonusXaf: Number(t.bonus) })) };
    if (await run(() => campaignsApi.create(body), 'Campaign created.')) onDone();
  }

  return (
    <FormModal title="New sales campaign" submitLabel="Create Campaign" busy={busy} error={error} onSubmit={submit} onClose={onClose}>
      <Field label="Name" required><input className="input" value={f.name} onChange={e => set('name', e.target.value)} required maxLength={100} placeholder="e.g. December push" /></Field>
      <Field label="Description"><input className="input" value={f.description} onChange={e => set('description', e.target.value)} maxLength={500} /></Field>
      <FormGrid>
        <Field label="First day" required><input className="input" type="date" value={f.startsOn} onChange={e => set('startsOn', e.target.value)} required /></Field>
        <Field label="Last day" required><input className="input" type="date" value={f.endsOn} min={f.startsOn} onChange={e => set('endsOn', e.target.value)} required /></Field>
      </FormGrid>
      <Field label="Who takes part" hint="Sellers assigned to the location, or every seller">
        <select className="input" value={f.operationId} onChange={e => set('operationId', e.target.value)}>
          <option value="">All sellers</option>{(locations.data ?? []).map(o => <option key={o.id} value={o.id}>{o.name}</option>)}
        </select>
      </Field>
      <fieldset className="stack">
        <legend>Tiers</legend>
        {tiers.map((t, i) => (
          <FormGrid key={i}>
            <Field label={`Tier ${i + 1}: net sales reached (XAF)`} required><input className="input" type="number" min={1} value={t.threshold} onChange={e => setTier(i, 'threshold', e.target.value)} required /></Field>
            <Field label="Bonus (XAF)" required>
              <div className="input-row">
                <input className="input" type="number" min={1} value={t.bonus} onChange={e => setTier(i, 'bonus', e.target.value)} required />
                {tiers.length > 1 && <button type="button" className="icon-btn danger" aria-label={`Remove tier ${i + 1}`} onClick={() => setTiers(x => x.filter((_, j) => j !== i))}><Trash2 size={15} /></button>}
              </div>
            </Field>
          </FormGrid>
        ))}
        {tiers.length < 10 && <button type="button" className="btn sm" onClick={() => setTiers(x => [...x, { threshold: '', bonus: '' }])}><Plus size={15} />Add tier</button>}
      </fieldset>
      <Note>A seller gets the bonus of the highest tier they reach, not the sum. Net sales are paid sales less refunds. Bonuses are paid with the earnings after the last day, when you choose Pay bonuses.</Note>
    </FormModal>
  );
}
