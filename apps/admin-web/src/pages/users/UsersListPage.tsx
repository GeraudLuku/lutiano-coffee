import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, Shield, ShoppingCart, UserCheck, Users } from 'lucide-react';
import { staffApi } from '../../api/services';
import { usePagedQuery } from '../../hooks/usePagedQuery';
import { useDebouncedState } from '../../hooks/useDebounced';
import { Avatar, DataTable, ErrorNote, FilterBar, Kpi, KpiRow, PageHeader, Pager, Panel, SearchInput, SelectFilter, TitleCell, ListState } from '../../components/ui';
import { RolePill, StatusPill } from '../../components/domain';
import { ago, num } from '../../utils/format';
import { ROLE_LABEL } from '../../utils/labels';
import type { Role } from '../../types/models';

export function UsersListPage() {
  const nav = useNavigate();
  const [q, setQ, search] = useDebouncedState('');
  const [role, setRole] = useState('');
  const [status, setStatus] = useState('');
  const { data, items, summary, pager, error, loading, reload } = usePagedQuery(
    (page, size) => staffApi.list({ q: search, role, active: status, page, size }), [search, role, status]);
  const count = (r: Role) => summary?.[r] ?? 0;

  return (
    <>
      <PageHeader title="Users" subtitle="Manage staff accounts, roles and sales location assignments." crumbs={[['Users']]}
        actions={<button className="btn gold" onClick={() => nav('/users/new')}><Plus size={18} />Add User</button>} />
      <ErrorNote error={error} retry={reload} />
      <KpiRow>
        <Kpi tone="navy" icon={<Users />} value={num(summary?.total ?? 0)} label="Total Users" />
        <Kpi tone="teal" icon={<Shield />} value={num(count('ADMIN'))} label="Administrators" />
        <Kpi tone="blue" icon={<UserCheck />} value={num(count('INVENTORY_MANAGER'))} label="Inventory Managers" />
        <Kpi tone="purple" icon={<ShoppingCart />} value={num(count('SELLER'))} label="Sellers" />
      </KpiRow>
      <Panel>
        <FilterBar>
          <SearchInput value={q} onChange={setQ} placeholder="Search name, email or phone…" />
          <SelectFilter label="Role" value={role} onChange={setRole} allLabel="All Roles" options={Object.entries(ROLE_LABEL)} />
          <SelectFilter label="Status" value={status} onChange={setStatus} allLabel="All Status" options={[['true', 'Active'], ['false', 'Inactive']]} />
        </FilterBar>
        <ListState loading={loading} data={data} items={items} empty={{ title: 'No users found', text: 'Adjust the filters or add a user.' }}>
          <DataTable rows={items} rowKey={u => u.id} onRowClick={u => nav(`/users/${u.id}`)} columns={[
            { header: 'User', cell: u => <TitleCell lead={<Avatar name={u.full_name} url={u.image_url} />} title={u.full_name} sub={u.email} /> },
            { header: 'Role', cell: u => <RolePill role={u.role} sellerRole={u.seller_role} /> },
            { header: 'Phone', cell: u => u.phone ?? '—' },
            { header: 'Locations', align: 'right', cell: u => (u.role === 'SELLER' ? num(u.operation_count) : '—') },
            { header: 'Status', cell: u => <StatusPill active={u.active} /> },
            { header: 'Last Login', cell: u => (u.last_login_at ? ago(u.last_login_at) : 'Never') },
          ]} />
        </ListState>
        <Pager {...pager} />
      </Panel>
    </>
  );
}
