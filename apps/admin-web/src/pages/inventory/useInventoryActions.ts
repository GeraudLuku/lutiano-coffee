import type { ReasonText } from '../../components/ui';
import { useState } from 'react';
import { adjustmentsApi, stockRequestsApi, transfersApi } from '../../api/services';
import { useAuth } from '../../auth/AuthContext';
import { useAction } from '../../hooks/useAction';
import type { Adjustment, Allocation, StockRequest } from '../../types/models';

/** A reason prompt waiting for input (reject / cancel). */
export interface ReasonPrompt extends ReasonText { danger?: boolean; run: (note: string) => Promise<unknown>; success: string }

/**
 * Approval actions shared by the Approvals, Transfers and Adjustments tabs.
 * `canApprove` mirrors the server's second-approver rule so buttons can explain why they are disabled.
 */
export function useInventoryActions(secondApprover: boolean, reload: () => void) {
  const { session } = useAuth();
  const me = session!.user.id;
  const { busy, error, setError, run } = useAction();
  const [prompt, setPrompt] = useState<ReasonPrompt | null>(null);

  async function go(fn: () => Promise<unknown>, success: string) {
    if ((await run(fn, success)) !== undefined) reload();
  }

  return {
    busy, error, setError, prompt, closePrompt: () => setPrompt(null),
    canApprove: (creatorId: string | null | undefined) => !(secondApprover && creatorId === me),
    approveTransfer: (t: Allocation) => go(() => transfersApi.approve(t.id), `Approved: ${t.sent_quantity} × ${t.product_name} set aside for ${t.operation_name}.`),
    dispatchTransfer: (t: Allocation) => go(() => transfersApi.dispatch(t.id), `Dispatched to ${t.operation_name}. The location must confirm receipt.`),
    rejectTransfer: (t: Allocation) => setPrompt({ title: 'Reject transfer request', label: 'Reason for rejecting', confirm: 'Reject', danger: true, run: n => transfersApi.reject(t.id, n), success: 'Transfer request rejected.' }),
    cancelTransfer: (t: Allocation) => setPrompt({ title: 'Cancel transfer', label: 'Reason for cancelling', confirm: 'Cancel transfer', danger: true, run: n => transfersApi.cancel(t.id, n), success: t.status === 'APPROVED' ? 'Transfer cancelled; stock returned to central.' : 'Transfer cancelled.' }),
    approveRequest: (r: StockRequest) => go(() => stockRequestsApi.approve(r.id), `${r.reference} approved: ${r.item_count} products set aside for ${r.operation_name}.`),
    dispatchRequest: (r: StockRequest) => go(() => stockRequestsApi.dispatch(r.id), `${r.reference} dispatched to ${r.operation_name}. The location confirms each product on arrival.`),
    rejectRequest: (r: StockRequest) => setPrompt({ title: `Reject ${r.reference}`, label: 'Reason for rejecting (the seller sees it)', confirm: 'Reject', danger: true, run: n => stockRequestsApi.reject(r.id, n), success: `${r.reference} rejected.` }),
    cancelRequest: (r: StockRequest) => setPrompt({ title: `Cancel ${r.reference}`, label: 'Reason for cancelling', confirm: 'Cancel request', danger: true, run: n => stockRequestsApi.cancel(r.id, n), success: r.status === 'APPROVED' ? `${r.reference} cancelled; stock returned to central.` : `${r.reference} cancelled.` }),
    approveAdjustment: (a: Adjustment) => go(() => adjustmentsApi.approve(a.id), `Adjustment applied to ${a.location_name}.`),
    rejectAdjustment: (a: Adjustment) => setPrompt({ title: 'Reject adjustment', label: 'Reason for rejecting', confirm: 'Reject', danger: true, run: n => adjustmentsApi.reject(a.id, n), success: 'Adjustment rejected.' }),
    cancelAdjustment: (a: Adjustment) => go(() => adjustmentsApi.cancel(a.id), 'Adjustment request cancelled.'),
    submitPrompt: async (note: string) => {
      if (!prompt) return;
      if ((await run(() => prompt.run(note), prompt.success)) !== undefined) { setPrompt(null); reload(); }
    },
  };
}

export type InventoryActions = ReturnType<typeof useInventoryActions>;
