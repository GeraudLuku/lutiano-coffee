import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Building2, Boxes, CreditCard, Globe2, Pencil, Store, Trophy } from 'lucide-react';
import { businessApi } from '../../api/services';
import { useAuth } from '../../auth/AuthContext';
import { useAction } from '../../hooks/useAction';
import { useLoad } from '../../hooks/useLoad';
import { ErrorNote, Facts, Field, FormGrid, Loading, Logo, Note, Panel } from '../../components/ui';
import { dateTime } from '../../utils/format';

type Section = 'business' | 'sales' | 'loyalty' | 'payments' | 'inventory' | 'regional';
const SECTIONS: [Section, string, string, typeof Building2][] = [
  ['business', 'Business Information', 'Company details and branding', Building2], ['sales', 'Sales Configuration', 'Sales operations and channels', Store],
  ['loyalty', 'Loyalty Settings', 'Points, rewards and tiers', Trophy], ['payments', 'Payment Settings', 'Payment methods and providers', CreditCard],
  ['inventory', 'Inventory Settings', 'Stock management and alerts', Boxes], ['regional', 'Regional Settings', 'Timezone, currency and language', Globe2],
];

/** Settings → General (design 15.1): business details you can edit, and pointers to where each area is configured. */
export function GeneralTab() {
  const [section, setSection] = useState<Section>('business');
  return (
    <div className="settings-layout">
      <Panel>
        <ul className="settings-menu">
          {SECTIONS.map(([k, title, sub, Icon]) => (
            <li key={k}><button className={section === k ? 'active' : ''} onClick={() => setSection(k)}><Icon size={19} /><span><b>{title}</b><small>{sub}</small></span></button></li>
          ))}
        </ul>
      </Panel>
      <div>
        {section === 'business' && <BusinessInfoPanel />}
        {section === 'sales' && <Pointer title="Sales Configuration" text="Sales locations, their channels (Fixed POS, Mobile Barista, Market Agent), assigned sellers and accepted payments are managed per location." to="/locations" link="Open Sales Locations" />}
        {section === 'loyalty' && <Pointer title="Loyalty Settings" text="Points per amount spent, point value, expiry, minimum redemption, earning rules, rewards and tiers." to="/loyalty?tab=settings" link="Open Loyalty configuration" />}
        {section === 'payments' && <Pointer title="Payment Settings" text="Cash is accepted today. MTN Mobile Money and Orange Money are configured but unavailable until their provider integration exists." to="/payments/methods" link="Open Payment Methods" />}
        {section === 'inventory' && <Pointer title="Inventory Settings" text="Each product has a low-stock threshold. Stock falling to it alerts the stock approvers and the sellers at that location. Transfers and adjustments need a second approver." to="/inventory?tab=reorder" link="Open Reorder Alerts" />}
        {section === 'regional' && (
          <Panel title="Regional Settings" icon={<Globe2 size={19} />}>
            <Facts items={[['Currency', 'XAF (Central African CFA franc), whole amounts'], ['Business time zone', 'Africa/Douala (UTC+1)'], ['Stored timestamps', 'UTC'],
              ['Languages', 'French and English (per user)']]} />
            <Note>These are fixed for V1 and are not editable here.</Note>
          </Panel>
        )}
      </div>
    </div>
  );
}

function Pointer({ title, text, to, link }: { title: string; text: string; to: string; link: string }) {
  return <Panel title={title}><p>{text}</p><Link className="btn gold" to={to}>{link}</Link></Panel>;
}

function BusinessInfoPanel() {
  const { can } = useAuth();
  const { data, error, loading, reload } = useLoad(() => businessApi.get(), []);
  const [editing, setEditing] = useState(false);
  const [f, setF] = useState({ name: '', description: '', email: '', phone: '', address: '' });
  const action = useAction();
  useEffect(() => { if (data) setF({ name: data.name, description: data.description ?? '', email: data.email ?? '', phone: data.phone ?? '', address: data.address ?? '' }); }, [data]);
  if (loading && !data) return <Loading />;
  const set = (k: keyof typeof f, v: string) => setF(prev => ({ ...prev, [k]: v }));
  const save = async () => { if (await action.run(() => businessApi.update(f), 'Business information saved.')) { setEditing(false); reload(); } };
  return (
    <div className="grid-form wide-left">
      <Panel title="Business Information" icon={<Building2 size={19} />} action={can('ADMIN') && !editing && <button className="btn sm" onClick={() => setEditing(true)}><Pencil size={14} />Edit</button>}>
        <ErrorNote error={error || action.error} retry={reload} />
        {editing ? (
          <form onSubmit={e => { e.preventDefault(); save(); }}>
            <Field label="Business Name" required><input className="input" value={f.name} onChange={e => set('name', e.target.value)} required maxLength={150} /></Field>
            <Field label="Description"><input className="input" value={f.description} onChange={e => set('description', e.target.value)} maxLength={255} /></Field>
            <FormGrid>
              <Field label="Email"><input className="input" type="email" value={f.email} onChange={e => set('email', e.target.value)} /></Field>
              <Field label="Phone"><input className="input" value={f.phone} onChange={e => set('phone', e.target.value)} maxLength={30} /></Field>
            </FormGrid>
            <Field label="Address"><input className="input" value={f.address} onChange={e => set('address', e.target.value)} maxLength={255} /></Field>
            <div className="form-actions"><button type="button" className="btn" onClick={() => { setEditing(false); reload(); }}>Cancel</button><button className="btn gold" disabled={action.busy}>{action.busy ? 'Saving…' : 'Save'}</button></div>
          </form>
        ) : (
          <Facts items={[['Business Name', data?.name ?? '—'], ['Description', data?.description ?? '—'], ['Email', data?.email ?? '—'], ['Phone', data?.phone ?? '—'],
            ['Address', data?.address ?? '—'], ['Currency', 'XAF (Central African CFA franc)'], ['Timezone', 'Africa/Douala (UTC+1)'], ['Last updated', data?.updated_at ? dateTime(data.updated_at) : '—']]} />
        )}
      </Panel>
      <div className="brand-card"><Logo size={72} /><b>LUTIANO</b><small>Coffee Business OS</small></div>
    </div>
  );
}
