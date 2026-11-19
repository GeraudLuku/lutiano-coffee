import { useEffect, useState } from 'react';
import { ShieldCheck } from 'lucide-react';
import { payoutsApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { useLoad } from '../../hooks/useLoad';
import { ErrorNote, Field, Loading, Note, Panel } from '../../components/ui';
import { xaf } from '../../utils/format';

/** The payout approval rule (maker-checker). */
export function SettingsTab({ onChanged }: { onChanged: () => void }) {
  const { data, error, loading, reload } = useLoad(() => payoutsApi.settings(), []);
  const [value, setValue] = useState('');
  const { busy, error: saveError, setError, run } = useAction();
  useEffect(() => { if (data) setValue(String(data.approvalThresholdXaf)); }, [data]);
  async function save() {
    const n = Number(value);
    if (!Number.isInteger(n) || n < 0 || value === '') { setError('Enter a whole number of FCFA, zero or more.'); return; }
    if (await run(() => payoutsApi.setThreshold(n), 'Approval threshold saved.')) { reload(); onChanged(); }
  }
  if (loading && !data) return <Loading />;
  const n = Number(value);
  return (
    <Panel title="Approval rule" icon={<ShieldCheck size={19} />}>
      <ErrorNote error={error || saveError} retry={reload} />
      <Note>A payout above this amount must be approved by an administrator other than the one who recorded it, and so must the reversal of such a payout.
        At or below it, the payout needs no second approver. <b>0 means every payout needs one</b>, which is recommended for the pilot.</Note>
      <form className="settings-form" onSubmit={e => { e.preventDefault(); save(); }}>
        <Field label="Approval threshold (XAF)" hint={Number.isFinite(n) && value !== '' ? (n === 0 ? 'Every payout needs a second approver.' : `Payouts above ${xaf(n)} need a second approver.`) : undefined}>
          <input className="input" type="number" min={0} step={1000} value={value} onChange={e => setValue(e.target.value)} required />
        </Field>
        <button className="btn gold" disabled={busy || value === String(data?.approvalThresholdXaf)}>{busy ? 'Saving…' : 'Save'}</button>
      </form>
    </Panel>
  );
}
