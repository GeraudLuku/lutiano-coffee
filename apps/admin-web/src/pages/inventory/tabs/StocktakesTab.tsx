import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { stocktakesApi } from '../../../api/services';
import { DataTable, ErrorNote, FilterBar, Pager, Panel, SelectFilter, TitleCell, ListState } from '../../../components/ui';
import { usePagedQuery } from '../../../hooks/usePagedQuery';
import { dateTime, num } from '../../../utils/format';
import { STOCKTAKE_STATUS } from '../../../utils/inventoryLabels';
import type { StocktakeStatus } from '../../../types/models';
import { StocktakeStatusPill } from '../InventoryBadges';

export function StocktakesTab({ version }: { version: number }) {
  const nav = useNavigate();
  const [status, setStatus] = useState('');
  const { data, items, error, loading, reload, pager } = usePagedQuery(
    (page, size) => stocktakesApi.list({ status: (status || undefined) as StocktakeStatus | undefined, page, size }), [status, version]);
  return (
    <Panel>
      <FilterBar>
        <SelectFilter label="Status" value={status} onChange={setStatus} allLabel="All statuses" options={Object.entries(STOCKTAKE_STATUS).map(([k, [l]]) => [k, l])} />
      </FilterBar>
      <ErrorNote error={error} retry={reload} />
      <ListState loading={loading} data={data} items={items} empty={{ title: 'No stock counts yet', text: 'Use Start Stock Count to count central stock or a sales location.' }}>
        <DataTable rows={items} rowKey={s => s.id} onRowClick={s => nav(`/inventory/stocktakes/${s.id}`)} columns={[
          { header: 'Started', cell: s => <TitleCell title={dateTime(s.created_at)} sub={s.created_by_name ?? undefined} /> },
          { header: 'Location', cell: s => <TitleCell title={s.location_name} sub={s.note ?? undefined} /> },
          { header: 'Progress', align: 'right', cell: s => `${num(s.counted_count)} / ${num(s.line_count)}` },
          { header: 'Differences', align: 'right', cell: s => <span className={s.variance_count ? 'warn' : ''}>{num(s.variance_count)}</span> },
          { header: 'Status', cell: s => <TitleCell title={<StocktakeStatusPill status={s.status} />} sub={s.decision_note ?? undefined} /> },
        ]} />
      </ListState>
      <Pager {...pager} />
    </Panel>
  );
}
