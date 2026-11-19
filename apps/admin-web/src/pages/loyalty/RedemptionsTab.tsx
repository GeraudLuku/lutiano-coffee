import { useState } from 'react';
import { loyaltyApi } from '../../api/services';
import { useAction } from '../../hooks/useAction';
import { usePagedQuery } from '../../hooks/usePagedQuery';
import { ErrorNote, Pager, Panel, ReasonModal, ListState } from '../../components/ui';
import { RedemptionsTable } from './RedemptionsTable';

export function RedemptionsTab() {
  const { data, items, error, loading, reload, pager } = usePagedQuery((page, size) => loyaltyApi.redemptions({ page, size }), []);
  const [reversing, setReversing] = useState<string | null>(null);
  const action = useAction();
  return (
    <Panel title="Reward Redemptions">
      <ErrorNote error={error} retry={reload} />
      <p className="muted small">Reversing a redemption returns the points to the batches they came from (and the item to stock, if it used stock).</p>
      <ListState loading={loading} data={data} items={items} empty={{ title: 'No redemptions yet' }}><RedemptionsTable rows={items} withCustomer onReverse={setReversing} /></ListState>
      <Pager {...pager} />
      {reversing && (
        <ReasonModal prompt={{ title: 'Reverse redemption', label: 'Reason', confirm: 'Reverse' }} busy={action.busy} error={action.error}
          onClose={() => { setReversing(null); action.setError(''); }}
          onSubmit={async reason => { if (await action.run(() => loyaltyApi.reverseRedemption(reversing, reason), 'Redemption reversed.')) { setReversing(null); reload(); } }} />
      )}
    </Panel>
  );
}
