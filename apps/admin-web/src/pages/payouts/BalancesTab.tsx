import { useState } from 'react';
import { Link } from 'react-router-dom';
import { HandCoins } from 'lucide-react';
import { payoutsApi } from '../../api/services';
import { usePagedQuery } from '../../hooks/usePagedQuery';
import { useDebouncedState } from '../../hooks/useDebounced';
import { Avatar, DataTable, ErrorNote, FilterBar, Pager, Panel, SearchInput, Switch, TitleCell, ListState } from '../../components/ui';
import { dateTime, xaf } from '../../utils/format';
import { SELLER_ROLE_LABEL } from '../../utils/labels';
import { RecordPayoutModal } from './RecordPayoutModal';

/** Every seller with finalised earnings: what is owed, what was paid and what is left, with a shortcut to record a payout. */
export function BalancesTab({ onChanged }: { onChanged: () => void }) {
  const [q, setQ, search] = useDebouncedState('');
  const [owedOnly, setOwedOnly] = useState(true);
  const [paying, setPaying] = useState<string | null>(null);
  const { data, items, error, loading, pager, reload } = usePagedQuery((page, size) => payoutsApi.balances({ q: search, owedOnly, page, size }), [search, owedOnly]);
  return (
    <Panel>
      <FilterBar>
        <SearchInput value={q} onChange={setQ} placeholder="Search seller…" />
        <Switch checked={owedOnly} onChange={setOwedOnly} label="Only sellers with a balance" />
      </FilterBar>
      <ErrorNote error={error} retry={reload} />
      <ListState loading={loading} data={data} items={items} empty={{ title: owedOnly ? 'No outstanding balances' : 'No finalised earnings yet', text: 'Balances appear when a month is finalised in Commissions → Earnings.' }}>
        <DataTable rows={items} rowKey={b => b.seller_id} columns={[
          { header: 'Seller', cell: b => <TitleCell lead={<Avatar name={b.full_name} url={b.image_url} />} title={<Link className="link" to={`/users/${b.seller_id}?tab=earnings`}>{b.full_name}</Link>}
            sub={b.seller_role ? SELLER_ROLE_LABEL[b.seller_role] : undefined} /> },
          { header: 'Earned (finalised)', align: 'right', cell: b => xaf(b.owed_xaf) },
          { header: 'Paid', align: 'right', cell: b => xaf(b.paid_xaf) },
          { header: 'In progress', align: 'right', cell: b => b.pending_xaf ? xaf(b.pending_xaf) : <span className="muted">—</span> },
          { header: 'Outstanding', align: 'right', cell: b => <b className={b.outstanding_xaf > 0 ? '' : 'muted'}>{xaf(b.outstanding_xaf)}</b> },
          { header: 'Last paid', cell: b => (b.last_paid_at ? dateTime(b.last_paid_at) : <span className="muted">Never</span>) },
          { header: '', align: 'right', cell: b => (
            <button className="btn sm" disabled={b.outstanding_xaf <= 0} onClick={() => setPaying(b.seller_id)}><HandCoins size={14} />Record payout</button>) },
        ]} />
      </ListState>
      <Pager {...pager} />
      {paying && <RecordPayoutModal sellerId={paying} onClose={() => setPaying(null)} onDone={() => { setPaying(null); reload(); onChanged(); }} />}
    </Panel>
  );
}
