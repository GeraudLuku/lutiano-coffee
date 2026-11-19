import { useState } from 'react';
import { loyaltyApi } from '../../api/services';
import { usePagedQuery } from '../../hooks/usePagedQuery';
import { ErrorNote, FilterBar, Pager, Panel, ListState } from '../../components/ui';
import { doualaDate } from '../../utils/format';
import { LEDGER_TYPE } from '../../utils/loyalty';
import type { LedgerType } from '../../types/models';
import { LedgerTable } from './LedgerTable';

/** The programme-wide points ledger. */
export function ActivityTab() {
  const [from, setFrom] = useState(doualaDate(-29));
  const [to, setTo] = useState(doualaDate());
  const [type, setType] = useState('');
  const { data, items, error, loading, reload, pager } = usePagedQuery((page, size) => loyaltyApi.ledger({ type, from, to, page, size }), [type, from, to]);
  return (
    <Panel title="Points Ledger">
      <FilterBar labelled>
        <label><span>From</span><input className="input" type="date" value={from} max={to} onChange={e => setFrom(e.target.value)} /></label>
        <label><span>To</span><input className="input" type="date" value={to} min={from} onChange={e => setTo(e.target.value)} /></label>
        <label><span>Type</span>
          <select className="input" value={type} onChange={e => setType(e.target.value)}>
            <option value="">All Types</option>{(Object.keys(LEDGER_TYPE) as LedgerType[]).map(t => <option key={t} value={t}>{LEDGER_TYPE[t][0]}</option>)}
          </select>
        </label>
      </FilterBar>
      <ErrorNote error={error} retry={reload} />
      <ListState loading={loading} data={data} items={items} empty={{ title: 'No points activity in this period' }}><LedgerTable rows={items} withCustomer /></ListState>
      <Pager {...pager} />
    </Panel>
  );
}
