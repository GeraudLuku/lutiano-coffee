import { useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowDown, ArrowUp, Plus, SlidersHorizontal, Users } from 'lucide-react';
import { loyaltyApi } from '../../api/services';
import { usePagedQuery } from '../../hooks/usePagedQuery';
import { DataTable, ErrorNote, FilterBar, LabelPill, Pager, Panel, StatCard, StatRow, TitleCell, ListState } from '../../components/ui';
import { dateTime, doualaDate, num } from '../../utils/format';
import { ADJUSTMENT_REASON, signedPts } from '../../utils/loyalty';
import { AdjustModal } from './AdjustModal';

/** Design 8.4 Points Adjustments. */
export function AdjustmentsTab() {
  const [from, setFrom] = useState(doualaDate(-29));
  const [to, setTo] = useState(doualaDate());
  const [reason, setReason] = useState('');
  const { data, items, summary, error, loading, reload, pager } = usePagedQuery((page, size) => loyaltyApi.adjustments({ reason, from, to, page, size }), [reason, from, to]);
  const [adding, setAdding] = useState(false);
  return (
    <>
      <StatRow>
        <StatCard tone="green" icon={<SlidersHorizontal size={20} />} value={num(summary?.adjustments ?? 0)} label="Total Adjustments" />
        <StatCard tone="purple" icon={<ArrowUp size={20} />} value={signedPts(Number(summary?.added ?? 0))} label="Points Added" />
        <StatCard tone="red" icon={<ArrowDown size={20} />} value={signedPts(Number(summary?.deducted ?? 0))} label="Points Deducted" />
        <StatCard tone="blue" icon={<Users size={20} />} value={num(summary?.members ?? 0)} label="Members Affected" />
      </StatRow>
      <Panel title="Points Adjustments" action={<button className="btn gold sm" onClick={() => setAdding(true)}><Plus size={15} />New Adjustment</button>}>
        <FilterBar labelled>
          <label><span>From</span><input className="input" type="date" value={from} max={to} onChange={e => setFrom(e.target.value)} /></label>
          <label><span>To</span><input className="input" type="date" value={to} min={from} onChange={e => setTo(e.target.value)} /></label>
          <label><span>Reason</span>
            <select className="input" value={reason} onChange={e => setReason(e.target.value)}>
              <option value="">All Reasons</option>{Object.entries(ADJUSTMENT_REASON).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
            </select>
          </label>
        </FilterBar>
        <ErrorNote error={error} retry={reload} />
        <ListState loading={loading} data={data} items={items} empty={{ title: 'No adjustments in this period' }}>
          <DataTable rows={items} rowKey={a => a.id} columns={[
            { header: 'Date & Time', cell: a => dateTime(a.created_at) },
            { header: 'Member', cell: a => <Link className="link" to={`/loyalty/members/${a.customer_id}`}>{a.customer_name}</Link> },
            { header: 'Type', cell: a => <LabelPill map={{ ADD: ['Add', 'green'], DEDUCT: ['Deduct', 'red'] }} value={a.points > 0 ? 'ADD' : 'DEDUCT'} /> },
            { header: 'Points', align: 'right', cell: a => <b className={a.points < 0 ? 'warn' : 'ok'}>{signedPts(a.points)}</b> },
            { header: 'Reason', cell: a => <TitleCell title={ADJUSTMENT_REASON[a.reason] ?? a.reason} sub={a.note ?? undefined} /> },
            { header: 'Reference', cell: a => a.reference },
            { header: 'By', cell: a => a.created_by_name },
          ]} />
        </ListState>
        <Pager {...pager} />
      </Panel>
      {adding && <AdjustModal onClose={() => setAdding(false)} onDone={() => { setAdding(false); reload(); }} />}
      <p className="muted small">Adjustments post immediately and are permanent; to undo one, make an opposite adjustment. </p>
    </>
  );
}
