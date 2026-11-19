import { useState } from 'react';
import { Paperclip } from 'lucide-react';
import { supportApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { usePagedQuery } from '../../hooks/usePagedQuery';
import { DataTable, ErrorNote, Field, FilterBar, FormModal, ListState, Pager, Panel, Pill, SelectFilter, TitleCell } from '../../components/ui';
import { dateTime, num } from '../../utils/format';
import type { SupportRequest, SupportStatus, SupportType } from '../../types/models';

const TYPE_LABEL: Record<SupportType, string> = { QUESTION: 'Question', ISSUE: 'Issue', IMPROVEMENT: 'Improvement', ACCOUNT: 'Account', ORDER: 'Order & payment', REWARDS: 'Points & rewards' };
const STATUS_LABEL: Record<SupportStatus, [string, string]> = { OPEN: ['Open', 'amber'], IN_PROGRESS: ['In progress', 'blue'], RESOLVED: ['Resolved', 'green'] };

/** Help requests sellers send from the app: read them, reply, and set their status. The reply shows in the seller's app. */
export function SupportTab() {
  const [status, setStatus] = useState('');
  const [requester, setRequester] = useState('');
  const [version, setVersion] = useState(0);
  const [open, setOpen] = useState<SupportRequest | null>(null);
  const { data, items, summary, error, loading, reload, pager } = usePagedQuery(
    (page, size) => supportApi.list({ status: (status || undefined) as SupportStatus | undefined, requester: requester || undefined, page, size }), [status, requester, version]);
  return (
    <>
      <Panel title="Support requests">
        {summary && <p className="muted">{num(summary.open_count)} open · {num(summary.in_progress)} in progress · {num(summary.resolved)} resolved</p>}
        <FilterBar>
          <SelectFilter label="From" value={requester} onChange={setRequester} allLabel="Sellers and customers" options={[['STAFF', 'Sellers'], ['CUSTOMER', 'Customers']]} />
          <SelectFilter label="Status" value={status} onChange={setStatus} allLabel="All statuses" options={Object.entries(STATUS_LABEL).map(([k, [l]]) => [k, l])} />
        </FilterBar>
        <ErrorNote error={error} retry={reload} />
        <ListState loading={loading} data={data} items={items} empty={{ title: 'No support requests', text: 'Requests sent from the Seller and Customer apps appear here.' }}>
          <DataTable rows={items} rowKey={r => r.id} columns={[
            { header: 'Request', cell: r => <TitleCell title={r.reference} sub={`${dateTime(r.created_at)} · ${r.user_name}${r.requester_type === 'CUSTOMER' ? ' (customer)' : ''}`} /> },
            { header: 'Subject', cell: r => <TitleCell title={<>{r.subject} {Number(r.has_attachment) > 0 && <Paperclip size={13} />}</>} sub={TYPE_LABEL[r.type]} /> },
            { header: 'Status', cell: r => <Pill tone={STATUS_LABEL[r.status][1]}>{STATUS_LABEL[r.status][0]}</Pill> },
            { header: '', align: 'right', cell: r => <button className="btn sm" onClick={() => setOpen(r)}>{r.reply ? 'View' : 'Reply'}</button> },
          ]} />
        </ListState>
        <Pager {...pager} />
      </Panel>
      {open && <ReplyModal request={open} onClose={() => setOpen(null)} onDone={() => { setOpen(null); setVersion(v => v + 1); }} />}
    </>
  );
}

function ReplyModal({ request, onClose, onDone }: { request: SupportRequest; onClose: () => void; onDone: () => void }) {
  const [status, setStatus] = useState<SupportStatus>(request.status === 'OPEN' ? 'IN_PROGRESS' : request.status);
  const [reply, setReply] = useState(request.reply ?? '');
  const { busy, error, run } = useAction();
  const submit = async () => {
    if (await run(() => supportApi.reply(request.id, { status, reply: reply.trim() === (request.reply ?? '') ? undefined : reply }), 'Saved. The seller is told when you reply.')) onDone();
  };
  async function download() {
    const blob = await supportApi.attachment(request.id);
    window.open(URL.createObjectURL(blob), '_blank', 'noopener');
  }
  return (
    <FormModal title={`${request.reference}: ${request.subject}`} submitLabel="Save" busy={busy} error={error} onSubmit={submit} onClose={onClose}>
      <p><b>{request.user_name}</b> · {TYPE_LABEL[request.type]} · {dateTime(request.created_at)}</p>
      <p style={{ whiteSpace: 'pre-wrap' }}>{request.description}</p>
      {Number(request.has_attachment) > 0 && <button type="button" className="btn sm" onClick={download}><Paperclip size={14} /> {request.attachment_name}</button>}
      <Field label="Status">
        <select className="input" value={status} onChange={e => setStatus(e.target.value as SupportStatus)}>
          {Object.entries(STATUS_LABEL).map(([k, [l]]) => <option key={k} value={k}>{l}</option>)}
        </select>
      </Field>
      <Field label="Reply to the seller" hint="They read it in the app and are notified.">
        <textarea className="input" rows={5} value={reply} onChange={e => setReply(e.target.value)} maxLength={2000} />
      </Field>
    </FormModal>
  );
}
