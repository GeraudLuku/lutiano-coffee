import { useState } from 'react';
import { Megaphone } from 'lucide-react';
import { announcementsApi, operationsApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { useLoad } from '../../hooks/useLoad';
import { usePagedQuery } from '../../hooks/usePagedQuery';
import { DataTable, ErrorNote, Field, FormModal, ListState, Note, Pager, Panel, Pill, Switch, TitleCell } from '../../components/ui';
import { dateTime, num } from '../../utils/format';
import { SELLER_ROLE_LABEL } from '../../utils/labels';
import type { Announcement, SellerRole } from '../../types/models';

const AUDIENCE_LABEL: Record<Announcement['audience'], string> = { ALL_SELLERS: 'All sellers', ROLE: 'A seller role', OPERATION: 'One sales operation', CUSTOMERS: 'Customers who accept offers' };

/** Messages to sellers: they appear in the Notifications of the Seller app. */
export function AnnouncementsTab() {
  const [creating, setCreating] = useState(false);
  const [version, setVersion] = useState(0);
  const { data, items, error, loading, reload, pager } = usePagedQuery((page, size) => announcementsApi.list({ page, size }), [version]);
  return (
    <>
      <Panel title="Announcements" icon={<Megaphone size={19} />} action={<button className="btn gold" onClick={() => setCreating(true)}>New Announcement</button>}>
        <Note>An announcement appears in the Notifications of the Seller app for the sellers you choose. An important one reaches them even if they switched System notifications off. It is not sent by SMS.</Note>
        <ErrorNote error={error} retry={reload} />
        <ListState loading={loading} data={data} items={items} empty={{ title: 'No announcements yet' }}>
          <DataTable rows={items} rowKey={a => a.id} columns={[
            { header: 'Sent', cell: a => <TitleCell title={dateTime(a.created_at)} sub={a.created_by_name} /> },
            { header: 'Message', cell: a => <TitleCell title={<>{a.title} {a.important && <Pill tone="amber">Important</Pill>}</>} sub={a.body.length > 90 ? `${a.body.slice(0, 90)}…` : a.body} /> },
            { header: 'To', cell: a => <TitleCell title={AUDIENCE_LABEL[a.audience]} sub={a.audience === 'ROLE' && a.seller_role ? SELLER_ROLE_LABEL[a.seller_role] : a.operation_name ?? undefined} /> },
            { header: 'Sellers', align: 'right', cell: a => num(a.recipient_count) },
          ]} />
        </ListState>
        <Pager {...pager} />
      </Panel>
      {creating && <AnnouncementModal onClose={() => setCreating(false)} onDone={() => { setCreating(false); setVersion(v => v + 1); }} />}
    </>
  );
}

function AnnouncementModal({ onClose, onDone }: { onClose: () => void; onDone: () => void }) {
  const [title, setTitle] = useState('');
  const [body, setBody] = useState('');
  const [audience, setAudience] = useState<Announcement['audience']>('ALL_SELLERS');
  const [sellerRole, setSellerRole] = useState<SellerRole>('MOBILE_BARISTA');
  const [operationId, setOperationId] = useState('');
  const [important, setImportant] = useState(false);
  const { busy, error, run } = useAction();
  const operations = useLoad(() => operationsApi.all(), []);
  const submit = async () => {
    const result = await run(() => announcementsApi.send({ title, body, audience, important, sellerRole: audience === 'ROLE' ? sellerRole : undefined, operationId: audience === 'OPERATION' ? operationId : undefined }), 'Announcement sent.');
    if (result) onDone();
  };
  return (
    <FormModal title="New announcement" submitLabel="Send" busy={busy} error={error} onSubmit={submit} onClose={onClose}>
      <Field label="Title" required><input className="input" value={title} onChange={e => setTitle(e.target.value)} maxLength={150} required /></Field>
      <Field label="Message" required><textarea className="input" rows={5} value={body} onChange={e => setBody(e.target.value)} maxLength={2000} required /></Field>
      <Field label="Send to" required>
        <select className="input" value={audience} onChange={e => setAudience(e.target.value as Announcement['audience'])}>
          {(Object.keys(AUDIENCE_LABEL) as Announcement['audience'][]).map(k => <option key={k} value={k}>{AUDIENCE_LABEL[k]}</option>)}
        </select>
      </Field>
      {audience === 'ROLE' && (
        <Field label="Seller role" required>
          <select className="input" value={sellerRole} onChange={e => setSellerRole(e.target.value as SellerRole)}>
            {(Object.keys(SELLER_ROLE_LABEL) as SellerRole[]).map(k => <option key={k} value={k}>{SELLER_ROLE_LABEL[k]}</option>)}
          </select>
        </Field>
      )}
      {audience === 'OPERATION' && (
        <Field label="Sales operation" required>
          <select className="input" value={operationId} onChange={e => setOperationId(e.target.value)} required>
            <option value="">Choose…</option>
            {(operations.data ?? []).filter(o => o.active).map(o => <option key={o.id} value={o.id}>{o.name}</option>)}
          </select>
        </Field>
      )}
      <Field label="Important" group hint="Reaches sellers even if they switched System notifications off.">
        <Switch checked={important} onChange={setImportant} label={important ? 'Important' : 'Normal'} />
      </Field>
    </FormModal>
  );
}
