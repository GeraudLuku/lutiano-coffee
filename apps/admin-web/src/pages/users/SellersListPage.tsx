import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AlertTriangle, BadgeCheck, Plus, ShoppingCart, Timer, Users } from 'lucide-react';
import { staffApi } from '../../api/services';
import { usePagedQuery } from '../../hooks/usePagedQuery';
import { useDebouncedState } from '../../hooks/useDebounced';
import { Avatar, DataTable, ErrorNote, FilterBar, Kpi, KpiRow, PageHeader, Pager, Panel, Pill, SearchInput, SelectFilter, TitleCell, ListState } from '../../components/ui';
import { EligibilityPill, RolePill, StatusPill } from '../../components/domain';
import { ago, num, xaf } from '../../utils/format';
import { SELLER_ROLE_LABEL } from '../../utils/labels';

/** A04 seller list: role and channel, locations, eligibility, recent performance and open shifts. */
export function SellersListPage() {
  const nav = useNavigate();
  const [q, setQ, search] = useDebouncedState('');
  const [role, setRole] = useState('');
  const [eligible, setEligible] = useState('');
  const [status, setStatus] = useState('');
  const { data, items, summary, pager, error, loading, reload } = usePagedQuery(
    (page, size) => staffApi.sellers({ q: search, sellerRole: role, eligible, active: status, page, size }), [search, role, eligible, status]);

  return (
    <>
      <PageHeader title="Sellers" subtitle="Fixed POS baristas, mobile baristas and market sales agents: assignments, eligibility and performance."
        crumbs={[['Users', '/users'], ['Sellers']]}
        actions={<button className="btn gold" onClick={() => nav('/users/new?role=SELLER')}><Plus size={18} />Add Seller</button>} />
      <ErrorNote error={error} retry={reload} />
      <KpiRow>
        <Kpi tone="navy" icon={<Users />} value={num(summary?.active ?? 0)} label="Active Sellers" />
        <Kpi tone="green" icon={<BadgeCheck />} value={num(summary?.eligible ?? 0)} label="Eligible" />
        <Kpi tone="gold" icon={<AlertTriangle />} value={num(summary?.attention ?? 0)} label="Need Attention" />
        <Kpi tone="purple" icon={<Timer />} value={num(summary?.open_shifts ?? 0)} label="Cash Shifts Open" />
      </KpiRow>
      <Panel>
        <FilterBar>
          <SearchInput value={q} onChange={setQ} placeholder="Search name, email, phone, area or location…" />
          <SelectFilter label="Seller role" value={role} onChange={setRole} allLabel="All Roles" options={Object.entries(SELLER_ROLE_LABEL)} />
          <SelectFilter label="Eligibility" value={eligible} onChange={setEligible} allLabel="Any Eligibility" options={[['true', 'Eligible'], ['false', 'Not eligible']]} />
          <SelectFilter label="Status" value={status} onChange={setStatus} allLabel="All Status" options={[['true', 'Active'], ['false', 'Inactive']]} />
        </FilterBar>
        <ListState loading={loading} data={data} items={items} empty={{ title: 'No sellers found', text: 'Adjust the filters or add a seller.' }}>
          <DataTable rows={items} rowKey={s => s.id} onRowClick={s => nav(`/users/${s.id}`)} columns={[
            { header: 'Seller', cell: s => <TitleCell lead={<Avatar name={s.full_name} url={s.image_url} />} title={s.full_name} sub={[s.phone, s.area].filter(Boolean).join(' · ') || s.email} /> },
            { header: 'Role', cell: s => <RolePill role={s.role} sellerRole={s.seller_role} /> },
            { header: 'Sales Locations', cell: s => (
              <TitleCell title={s.operation_names ?? <span className="muted">Unassigned</span>}
                sub={s.mismatched_operations ? <span className="warn">{s.mismatched_operations} not matching role</span> : undefined} />) },
            { header: 'Eligibility', cell: s => <EligibilityPill eligibility={s.eligibility} /> },
            { header: '30-day sales', align: 'right', cell: s => <TitleCell title={xaf(s.revenue_30d)} sub={`${num(s.sales_30d)} sales`} /> },
            { header: 'Last sale', cell: s => (s.last_sale_at ? ago(s.last_sale_at) : 'Never') },
            { header: 'Shift', cell: s => (s.open_shift ? <Pill tone="green"><ShoppingCart size={12} />Open</Pill> : '—') },
            { header: 'Status', cell: s => <StatusPill active={s.active} /> },
          ]} />
        </ListState>
        <Pager {...pager} />
      </Panel>
    </>
  );
}
