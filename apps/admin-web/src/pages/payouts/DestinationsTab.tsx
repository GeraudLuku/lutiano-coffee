import { useState } from 'react';
import { Check, Plus, X } from 'lucide-react';
import { payoutsApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { usePagedQuery } from '../../hooks/usePagedQuery';
import { useDebouncedState } from '../../hooks/useDebounced';
import { DataTable, ErrorNote, FilterBar, Note, Pager, Panel, ReasonModal, SearchInput, SelectFilter, TitleCell, ListState, type ReasonText } from '../../components/ui';
import { dateTime } from '../../utils/format';
import { DESTINATION_STATUS, PAYOUT_METHOD } from '../../utils/payouts';
import type { PayoutDestination } from '../../types/models';
import { DestinationModal } from './DestinationModal';
import { VerifyNumberModal } from './VerifyNumberModal';
import { DestinationStatusPill } from './PayoutPills';

/** Mobile money numbers sellers are paid to. A new number must be verified by an administrator other than the one who added it. */
export function DestinationsTab({ onChanged }: { onChanged: () => void }) {
  const [q, setQ, search] = useDebouncedState('');
  const [status, setStatus] = useState('');
  const [provider, setProvider] = useState('');
  const [adding, setAdding] = useState(false);
  const [rejecting, setRejecting] = useState<PayoutDestination | null>(null);
  const [verifying, setVerifying] = useState<PayoutDestination | null>(null);
  const { data, items, summary, error, loading, pager, reload } = usePagedQuery(
    (page, size) => payoutsApi.destinations({ status, provider, q: search, page, size }), [status, provider, search]);
  const action = useAction();
  const changed = () => { reload(); onChanged(); };
  const prompt: ReasonText = { title: 'Reject number', label: 'Why is this number rejected?', confirm: 'Reject Number' };

  return (
    <Panel action={<button className="btn gold sm" onClick={() => setAdding(true)}><Plus size={15} />Add Number</button>}>
      <Note>Sellers are paid by cash, MTN MoMo or Orange Money. A mobile money number can be paid to only after it is verified: a code is sent to the number by SMS and the seller reads it out.
        {summary ? ` ${summary.pending} awaiting verification.` : ''}</Note>
      <FilterBar>
        <SearchInput value={q} onChange={setQ} placeholder="Seller, number or account holder…" />
        <SelectFilter label="Status" value={status} onChange={setStatus} allLabel="All Statuses" options={Object.entries(DESTINATION_STATUS).map(([k, v]) => [k, v[0]] as [string, string])} />
        <SelectFilter label="Provider" value={provider} onChange={setProvider} allLabel="All Providers" options={[['MTN_MOMO', PAYOUT_METHOD.MTN_MOMO], ['ORANGE_MONEY', PAYOUT_METHOD.ORANGE_MONEY]]} />
      </FilterBar>
      <ErrorNote error={error || action.error} retry={reload} />
      <ListState loading={loading} data={data} items={items} empty={{ title: 'No payout numbers', text: "Add a seller's MTN MoMo or Orange Money number to pay them by mobile money." }}>
        <DataTable rows={items} rowKey={d => d.id} columns={[
          { header: 'Seller', cell: d => <TitleCell title={d.seller_name} sub={`Added ${dateTime(d.created_at)} by ${d.created_by_name}`} /> },
          { header: 'Provider', cell: d => PAYOUT_METHOD[d.provider] },
          { header: 'Number', cell: d => <TitleCell title={<b>{d.phone}</b>} sub={d.account_name} /> },
          { header: 'Status', cell: d => <TitleCell title={<DestinationStatusPill status={d.status} />}
            sub={d.verified_at ? `${d.status === 'REJECTED' ? 'Rejected' : 'Verified'} ${dateTime(d.verified_at)} by ${d.verified_by_name}${d.status === 'REJECTED' && d.note ? `: ${d.note}` : ''}` : undefined} /> },
          { header: '', align: 'right', cell: d => d.status !== 'PENDING' ? null : (
            <span className="row-actions">
              <button className="btn sm" onClick={() => setRejecting(d)}><X size={14} />Reject</button>
              <button className="btn gold sm" onClick={() => setVerifying(d)}><Check size={14} />Verify</button>
            </span>) },
        ]} />
      </ListState>
      <Pager {...pager} />
      {verifying && <VerifyNumberModal destination={verifying} onClose={() => setVerifying(null)} onDone={() => { setVerifying(null); changed(); }} />}
      {adding && <DestinationModal onClose={() => setAdding(false)} onDone={() => { setAdding(false); changed(); }} />}
      {rejecting && <ReasonModal prompt={prompt} busy={action.busy} error={action.error}
        onSubmit={async note => { if (await action.run(() => payoutsApi.rejectDestination(rejecting.id, note), 'Number rejected.') !== undefined) { setRejecting(null); changed(); } }}
        onClose={() => { setRejecting(null); action.setError(''); }} />}
    </Panel>
  );
}
