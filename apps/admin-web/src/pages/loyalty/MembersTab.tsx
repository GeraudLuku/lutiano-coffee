import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Plus } from 'lucide-react';
import { customersApi, loyaltyApi } from '../../api/services';
import { useLoad } from '../../hooks/useLoad';
import { usePagedQuery } from '../../hooks/usePagedQuery';
import { useDebouncedState } from '../../hooks/useDebounced';
import { DataTable, ErrorNote, FilterBar, LabelPill, Pager, Panel, SearchInput, TitleCell, ListState } from '../../components/ui';
import { date, num, xaf } from '../../utils/format';
import { pts } from '../../utils/loyalty';
import { CustomerFormModal } from './CustomerFormModal';
import { TierBadge } from './TierBadge';

export function MembersTab() {
  const nav = useNavigate();
  const [params, setParams] = useSearchParams();
  const tier = params.get('tier') ?? '';
  const tiers = useLoad(() => loyaltyApi.tiers(), []);
  const [q, setQ, search] = useDebouncedState('');
  const [adding, setAdding] = useState(false);
  const { data, items, error, loading, reload, pager } = usePagedQuery((page, size) => customersApi.list({ q: search, tier, page, size }), [tier, search]);
  const setTier = (v: string) => { const p = new URLSearchParams(params); if (v) p.set('tier', v); else p.delete('tier'); setParams(p, { replace: true }); };

  return (
    <Panel title={`Members (${num(data?.total ?? 0)})`} action={<button className="btn gold sm" onClick={() => setAdding(true)}><Plus size={15} />Add Member</button>}>
      <FilterBar>
        <SearchInput value={q} onChange={setQ} placeholder="Name, phone or member code…" />
        <select className="input" value={tier} onChange={e => setTier(e.target.value)} aria-label="Tier">
          <option value="">All Tiers</option>{(tiers.data ?? []).map(t => <option key={t.code} value={t.code}>{t.name}</option>)}
        </select>
      </FilterBar>
      <ErrorNote error={error} retry={reload} />
      <ListState loading={loading} data={data} items={items} empty={{ title: 'No members yet' }}>
        <DataTable rows={items} rowKey={c => c.id} onRowClick={c => nav(`/loyalty/members/${c.id}`)} columns={[
          { header: 'Member', cell: c => <TitleCell title={c.full_name} sub={`${c.code} · ${c.phone}`} /> },
          { header: 'Tier', cell: c => <TierBadge name={c.tier_name} color={c.tier_color} /> },
          { header: 'Points', align: 'right', cell: c => <b>{pts(c.points_balance)}</b> },
          { header: 'Purchases', align: 'right', cell: c => num(c.purchases) },
          { header: 'Lifetime Spend', align: 'right', cell: c => xaf(c.lifetime_spend_xaf) },
          { header: 'Last Purchase', cell: c => (c.last_purchase_at ? date(c.last_purchase_at) : '—') },
          { header: 'Status', cell: c => <LabelPill map={{ ACTIVE: ['Active', 'green'], SUSPENDED: ['Suspended', 'red'] }} value={c.status} /> },
        ]} />
      </ListState>
      <Pager {...pager} />
      {adding && <CustomerFormModal onClose={() => setAdding(false)} onDone={c => nav(`/loyalty/members/${c.id}`)} />}
    </Panel>
  );
}
