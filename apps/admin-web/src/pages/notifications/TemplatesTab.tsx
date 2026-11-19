import { useState } from 'react';
import { Pencil } from 'lucide-react';
import { notificationsApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { usePagedQuery } from '../../hooks/usePagedQuery';
import { useDebouncedState } from '../../hooks/useDebounced';
import { DataTable, ErrorNote, Field, FilterBar, FormModal, Loading, Note, Pager, Panel, Pill, SearchInput, Switch, TitleCell } from '../../components/ui';
import { AUDIENCE_LABEL, smsParts } from '../../utils/notifications';
import type { NotificationEvent } from '../../types/models';

/** Every message the platform can send, with its French and English text and an SMS on/off switch. */
export function TemplatesTab() {
  const [q, setQ, search] = useDebouncedState('');
  const { data, items, pager, error, loading, reload } = usePagedQuery((page, size) => notificationsApi.events({ q: search, page, size }), [search]);
  const [editing, setEditing] = useState<NotificationEvent | null>(null);
  if (loading && !data) return <Loading />;
  return (
    <Panel>
      <Note>Texts use placeholders such as {'{amount}'} that are filled in when the message is sent. Each message goes out in the recipient's language (set on their profile). Push notifications are not available yet.</Note>
      <FilterBar><SearchInput value={q} onChange={setQ} placeholder="Search messages…" /></FilterBar>
      <ErrorNote error={error} retry={reload} />
      <DataTable rows={items} rowKey={e => e.key} columns={[
        { header: 'Message', cell: e => <TitleCell title={e.name} sub={AUDIENCE_LABEL[e.audience]} /> },
        { header: 'Type', cell: e => <Pill tone={e.category === 'ESSENTIAL' ? 'navy' : 'purple'}>{e.category === 'ESSENTIAL' ? 'Essential' : 'Marketing'}</Pill> },
        { header: 'SMS', cell: e => <Pill tone={e.sms_enabled ? 'green' : 'grey'}>{e.sms_enabled ? 'On' : 'Off'}</Pill> },
        { header: 'Push', cell: () => <Pill tone="grey">Not available</Pill> },
        { header: 'French text', cell: e => <span className="sms-body">{e.fr}</span> },
        { header: '', align: 'right', cell: e => <button className="btn sm" onClick={() => setEditing(e)}><Pencil size={14} />Edit</button> },
      ]} />
      <Pager {...pager} />
      {editing && <TemplateModal event={editing} onClose={() => setEditing(null)} onDone={() => { setEditing(null); reload(); }} />}
    </Panel>
  );
}

function TemplateModal({ event, onClose, onDone }: { event: NotificationEvent; onClose: () => void; onDone: () => void }) {
  const [fr, setFr] = useState(event.fr);
  const [en, setEn] = useState(event.en);
  const [sms, setSms] = useState(event.sms_enabled);
  const { busy, error, run } = useAction();
  const submit = async () => { if (await run(() => notificationsApi.saveEvent(event.key, { smsEnabled: sms, fr, en }), 'Message saved.')) onDone(); };
  const count = (t: string) => `${t.length} characters · ${smsParts(t)} SMS`;
  return (
    <FormModal title={event.name} submitLabel="Save" busy={busy} error={error} onSubmit={submit} onClose={onClose}>
      <p className="muted small">You can use: {event.placeholders.map(p => `{${p}}`).join('  ')}</p>
      <Field label="French text" required hint={count(fr)}><textarea className="input" rows={3} value={fr} onChange={e => setFr(e.target.value)} required maxLength={480} /></Field>
      <Field label="English text" required hint={count(en)}><textarea className="input" rows={3} value={en} onChange={e => setEn(e.target.value)} required maxLength={480} /></Field>
      <Field label="SMS" group hint={event.locked ? 'Code messages cannot be switched off.' : !sms ? 'Nothing is sent for this message; it is recorded as skipped.' : undefined}>
        <Switch checked={sms} onChange={setSms} label={sms ? 'On' : 'Off'} disabled={event.locked} />
      </Field>
    </FormModal>
  );
}
