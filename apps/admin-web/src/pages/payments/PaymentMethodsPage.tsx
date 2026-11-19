import { useEffect, useState } from 'react';
import { Lock } from 'lucide-react';
import { paymentsApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { useLoad } from '../../hooks/useLoad';
import { ErrorNote, Field, Loading, Note, PageHeader, Panel, Pill, Switch } from '../../components/ui';
import { dateTime } from '../../utils/format';
import type { PaymentMethod } from '../../types/models';
import { MethodIcon } from './PaymentBadges';

/** A07 payment methods. Only Cash has an integration today; Mobile Money is listed but greyed out until its provider is connected. */
export function PaymentMethodsPage() {
  const { data, error, loading, reload } = useLoad(() => paymentsApi.methods(), []);
  if (loading && !data) return <Loading />;
  return (
    <>
      <PageHeader title="Payment Methods" subtitle="Choose which payment methods sellers can offer at checkout." crumbs={[['Payments', '/payments'], ['Payment Methods']]} />
      <ErrorNote error={error} retry={reload} />
      <Note>Cash is the only payment method for now. MTN Mobile Money and Orange Money need merchant onboarding and a provider integration before they can be switched on.</Note>
      <Panel>
        <div className="method-list">{(data ?? []).map(m => <MethodCard key={m.code} method={m} onSaved={reload} />)}</div>
      </Panel>
    </>
  );
}

function MethodCard({ method: m, onSaved }: { method: PaymentMethod; onSaved: () => void }) {
  const [label, setLabel] = useState(m.label);
  const [merchant, setMerchant] = useState(m.merchant_account ?? '');
  useEffect(() => { setLabel(m.label); setMerchant(m.merchant_account ?? ''); }, [m]);
  const { busy, error, run } = useAction();
  const dirty = label.trim() !== m.label || merchant.trim() !== (m.merchant_account ?? '');
  const save = async (enabled = m.enabled) => {
    if (await run(() => paymentsApi.updateMethod(m.code, { label: label.trim(), merchantAccount: merchant.trim() || null, enabled }), 'Payment method saved.') !== undefined) onSaved();
  };
  const locked = !m.available;
  const toggleLocked = locked || m.code === 'CASH';
  return (
    <article className={`method-card ${locked ? 'unavailable' : ''}`} aria-disabled={locked || undefined}>
      <div className={`stat-icon ${m.code === 'CASH' ? 'green' : 'amber'}`}><MethodIcon code={m.code} size={20} /></div>
      <div>
        <h3>{m.label}
          {locked ? <Pill><Lock size={11} aria-hidden="true" />Not available yet</Pill> : m.enabled ? <Pill tone="green">Enabled</Pill> : <Pill>Disabled</Pill>}
        </h3>
        <p className="muted small">{[m.provider, m.note].filter(Boolean).join(' · ')}</p>
        {m.updated_at && <p className="muted small">Last changed {dateTime(m.updated_at)}</p>}
      </div>
      <Switch checked={m.enabled} disabled={toggleLocked || busy} onChange={v => save(v)}
        label={locked ? 'Unavailable' : m.code === 'CASH' ? 'Always on' : m.enabled ? 'On' : 'Off'} />
      <div className="method-edit">
        <Field label="Label shown to sellers">
          <input className="input" value={label} disabled={locked} onChange={e => setLabel(e.target.value)} maxLength={60} />
        </Field>
        {m.requires_reference && (
          <Field label="Merchant account" hint={locked ? 'Set when the integration is connected.' : undefined}>
            <input className="input" value={merchant} disabled={locked} onChange={e => setMerchant(e.target.value)} maxLength={60} placeholder="Merchant number" />
          </Field>
        )}
        {!locked && <div><button className="btn" disabled={!dirty || busy || !label.trim()} onClick={() => save()}>{busy ? 'Saving…' : 'Save'}</button></div>}
      </div>
      {error && <div className="method-edit"><ErrorNote error={error} /></div>}
    </article>
  );
}
