import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { payoutsApi } from '../../api/services';
import { usePagedQuery } from '../../hooks/usePagedQuery';
import { useDebouncedState } from '../../hooks/useDebounced';
import { DataTable, ErrorNote, FilterBar, Pager, Panel, SearchInput, SelectFilter, TitleCell, ListState } from '../../components/ui';
import { dateTime, xaf } from '../../utils/format';
import { PAYOUT_METHOD, PAYOUT_STATUS, periodLabel } from '../../utils/payouts';
import { PayoutStatusPill } from './PayoutPills';

/** Payouts, newest first, filterable by status and method. Pass {@code sellerId} to show one seller's payouts. */
export function PayoutsList({ sellerId }: { sellerId?: string }) {
  const nav = useNavigate();
  const [q, setQ, search] = useDebouncedState('');
  const [status, setStatus] = useState('');
  const [method, setMethod] = useState('');
  const { data, items, pager, error, loading, reload } = usePagedQuery(
    (page, size) => payoutsApi.list({ sellerId, status, method, q: search, page, size }), [sellerId, status, method, search]);
  return (
    <Panel>
      <FilterBar>
        <SearchInput value={q} onChange={setQ} placeholder="Reference, seller or number…" />
        <SelectFilter label="Status" value={status} onChange={setStatus} allLabel="All Statuses" options={Object.entries(PAYOUT_STATUS).map(([k, v]) => [k, v[0]] as [string, string])} />
        <SelectFilter label="Method" value={method} onChange={setMethod} allLabel="All Methods" options={Object.entries(PAYOUT_METHOD)} />
      </FilterBar>
      <ErrorNote error={error} retry={reload} />
      <ListState loading={loading} data={data} items={items} empty={{ title: 'No payouts', text: 'Payouts you record appear here.' }}>
        <DataTable rows={items} rowKey={p => p.id} onRowClick={p => nav(`/payouts/${p.id}`)} columns={[
          { header: 'Payout', cell: p => <TitleCell title={<b>{p.reference}</b>} sub={dateTime(p.requested_at)} /> },
          ...(sellerId ? [] : [{ header: 'Seller', cell: (p: typeof items[number]) => p.seller_name }]),
          { header: 'Period', cell: p => periodLabel(p.period_starts_on, p.period_ends_on) },
          { header: 'Amount', align: 'right', cell: p => <b>{xaf(p.amount_xaf)}</b> },
          { header: 'Method', cell: p => <TitleCell title={PAYOUT_METHOD[p.method]} sub={p.destination_phone ?? undefined} /> },
          { header: 'Status', cell: p => <PayoutStatusPill status={p.status} /> },
        ]} />
      </ListState>
      <Pager {...pager} />
    </Panel>
  );
}
