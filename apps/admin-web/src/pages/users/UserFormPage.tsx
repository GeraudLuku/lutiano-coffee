import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { Camera, MapPin, Shield, User } from 'lucide-react';
import { operationsApi, staffApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { useLoad } from '../../hooks/useLoad';
import { ChoiceTiles, ErrorNote, Field, FormGrid, ImageField, Loading, Note, PageHeader, Panel } from '../../components/ui';
import { uploadPicked } from '../../utils/images';
import { ROLE_DESCRIPTION, ROLE_LABEL, SELLER_ROLE_CHANNEL, SELLER_ROLE_DESCRIPTION, SELLER_ROLE_LABEL, TYPE_LABEL } from '../../utils/labels';
import type { Role, SellerRole } from '../../types/models';

interface FormState {
  fullName: string; email: string; phone: string; role: Role; sellerRole: SellerRole; address: string; area: string; password: string; operationId: string; language: 'fr' | 'en';
}
const EMPTY: FormState = { fullName: '', email: '', phone: '', role: 'SELLER', sellerRole: 'FIXED_POS_BARISTA', address: '', area: '', password: '', operationId: '', language: 'fr' };

export function UserFormPage() {
  const { id } = useParams();
  const [params] = useSearchParams();
  const nav = useNavigate();
  const editing = !!id;
  const existing = useLoad(() => (id ? staffApi.get(id) : Promise.resolve(null)), [id]);
  const ops = useLoad(() => operationsApi.all(), []);
  const [f, setF] = useState<FormState>(() => ({ ...EMPTY, role: (params.get('role') as Role) || EMPTY.role }));
  const [photo, setPhoto] = useState<File | null>(null);
  const { busy, error, run } = useAction();
  useEffect(() => {
    const d = existing.data;
    if (d) setF({ ...EMPTY, fullName: d.full_name, email: d.email, phone: d.phone ?? '', role: d.role,
      sellerRole: d.seller_role ?? EMPTY.sellerRole, address: d.address ?? '', area: d.area ?? '', language: d.language ?? 'fr' });
  }, [existing.data]);
  const set = <K extends keyof FormState>(k: K, v: FormState[K]) => setF(prev => ({ ...prev, [k]: v }));
  const seller = f.role === 'SELLER';
  const channel = SELLER_ROLE_CHANNEL[f.sellerRole];
  const matchingOps = (ops.data ?? []).filter(o => o.active && o.type === channel);

  async function submit(e: FormEvent) {
    e.preventDefault();
    const saved = await run(async () => {
      const body = { fullName: f.fullName, email: f.email, phone: f.phone || null, role: f.role, sellerRole: seller ? f.sellerRole : null,
        address: f.address, area: f.area, password: f.password, language: f.language };
      const user = editing ? await staffApi.update(id!, body) : await staffApi.create(body);
      if (!editing && seller && f.operationId) await staffApi.assign(user.id, f.operationId);
      await uploadPicked(photo, file => staffApi.uploadImage(user.id, file), 'User');
      return user;
    }, editing ? 'User updated.' : 'User created.');
    if (saved) nav(`/users/${saved.id}`);
  }

  if (editing && existing.loading) return <Loading />;
  const roles = (Object.keys(ROLE_LABEL) as Role[]).map(r => ({ value: r, label: ROLE_LABEL[r], description: ROLE_DESCRIPTION[r] }));
  const sellerRoles = (Object.keys(SELLER_ROLE_LABEL) as SellerRole[]).map(r => ({ value: r, label: SELLER_ROLE_LABEL[r], description: SELLER_ROLE_DESCRIPTION[r] }));
  const before = existing.data;
  const mismatchWarning = editing && before?.role === 'SELLER' && seller && before.seller_role !== f.sellerRole
    && before.operations.some(o => o.type !== channel);
  return (
    <form onSubmit={submit}>
      <PageHeader title={editing ? 'Edit User' : seller ? 'Add Seller' : 'Add User'}
        subtitle={editing ? 'Update profile details and role.' : 'Create an account. The person signs in with this email and temporary password.'}
        crumbs={[['Users', '/users'], [editing ? 'Edit User' : 'Add User']]}
        actions={<><button type="button" className="btn" onClick={() => nav(-1)}>Cancel</button><button className="btn gold" disabled={busy}>{busy ? 'Saving…' : editing ? 'Save Changes' : 'Create User'}</button></>} />
      <ErrorNote error={error || existing.error} />
      <div className="grid-form">
        <div className="stack">
          <Panel title="Profile Information" icon={<User size={19} />}>
            <FormGrid>
              <Field label="Full name" required><input className="input" value={f.fullName} onChange={e => set('fullName', e.target.value)} required maxLength={150} /></Field>
              <Field label="Phone number"><input className="input" value={f.phone} onChange={e => set('phone', e.target.value)} placeholder="+237 6XX XX XX XX" maxLength={30} /></Field>
              <Field label="Message language" hint="The language of the SMS messages this person receives.">
                <select className="input" value={f.language} onChange={e => set('language', e.target.value as 'fr' | 'en')}><option value="fr">Français</option><option value="en">English</option></select>
              </Field>
              <Field label="Email address" required><input className="input" type="email" value={f.email} onChange={e => set('email', e.target.value)} required /></Field>
              {!editing && (
                <Field label="Temporary password" required hint="At least 10 characters. Share it securely; the user can change it after signing in.">
                  <input className="input" type="password" autoComplete="new-password" value={f.password} onChange={e => set('password', e.target.value)} required minLength={10} maxLength={72} />
                </Field>
              )}
            </FormGrid>
          </Panel>
          <Panel title="Photo" icon={<Camera size={19} />}>
            <ImageField url={existing.data?.image_url} file={photo} onChange={setPhoto} label="Choose a photo" round compact />
            <small className="muted">JPG, PNG or WebP, up to 5 MB. Shown on the user's profile and in lists.</small>
          </Panel>
          <Panel title="Location / Area" icon={<MapPin size={19} />}>
            <FormGrid>
              <Field label="Area" hint="e.g. Douala – Akwa / Bonanjo"><input className="input" value={f.area} onChange={e => set('area', e.target.value)} maxLength={120} /></Field>
              <Field label="Address"><input className="input" value={f.address} onChange={e => set('address', e.target.value)} maxLength={255} /></Field>
            </FormGrid>
          </Panel>
        </div>
        <Panel title="Role & Access" icon={<Shield size={19} />}>
          <Field label="Account type" required group><ChoiceTiles name="role" value={f.role} onChange={r => set('role', r)} options={roles} /></Field>
          {seller && (
            <Field label="Seller role" required group hint={`Works ${TYPE_LABEL[channel]} locations. Sellers can only be assigned to locations of their channel.`}>
              <ChoiceTiles name="sellerRole" value={f.sellerRole} onChange={r => setF(prev => ({ ...prev, sellerRole: r, operationId: '' }))} options={sellerRoles} />
            </Field>
          )}
          {!editing && seller && (
            <Field label="Assign to sales location" hint={matchingOps.length ? 'Optional. You can add more locations later.' : `No active ${TYPE_LABEL[channel]} locations yet.`}>
              <select className="input" value={f.operationId} onChange={e => set('operationId', e.target.value)}>
                <option value="">Not assigned yet</option>
                {matchingOps.map(o => <option key={o.id} value={o.id}>{o.name} ({o.code})</option>)}
              </select>
            </Field>
          )}
          {editing && before?.role === 'SELLER' && !seller && <Note tone="warn">Changing this seller's account type removes all their sales location assignments.</Note>}
          {mismatchWarning && <Note tone="warn">Locations that are not {TYPE_LABEL[channel]} locations will be removed from this seller when you save.</Note>}
        </Panel>
      </div>
    </form>
  );
}
