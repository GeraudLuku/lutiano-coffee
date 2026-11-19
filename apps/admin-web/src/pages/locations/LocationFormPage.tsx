import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { CheckCircle2, ImagePlus, MapPin, Settings2, Store } from 'lucide-react';
import { operationsApi, paymentsApi, staffApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { useLoad } from '../../hooks/useLoad';
import { ChoiceTiles, ErrorNote, Field, FormGrid, ImageField, Loading, Note, PageHeader, Panel } from '../../components/ui';
import { CODE_PREFIX, TYPE_DESCRIPTION, TYPE_LABEL } from '../../utils/labels';
import { uploadPicked } from '../../utils/images';
import type { OperationInput, OperationType } from '../../types/models';
import { TYPE_ICON } from './locationTypes';
import { RegionCityFields } from './RegionCityFields';
import { PositionPicker } from './LocationMaps';
import { EMPTY_HOURS, OpeningHoursField, draftToHours, hoursToDraft, type HoursDraft } from './OpeningHoursField';

const EMPTY: OperationInput = { name: '', type: 'FIXED_POS', address: '', description: '', city: '', region: '', latitude: null, longitude: null, active: true, paymentMethods: [] };

export function LocationFormPage() {
  const { id } = useParams();
  const nav = useNavigate();
  const editing = !!id;
  const existing = useLoad(() => (id ? operationsApi.get(id) : Promise.resolve(null)), [id]);
  const sellers = useLoad(async () => (id ? [] : (await staffApi.all()).filter(s => s.role === 'SELLER' && s.active)), [id]);
  const methods = useLoad(() => paymentsApi.methods(), []);
  const [f, setF] = useState<OperationInput>(EMPTY);
  const [sellerId, setSellerId] = useState('');
  const [allocate, setAllocate] = useState(false);
  const [photo, setPhoto] = useState<File | null>(null);
  const [hours, setHours] = useState<HoursDraft>(EMPTY_HOURS);
  const { busy, error, run } = useAction();
  useEffect(() => {
    const d = existing.data;
    if (d) setHours(hoursToDraft(d.hours));
    if (d) setF({ name: d.name, type: d.type, address: d.address ?? '', description: d.description ?? '', city: d.city ?? '', region: d.region ?? '', latitude: d.latitude, longitude: d.longitude, active: d.active, paymentMethods: d.payment_methods });
  }, [existing.data]);
  // New locations start by accepting every method; the ones not usable yet are accepted automatically once enabled.
  useEffect(() => { if (!editing && methods.data && f.paymentMethods.length === 0) setF(prev => ({ ...prev, paymentMethods: methods.data!.map(m => m.code) })); }, [editing, methods.data]); // eslint-disable-line react-hooks/exhaustive-deps
  const set = <K extends keyof OperationInput>(k: K, v: OperationInput[K]) => setF(prev => ({ ...prev, [k]: v }));
  const position = f.latitude != null && f.longitude != null ? { lat: f.latitude, lng: f.longitude } : null;
  const toggleMethod = (code: string, on: boolean) => set('paymentMethods', on ? [...f.paymentMethods, code] : f.paymentMethods.filter(c => c !== code));

  async function submit(e: FormEvent) {
    e.preventDefault();
    const saved = await run(async () => {
      if (!position) throw new Error("Set the location's position on the map.");
      const op = editing ? await operationsApi.update(id!, f) : await operationsApi.create(f);
      if (!editing && sellerId) await staffApi.assign(sellerId, op.id);
      await operationsApi.setHours(op.id, draftToHours(hours));
      await uploadPicked(photo, file => operationsApi.uploadImage(op.id, file), 'Location');
      return op;
    }, editing ? 'Location updated.' : 'Location created with its permanent QR code.');
    if (saved) nav(!editing && allocate ? `/inventory?action=transfer&operationId=${saved.id}` : `/locations/${saved.id}`);
  }

  if (editing && existing.loading) return <Loading />;
  const types = (Object.keys(TYPE_LABEL) as OperationType[]).map(t => {
    const Icon = TYPE_ICON[t];
    return { value: t, label: TYPE_LABEL[t], description: TYPE_DESCRIPTION[t], icon: <Icon size={22} /> };
  });
  return (
    <form onSubmit={submit}>
      <PageHeader title={editing ? 'Edit Sales Location' : 'Add Sales Location'}
        subtitle={editing ? 'Update the location details. The code and QR code do not change.' : 'Create a new sales operation and generate its permanent QR code.'}
        crumbs={[['Sales Locations', '/locations'], [editing ? 'Edit Sales Location' : 'Add Sales Location']]}
        actions={<button type="button" className="btn" onClick={() => nav('/locations')}>Back to Locations</button>} />
      <ErrorNote error={error || existing.error} />
      <div className="grid-form wide-left">
        <div className="stack">
          <Panel title="Location Information" icon={<MapPin size={19} />}>
            <FormGrid>
              <Field label="Location Name" required><input className="input" value={f.name} onChange={e => set('name', e.target.value)} required maxLength={150} /></Field>
              <Field label="Location Code" hint={editing ? 'Codes never change; they are printed with the QR code.' : 'Generated automatically when you create the location.'}>
                <input className="input" readOnly value={editing ? existing.data?.code ?? '' : `${CODE_PREFIX[f.type]}-###`} aria-readonly="true" />
              </Field>
            </FormGrid>
            <Field label="Location Type" required group>
              <ChoiceTiles name="type" columns={3} value={f.type} onChange={t => set('type', t)} options={types} />
            </Field>
            <Field label="Description" hint={`${f.description.length} / 200`}><textarea className="input" rows={3} value={f.description} onChange={e => set('description', e.target.value)} maxLength={200} placeholder="e.g. Mobile coffee service in Bonamoussadi area." /></Field>
            <Field label="Address / Area"><input className="input" value={f.address} onChange={e => set('address', e.target.value)} maxLength={255} /></Field>
            <RegionCityFields region={f.region} city={f.city} onChange={(region, city) => setF(prev => ({ ...prev, region, city }))} />
            <Field label="Map Position" required group hint={f.type === 'MOBILE' ? 'For a mobile barista, the usual base or starting point.' : undefined}>
              <PositionPicker value={position} region={f.region} city={f.city} type={f.type} onChange={p => setF(prev => ({ ...prev, latitude: p.lat, longitude: p.lng }))} />
            </Field>
          </Panel>
          <Panel title="Opening Hours" icon={<MapPin size={19} />}>
            <OpeningHoursField value={hours} onChange={setHours} />
          </Panel>
          <Panel title="Location Photo" icon={<ImagePlus size={19} />}>
            <ImageField url={existing.data?.image_url} file={photo} onChange={setPhoto} label="Choose a photo" compact />
            <small className="muted">JPG, PNG or WebP, up to 5 MB. Shown on the location page.</small>
          </Panel>
        </div>
        <Panel title="Assignment & Configuration" icon={<Settings2 size={19} />}>
          {editing ? <p className="muted">Manage assigned sellers from the location's Assigned Seller tab.</p> : (
            <>
              <Field label="Assign User (Seller)" hint="Optional. You can assign more sellers later.">
                <select className="input" value={sellerId} onChange={e => setSellerId(e.target.value)}>
                  <option value="">Not assigned yet</option>
                  {(sellers.data ?? []).map(s => <option key={s.id} value={s.id}>{s.full_name}</option>)}
                </select>
              </Field>
              <label className="check-line"><input type="checkbox" checked={allocate} onChange={e => setAllocate(e.target.checked)} />Allocate stock after creation</label>
              <small className="muted">Opens a stock transfer request for this location once it is created.</small>
            </>
          )}
          <Field label="Sales Settings: accepted payments" group hint="Mobile Money is accepted automatically once it is enabled under Payments → Payment Methods.">
            <div>
              {(methods.data ?? []).map(m => {
                const usable = m.available && m.enabled;
                return (
                  <label key={m.code} className="check-line">
                    <input type="checkbox" checked={f.paymentMethods.includes(m.code)} disabled={!usable} onChange={e => toggleMethod(m.code, e.target.checked)} />
                    {m.label}{!usable && <span className="muted small"> (not available yet)</span>}
                  </label>
                );
              })}
            </div>
          </Field>
          {!editing && (
            <Field label="Status" group>
              <div className="radio-tiles">
                <label className={`radio-tile${f.active ? ' on' : ''}`}><input type="radio" name="status" checked={f.active} onChange={() => set('active', true)} /><span><b>Active</b><small>Operation is live and can record sales.</small></span></label>
                <label className={`radio-tile${!f.active ? ' on' : ''}`}><input type="radio" name="status" checked={!f.active} onChange={() => set('active', false)} /><span><b>Inactive</b><small>Operation is paused (no sales allowed).</small></span></label>
              </div>
            </Field>
          )}
          <Note tone="ok" icon={<CheckCircle2 size={18} />}>
            {editing ? 'This location keeps its permanent QR code.' : 'A permanent QR code is generated when you create this location. It identifies this sales operation and can be used by the assigned seller(s).'}
          </Note>
        </Panel>
      </div>
      <div className="form-actions">
        <button type="button" className="btn" onClick={() => nav(-1)}>Cancel</button>
        <button className="btn gold" disabled={busy}><Store size={17} />{busy ? 'Saving…' : editing ? 'Save Changes' : 'Create Location'}</button>
      </div>
    </form>
  );
}
