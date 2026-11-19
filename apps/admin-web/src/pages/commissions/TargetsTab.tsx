import { useState } from 'react';
import { Plus, X } from 'lucide-react';
import { staffApi, targetsApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { useLoad } from '../../hooks/useLoad';
import { usePagedQuery } from '../../hooks/usePagedQuery';
import { ErrorNote, FilterBar, Pager, Panel, ReasonModal, ListState } from '../../components/ui';
import { TargetModal } from './TargetModal';
import { TargetsTable } from './TargetsTable';

export function TargetsTab() {
  const sellers = useLoad(() => staffApi.all().then(all => all.filter(s => s.role === 'SELLER')), []);
  const [sellerId, setSellerId] = useState('');
  const [state, setState] = useState('CURRENT');
  const { data, items, pager, error, loading, reload } = usePagedQuery(
    (page, size) => targetsApi.list({ sellerId, state, status: state ? 'ACTIVE' : undefined, page, size }), [sellerId, state]);
  const [adding, setAdding] = useState(false);
  const [cancelling, setCancelling] = useState<string | null>(null);
  const action = useAction();

  return (
    <Panel title="Sales Targets" action={<button className="btn gold sm" onClick={() => setAdding(true)}><Plus size={15} />New Target</button>}>
      <FilterBar>
        <select className="input" value={sellerId} onChange={e => setSellerId(e.target.value)} aria-label="Seller">
          <option value="">All Sellers</option>{(sellers.data ?? []).map(s => <option key={s.id} value={s.id}>{s.full_name}</option>)}
        </select>
        <select className="input" value={state} onChange={e => setState(e.target.value)} aria-label="Period">
          <option value="CURRENT">In progress</option><option value="UPCOMING">Upcoming</option><option value="ENDED">Ended</option><option value="">All, including cancelled</option>
        </select>
      </FilterBar>
      <ErrorNote error={error} retry={reload} />
      <ListState loading={loading} data={data} items={items} empty={{ title: 'No targets', text: 'Set a value or quantity target for a seller.' }}>
        <TargetsTable rows={items} withSeller actions={t => (t.status === 'ACTIVE' && t.bonus_paid_xaf == null
          ? <button className="icon-btn danger" aria-label={`Cancel ${t.name}`} onClick={() => setCancelling(t.id)}><X size={15} /></button> : null)} />
      </ListState>
      <Pager {...pager} />
      {adding && <TargetModal onClose={() => setAdding(false)} onDone={() => { setAdding(false); reload(); }} />}
      {cancelling && (
        <ReasonModal prompt={{ title: 'Cancel target', label: 'Reason', confirm: 'Cancel Target' }} busy={action.busy} error={action.error}
          onClose={() => { setCancelling(null); action.setError(''); }}
          onSubmit={async reason => { if (await action.run(() => targetsApi.cancel(cancelling, reason), 'Target cancelled.')) { setCancelling(null); reload(); } }} />
      )}
    </Panel>
  );
}
