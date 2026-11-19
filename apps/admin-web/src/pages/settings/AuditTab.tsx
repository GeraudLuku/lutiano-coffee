import { useState } from 'react';
import { Download, FileText, ShieldAlert, Users } from 'lucide-react';
import { staffApi } from '../../api/services';
import { auditApi } from '../../api/services';
import { useLoad } from '../../hooks/useLoad';
import { usePagedQuery } from '../../hooks/usePagedQuery';
import { useDebouncedState } from '../../hooks/useDebounced';
import { useAction } from '../../hooks/useAction';
import { fetchAll } from '../../api/pages';
import { DataTable, ErrorNote, FilterBar, Pager, Panel, Pill, SearchInput, SelectFilter, StatCard, ListState } from '../../components/ui';
import { dateTime, humanize, num } from '../../utils/format';
import { saveFile, toCsv } from '../../utils/files';
import { MODULE } from '../../utils/labels';

export function AuditTab() {
  const [q, setQ, search] = useDebouncedState('');
  const [module, setModule] = useState('');
  const [action, setAction] = useState('');
  const [actorId, setActorId] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const people = useLoad(() => staffApi.all(), []);
  const actions = useLoad(() => auditApi.actions(), []);
  const filters = { q: search, entityType: module, action, actorId, from, to };
  const { data, items, summary, pager, error, loading, reload } = usePagedQuery((page, size) => auditApi.list({ ...filters, page, size }), [search, module, action, actorId, from, to]);
  const exporting = useAction();

  const exportCsv = () => exporting.run(async () => saveFile(toCsv((await fetchAll((page, size) => auditApi.list({ ...filters, page, size }), 5000)).map(e => ({ ...e, created_at: dateTime(e.created_at), action: humanize(e.action) })),
    [['created_at', 'Date & time (Douala)'], ['actor_name', 'User'], ['action', 'Action'], ['entity_type', 'Module'], ['details', 'Details'], ['ip', 'IP address']]), 'lutiano-audit-log.csv'));

  return (
    <>
      <div className="stats3">
        <StatCard tone="amber" icon={<FileText size={20} />} value={num(summary?.events ?? 0)} label="Log Entries" />
        <StatCard tone="blue" icon={<Users size={20} />} value={num(summary?.users ?? 0)} label="Unique Users" />
        <StatCard tone="red" icon={<ShieldAlert size={20} />} value={num(summary?.critical ?? 0)} label="Critical Actions" />
        <div className="stat-actions"><button className="btn gold" onClick={exportCsv} disabled={!data?.total || exporting.busy}><Download size={16} />{exporting.busy ? 'Exporting…' : 'Export Logs'}</button></div>
      </div>
      <ErrorNote error={error || exporting.error} retry={reload} />
      <Panel title="Recent Activity Logs">
        <FilterBar>
          <SearchInput value={q} onChange={setQ} placeholder="Search logs, users or details…" />
          <label className="inline-date"><span>From</span><input className="input" type="date" value={from} max={to || undefined} onChange={e => setFrom(e.target.value)} /></label>
          <label className="inline-date"><span>To</span><input className="input" type="date" value={to} min={from || undefined} onChange={e => setTo(e.target.value)} /></label>
          <SelectFilter label="User" value={actorId} onChange={setActorId} allLabel="All Users" options={(people.data ?? []).map(u => [u.id, u.full_name] as [string, string])} />
          <SelectFilter label="Module" value={module} onChange={setModule} allLabel="All Modules" options={Object.entries(MODULE).map(([k, [l]]) => [k, l])} />
          <SelectFilter label="Action" value={action} onChange={setAction} allLabel="All Actions" options={(actions.data ?? []).map(a => [a, humanize(a)])} />
        </FilterBar>
        <ListState loading={loading} data={data} items={items} empty={{ title: 'No log entries' }}>
          <DataTable rows={items} rowKey={e => e.id ?? e.created_at} columns={[
            { header: 'Date & Time', cell: e => dateTime(e.created_at) },
            { header: 'User', cell: e => e.actor_name ?? 'System' },
            { header: 'Action', cell: e => <Pill tone="blue">{humanize(e.action)}</Pill> },
            { header: 'Module', cell: e => { const [l, t] = MODULE[e.entity_type ?? ''] ?? [e.entity_type, 'grey']; return <Pill tone={t}>{l}</Pill>; } },
            { header: 'Details', cell: e => e.details ?? '—' },
            { header: 'IP Address', cell: e => e.ip ?? '—' },
          ]} />
        </ListState>
        <Pager {...pager} />
      </Panel>
    </>
  );
}
