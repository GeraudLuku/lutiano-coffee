import { useState } from 'react';
import { RotateCw } from 'lucide-react';
import { notificationsApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { useLoad } from '../../hooks/useLoad';
import { usePagedQuery } from '../../hooks/usePagedQuery';
import { useDebouncedState } from '../../hooks/useDebounced';
import { DataTable, ErrorNote, FilterBar, LabelPill, Pager, Panel, SearchInput, SelectFilter, StatCard, StatRow, TitleCell, ListState } from '../../components/ui';
import { dateTime, num } from '../../utils/format';
import { DELIVERY_STATUS } from '../../utils/notifications';
import { CheckCircle2, Clock, MessageSquare, XCircle } from 'lucide-react';

/** Every message the platform tried to send, newest first. Numbers are masked and codes are never stored. */
export function DeliveryLogTab() {
  const [q, setQ, search] = useDebouncedState('');
  const [status, setStatus] = useState('');
  const [event, setEvent] = useState('');
  const events = useLoad(() => notificationsApi.allEvents(), []);
  const { data, items, summary, pager, error, loading, reload } = usePagedQuery((page, size) => notificationsApi.deliveries({ status, event, q: search, page, size }), [status, event, search]);
  const action = useAction();
  return (
    <>
      <StatRow>
        <StatCard tone="blue" icon={<MessageSquare size={20} />} value={num(summary?.messages ?? 0)} label="Messages" />
        <StatCard tone="green" icon={<CheckCircle2 size={20} />} value={num(summary?.sent ?? 0)} label="Sent or logged" />
        <StatCard tone="red" icon={<XCircle size={20} />} value={num(summary?.failed ?? 0)} label="Failed" />
        <StatCard tone="amber" icon={<Clock size={20} />} value={num((summary?.skipped ?? 0) + (summary?.queued ?? 0))} label="Skipped or queued" />
      </StatRow>
      <Panel>
        <FilterBar>
          <SearchInput value={q} onChange={setQ} placeholder="Person, number or text…" />
          <SelectFilter label="Status" value={status} onChange={setStatus} allLabel="All Statuses" options={Object.entries(DELIVERY_STATUS).map(([k, v]) => [k, v[0]] as [string, string])} />
          <SelectFilter label="Message" value={event} onChange={setEvent} allLabel="All Messages" options={(events.data ?? []).map(e => [e.key, e.name] as [string, string])} />
        </FilterBar>
        <ErrorNote error={error || action.error} retry={reload} />
        <ListState loading={loading} data={data} items={items} empty={{ title: 'No messages yet', text: 'Messages appear here as the platform sends them.' }}>
          <DataTable rows={items} rowKey={d => d.id} columns={[
            { header: 'When', cell: d => dateTime(d.created_at) },
            { header: 'Message', cell: d => <TitleCell title={d.event_name} sub={d.language === 'fr' ? 'French' : 'English'} /> },
            { header: 'To', cell: d => <TitleCell title={d.recipient_name ?? '—'} sub={d.phone_masked || undefined} /> },
            { header: 'Text', cell: d => <span className="sms-body">{d.body}</span> },
            { header: 'Status', cell: d => <TitleCell title={<LabelPill map={DELIVERY_STATUS} value={d.status} />} sub={d.error ?? undefined} /> },
            { header: '', align: 'right', cell: d => d.resendable && (
              <button className="btn sm" disabled={action.busy} onClick={() => action.run(() => notificationsApi.resend(d.id), 'Message sent again.').then(r => r && reload())}><RotateCw size={14} />Resend</button>) },
          ]} />
        </ListState>
        <Pager {...pager} />
      </Panel>
    </>
  );
}
