import { useState } from 'react';
import { Pencil } from 'lucide-react';
import { loyaltyApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { useLoad } from '../../hooks/useLoad';
import { DataTable, ErrorNote, Field, FormGrid, FormModal, LabelPill, Loading, Panel, Switch } from '../../components/ui';
import { num, xaf } from '../../utils/format';
import type { LoyaltyTier } from '../../types/models';
import { TierBadge } from './TierBadge';

/** Design 8.4 Loyalty Tiers: qualification by spend over the last 12 months; the multiplier applies to base points. */
export function TiersTab() {
  const { data, error, loading, reload } = useLoad(() => loyaltyApi.tiers(), []);
  const [editing, setEditing] = useState<LoyaltyTier | null>(null);
  const total = (data ?? []).reduce((s, t) => s + Number(t.members), 0);
  const next = (i: number) => data?.[i + 1]?.min_annual_spend_xaf;
  return (
    <Panel title="Loyalty Tiers">
      <ErrorNote error={error} retry={reload} />
      {loading && !data ? <Loading /> : (
        <DataTable rows={data ?? []} rowKey={t => t.code} columns={[
          { header: 'Tier', cell: t => <TierBadge name={t.name} color={t.color} /> },
          { header: 'Qualification (12-month spend)', cell: t => { const i = data!.indexOf(t); return next(i) ? `${xaf(t.min_annual_spend_xaf)} – ${xaf(Number(next(i)) - 1)}` : `≥ ${xaf(t.min_annual_spend_xaf)}`; } },
          { header: 'Earning Multiplier', cell: t => <b>{(t.multiplier_pct / 100).toFixed(2).replace(/0$/, '')}×</b> },
          { header: 'Key Benefits', cell: t => <span className="muted small">{t.benefits ?? '—'}</span> },
          { header: 'Members', align: 'right', cell: t => `${num(t.members)}${total ? ` (${Math.round((Number(t.members) / total) * 100)}%)` : ''}` },
          { header: 'Status', cell: t => <LabelPill map={{ Y: ['Active', 'green'], N: ['Inactive', 'grey'] }} value={t.active ? 'Y' : 'N'} /> },
          { header: '', cell: t => <button className="icon-btn" aria-label={`Edit ${t.name}`} onClick={() => setEditing(t)}><Pencil size={15} /></button> },
        ]} />
      )}
      <p className="muted small">Members move between tiers automatically after each purchase, based on their spend over the last 12 months.</p>
      {editing && <TierModal tier={editing} onClose={() => setEditing(null)} onDone={() => { setEditing(null); reload(); }} />}
    </Panel>
  );
}

function TierModal({ tier, onClose, onDone }: { tier: LoyaltyTier; onClose: () => void; onDone: () => void }) {
  const [name, setName] = useState(tier.name);
  const [color, setColor] = useState(tier.color);
  const [minSpend, setMinSpend] = useState(String(tier.min_annual_spend_xaf));
  const [multiplier, setMultiplier] = useState(String(tier.multiplier_pct / 100));
  const [benefits, setBenefits] = useState(tier.benefits ?? '');
  const [active, setActive] = useState(tier.active);
  const { busy, error, run } = useAction();
  const entry = tier.code === 'BRONZE';
  async function submit() {
    const ok = await run(() => loyaltyApi.updateTier(tier.code, { name, color, minAnnualSpendXaf: Number(minSpend), multiplierPct: Math.round(Number(multiplier) * 100), benefits, active }), 'Tier saved.');
    if (ok) onDone();
  }
  return (
    <FormModal title={`Edit ${tier.name} tier`} submitLabel="Save Changes" busy={busy} error={error} onSubmit={submit} onClose={onClose}>
      <FormGrid>
        <Field label="Tier name" required><input className="input" value={name} onChange={e => setName(e.target.value)} required maxLength={40} /></Field>
        <Field label="Tier colour"><input className="input" type="color" value={color} onChange={e => setColor(e.target.value)} /></Field>
        <Field label="Qualifying spend (XAF, last 12 months)" required hint={entry ? 'The entry tier always starts at 0.' : undefined}>
          <input className="input" type="number" min={0} value={minSpend} onChange={e => setMinSpend(e.target.value)} disabled={entry} required />
        </Field>
        <Field label="Earning multiplier" required hint="1.25 = 25% more base points"><input className="input" type="number" min={1} max={10} step={0.05} value={multiplier} onChange={e => setMultiplier(e.target.value)} required /></Field>
      </FormGrid>
      <Field label="Key benefits"><textarea className="input" rows={2} value={benefits} onChange={e => setBenefits(e.target.value)} maxLength={500} /></Field>
      {!entry && <Field label="Status" group><Switch checked={active} onChange={setActive} label={active ? 'Active' : 'Inactive'} /></Field>}
    </FormModal>
  );
}
