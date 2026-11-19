import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Bike, MapPin, Plus, Store, Users } from 'lucide-react';
import { operationsApi } from '../../api/services';
import { useAuth } from '../../auth/AuthContext';
import { useLoad } from '../../hooks/useLoad';
import { fetchAll } from '../../api/pages';
import { usePagedQuery } from '../../hooks/usePagedQuery';
import { useDebouncedState } from '../../hooks/useDebounced';
import { DataTable, ErrorNote, FilterBar, Kpi, KpiRow, PageHeader, Pager, Panel, SearchInput, SelectFilter, TitleCell, ListState } from '../../components/ui';
import { StatusPill, TypePill } from '../../components/domain';
import { num, xaf } from '../../utils/format';
import { TYPE_LABEL } from '../../utils/labels';
import { TypeIcon } from './locationTypes';
import { LocationsMap } from './LocationMaps';

export function LocationsListPage() {
  const nav = useNavigate();
  const { can } = useAuth();
  const [q, setQ, search] = useDebouncedState('');
  const [type, setType] = useState('');
  const [status, setStatus] = useState('');
  const { data, items, summary, pager, error, loading, reload } = usePagedQuery(
    (page, size) => operationsApi.list({ q: search, type, active: status, page, size }), [search, type, status]);
  // The map shows every location matching the filters, not only the current page.
  const pins = useLoad(() => fetchAll((page, size) => operationsApi.list({ q: search, type, active: status, page, size })), [search, type, status]);
  const unplaced = (pins.data ?? []).filter(o => o.latitude == null || o.longitude == null).length;

  return (
    <>
      <PageHeader title="Sales Locations Overview" subtitle="Manage all sales operations (Fixed POS, Mobile Barista and Market Sales Agent)." crumbs={[['Sales Locations']]}
        actions={can('ADMIN') && <button className="btn gold" onClick={() => nav('/locations/new')}><Plus size={18} />Add Sales Location</button>} />
      <ErrorNote error={error} retry={reload} />
      <KpiRow>
        <Kpi tone="teal" icon={<MapPin />} value={num(summary?.total ?? 0)} label="Total Locations" />
        <Kpi tone="green" icon={<Store />} value={num(summary?.FIXED_POS ?? 0)} label="Fixed POS" />
        <Kpi tone="blue" icon={<Bike />} value={num(summary?.MOBILE ?? 0)} label="Mobile Baristas" />
        <Kpi tone="purple" icon={<Users />} value={num(summary?.MARKET_AGENT ?? 0)} label="Market Agents" />
      </KpiRow>
      <div className="grid-locations">
      <Panel className="map-panel">
        <LocationsMap locations={pins.data ?? []} onOpen={id => nav(`/locations/${id}`)} />
        {unplaced > 0 && <p className="muted small map-note">{num(unplaced)} location{unplaced === 1 ? ' has' : 's have'} no map position yet. Edit {unplaced === 1 ? 'it' : 'them'} to place the pin.</p>}
      </Panel>
      <Panel title={`Sales Locations (${num(data?.total ?? 0)})`}>
        <FilterBar>
          <SearchInput value={q} onChange={setQ} placeholder="Search name, code, city or seller…" />
          <SelectFilter label="Type" value={type} onChange={setType} allLabel="All Types" options={Object.entries(TYPE_LABEL)} />
          <SelectFilter label="Status" value={status} onChange={setStatus} allLabel="All Status" options={[['true', 'Active'], ['false', 'Inactive']]} />
        </FilterBar>
        <ListState loading={loading} data={data} items={items} empty={{ title: 'No sales locations', text: 'Add a location to start assigning sellers and stock.' }}>
          <DataTable rows={items} rowKey={o => o.id} onRowClick={o => nav(`/locations/${o.id}`)} columns={[
            { header: 'Location / Code', cell: o => <TitleCell lead={<TypeIcon type={o.type} />} title={o.name} sub={o.code} /> },
            { header: 'Type', cell: o => <TypePill type={o.type} /> },
            { header: 'City', cell: o => o.city ?? '—' },
            { header: "Today's Sales", align: 'right', cell: o => xaf(o.today_xaf ?? 0) },
            { header: 'Assigned Sellers', cell: o => o.seller_names ?? <span className="muted">Unassigned</span> },
            { header: 'Status', cell: o => <StatusPill active={o.active} /> },
          ]} />
        </ListState>
        <Pager {...pager} />
      </Panel>
      </div>
    </>
  );
}
