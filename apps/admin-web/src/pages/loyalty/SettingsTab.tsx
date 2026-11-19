import { useEffect, useState } from 'react';
import { CalendarClock, Coins, Power, Timer } from 'lucide-react';
import { loyaltyApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { useLoad } from '../../hooks/useLoad';
import { ErrorNote, Field, FormGrid, Loading, Note, Panel, Switch, toast } from '../../components/ui';
import { dateTime, num, xaf } from '../../utils/format';

/** Design 8.2 Point Settings, Expiry & Validity and Program Settings. */
export function SettingsTab() {
  const { data: s, error, loading, reload } = useLoad(() => loyaltyApi.settings(), []);
  const [f, setF] = useState({ programActive: true, basePoints: '', baseAmountXaf: '', pointValueXaf: '', expiryMonths: '', expiryNoticeDays: '', minRedeemPoints: '' });
  const save = useAction();
  const expire = useAction();
  useEffect(() => {
    if (s) setF({ programActive: !!s.program_active, basePoints: String(s.base_points), baseAmountXaf: String(s.base_amount_xaf), pointValueXaf: String(s.point_value_xaf),
      expiryMonths: String(s.expiry_months), expiryNoticeDays: String(s.expiry_notice_days), minRedeemPoints: String(s.min_redeem_points) });
  }, [s]);
  if (loading && !s) return <Loading />;
  const set = (k: keyof typeof f) => (e: React.ChangeEvent<HTMLInputElement>) => setF(prev => ({ ...prev, [k]: e.target.value }));

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    if (await save.run(() => loyaltyApi.saveSettings({
      programActive: f.programActive, basePoints: Number(f.basePoints), baseAmountXaf: Number(f.baseAmountXaf), pointValueXaf: Number(f.pointValueXaf),
      expiryMonths: Number(f.expiryMonths), expiryNoticeDays: Number(f.expiryNoticeDays), minRedeemPoints: Number(f.minRedeemPoints),
    }), 'Loyalty settings saved.')) reload();
  }

  return (
    <form onSubmit={submit}>
      <ErrorNote error={error || save.error} retry={reload} />
      <div className="grid-2">
        <Panel title="Program" icon={<Power size={19} />}>
          <Switch checked={f.programActive} onChange={v => setF(p => ({ ...p, programActive: v }))} label={f.programActive ? 'Programme active: members earn points' : 'Programme paused: no points are earned'} />
          <p className="muted small">Pausing stops earning on new sales. Balances, redemptions and expiry continue.</p>
          {s?.updated_at && <p className="muted small">Last changed {dateTime(s.updated_at)}</p>}
        </Panel>
        <Panel title="Point Settings" icon={<Coins size={19} />}>
          <FormGrid>
            <Field label="Default points" required><input className="input" type="number" min={0} value={f.basePoints} onChange={set('basePoints')} required /></Field>
            <Field label="Per amount spent (XAF)" required><input className="input" type="number" min={1} value={f.baseAmountXaf} onChange={set('baseAmountXaf')} required /></Field>
            <Field label="Value of 1 point (XAF)" required hint="Used to show the programme liability"><input className="input" type="number" min={0} value={f.pointValueXaf} onChange={set('pointValueXaf')} required /></Field>
            <Field label="Minimum balance to redeem (pts)" required><input className="input" type="number" min={0} value={f.minRedeemPoints} onChange={set('minRedeemPoints')} required /></Field>
          </FormGrid>
          <Note>Default rule: {num(Number(f.basePoints))} pts per {xaf(Number(f.baseAmountXaf) || 0)} of eligible paid value.</Note>
        </Panel>
        <Panel title="Expiry & Validity" icon={<Timer size={19} />}>
          <FormGrid>
            <Field label="Points expire after (months)" required><input className="input" type="number" min={1} max={120} value={f.expiryMonths} onChange={set('expiryMonths')} required /></Field>
            <Field label="Expiry reminder (days before)" required hint="Members see points expiring in this window"><input className="input" type="number" min={0} max={365} value={f.expiryNoticeDays} onChange={set('expiryNoticeDays')} required /></Field>
          </FormGrid>
          <p className="muted small">Each purchase’s points expire on their own date. Expiry runs every night at 01:30 (Douala); new values apply to points earned from now on.</p>
        </Panel>
        <Panel title="Run Expiry Now" icon={<CalendarClock size={19} />}>
          <p className="muted">Expires every batch already past its date (normally done automatically each night).</p>
          <ErrorNote error={expire.error} />
          <button type="button" className="btn" disabled={expire.busy}
            onClick={async () => { const r = await expire.run(() => loyaltyApi.expire()); if (r) toast(r.expiredPoints ? `${num(r.expiredPoints)} pts expired.` : 'Nothing to expire.'); }}>
            {expire.busy ? 'Running…' : 'Run Expiry'}
          </button>
        </Panel>
      </div>
      <div className="form-actions"><button className="btn gold" disabled={save.busy}>{save.busy ? 'Saving…' : 'Save Settings'}</button></div>
    </form>
  );
}
