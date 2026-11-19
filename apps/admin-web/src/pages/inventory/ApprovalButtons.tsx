import { Check, Send, X } from 'lucide-react';
import type { Adjustment, Allocation, StockRequest } from '../../types/models';
import type { InventoryActions } from './useInventoryActions';

const SELF_HINT = 'You created this request. Another manager must approve it.';

/** Context-aware buttons for a transfer, by status. */
export function TransferActions({ t, actions }: { t: Allocation; actions: InventoryActions }) {
  const can = actions.canApprove(t.created_by);
  if (t.status === 'PENDING_APPROVAL') return (
    <div className="row-actions">
      <button className="btn sm gold" disabled={!can || actions.busy} title={can ? undefined : SELF_HINT} onClick={() => actions.approveTransfer(t)}><Check size={15} />Approve</button>
      <button className="btn sm" disabled={actions.busy} onClick={() => actions.rejectTransfer(t)}><X size={15} />Reject</button>
    </div>
  );
  if (t.status === 'APPROVED') return (
    <div className="row-actions">
      <button className="btn sm gold" disabled={actions.busy} onClick={() => actions.dispatchTransfer(t)}><Send size={15} />Dispatch</button>
      <button className="btn sm" disabled={actions.busy} onClick={() => actions.cancelTransfer(t)}>Cancel</button>
    </div>
  );
  return null;
}

/** Context-aware buttons for a stock request, by status: it is decided as a whole. */
export function RequestActions({ r, actions }: { r: StockRequest; actions: InventoryActions }) {
  const can = actions.canApprove(r.requested_by);
  if (r.status === 'PENDING_APPROVAL') return (
    <div className="row-actions">
      <button className="btn sm gold" disabled={!can || actions.busy} title={can ? undefined : SELF_HINT} onClick={() => actions.approveRequest(r)}><Check size={15} />Approve</button>
      <button className="btn sm" disabled={actions.busy} onClick={() => actions.rejectRequest(r)}><X size={15} />Reject</button>
    </div>
  );
  if (r.status === 'APPROVED') return (
    <div className="row-actions">
      <button className="btn sm gold" disabled={actions.busy} onClick={() => actions.dispatchRequest(r)}><Send size={15} />Dispatch</button>
      <button className="btn sm" disabled={actions.busy} onClick={() => actions.cancelRequest(r)}>Cancel</button>
    </div>
  );
  return null;
}

export function AdjustmentActions({ a, actions }: { a: Adjustment; actions: InventoryActions }) {
  if (a.status !== 'PENDING_APPROVAL') return null;
  const can = actions.canApprove(a.requested_by);
  return (
    <div className="row-actions">
      <button className="btn sm gold" disabled={!can || actions.busy} title={can ? undefined : SELF_HINT} onClick={() => actions.approveAdjustment(a)}><Check size={15} />Approve</button>
      <button className="btn sm" disabled={actions.busy} onClick={() => actions.rejectAdjustment(a)}><X size={15} />Reject</button>
      {!can && <button className="btn sm" disabled={actions.busy} onClick={() => actions.cancelAdjustment(a)}>Withdraw</button>}
    </div>
  );
}

export const selfApprovalHint = SELF_HINT;
