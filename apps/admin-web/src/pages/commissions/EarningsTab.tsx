import { useNavigate } from 'react-router-dom';
import { earningsApi } from '../../api/services';
import { usePagedQuery } from '../../hooks/usePagedQuery';
import { DataTable, ErrorNote, LabelPill, Pager, Panel, TitleCell, ListState } from '../../components/ui';
import { date, dateTime, xaf } from '../../utils/format';

const STATUS: Record<string, [string, string]> = { OPEN: ['Open', 'blue'], FINALIZED: ['Finalised', 'green'] };

/** Monthly earning periods; open ones are calculated live, finalised ones are frozen. */
export function EarningsTab() {
  const nav = useNavigate();
  const { data, items, pager, error, loading, reload } = usePagedQuery((page, size) => earningsApi.periods({ page, size }), []);
  return (
    <Panel title="Earning Periods">
      <p className="muted small">Each month's statement = commissions on its sales + bonuses of targets ending in it + adjustments dated in it. Finalise a month once it has ended to freeze it.</p>
      <ErrorNote error={error} retry={reload} />
      <ListState loading={loading} data={data} items={items} empty={{ title: 'No earning periods yet' }}>
        <DataTable rows={items} rowKey={p => p.id} onRowClick={p => nav(`/commissions/periods/${p.id}`)} columns={[
          { header: 'Period', cell: p => <TitleCell title={new Date(`${p.starts_on.slice(0, 10)}T12:00:00Z`).toLocaleDateString('en-GB', { month: 'long', year: 'numeric' })}
              sub={`${date(p.starts_on)} – ${date(p.ends_on)}`} /> },
          { header: 'Status', cell: p => <TitleCell title={<LabelPill map={STATUS} value={p.status} />} sub={p.finalized_at ? `${p.finalized_by_name ?? ''} ${dateTime(p.finalized_at)}` : undefined} /> },
          { header: 'Total earnings', align: 'right', cell: p => (p.status === 'FINALIZED' ? xaf(p.total_xaf ?? 0) : <span className="muted">Open — see details</span>) },
          { header: 'Payable', align: 'right', cell: p => (p.status === 'FINALIZED' ? <b>{xaf(p.payable_xaf ?? 0)}</b> : '—') },
        ]} />
      </ListState>
      <Pager {...pager} />
    </Panel>
  );
}
