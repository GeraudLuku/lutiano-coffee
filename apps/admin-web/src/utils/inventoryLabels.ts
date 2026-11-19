import type { AdjustmentReason, AdjustmentStatus, MovementType, ReceiptSource, StocktakeStatus, TransferStatus } from '../types/models';

/** [label, pill tone] per status. */
export const TRANSFER_STATUS: Record<TransferStatus, [string, string]> = {
  PENDING_APPROVAL: ['Awaiting approval', 'amber'], APPROVED: ['Approved – to dispatch', 'purple'], IN_TRANSIT: ['In transit', 'blue'],
  RECEIVED: ['Received', 'green'], REJECTED: ['Rejected', 'red'], CANCELLED: ['Cancelled', 'grey'],
};
export const ADJUSTMENT_STATUS: Record<AdjustmentStatus, [string, string]> = {
  PENDING_APPROVAL: ['Awaiting approval', 'amber'], APPLIED: ['Applied', 'green'], REJECTED: ['Rejected', 'red'], CANCELLED: ['Cancelled', 'grey'],
};
export const STOCKTAKE_STATUS: Record<StocktakeStatus, [string, string]> = {
  COUNTING: ['Counting', 'blue'], SUBMITTED: ['Awaiting approval', 'amber'], APPROVED: ['Approved', 'green'], CANCELLED: ['Cancelled', 'grey'],
};

/** Reason label and which direction it allows: -1 remove only, 1 add only, 0 either. Mirrors the server. */
export const ADJUSTMENT_REASON: Record<AdjustmentReason, [string, -1 | 0 | 1]> = {
  DAMAGED: ['Damaged', -1], EXPIRED: ['Expired', -1], MISSING: ['Missing / lost', -1], FOUND: ['Found', 1],
  COUNT_CORRECTION: ['Count correction', 0], RETURN_TO_CENTRAL: ['Return to central stock', -1],
  CUSTOMER_RETURN: ['Customer return (refund)', 1], OTHER: ['Other', 0],
};

export const MOVEMENT_TYPE: Record<MovementType, [string, string]> = {
  OPENING_BALANCE: ['Opening balance', 'grey'], RECEIPT: ['Supplier receipt', 'green'], ALLOCATION_OUT: ['Transfer approved (out)', 'purple'],
  ALLOCATION_RELEASED: ['Transfer cancelled (back)', 'grey'], TRANSFER_IN: ['Transfer received', 'blue'], SALE: ['Sale', 'gold'],
  ADJUSTMENT: ['Adjustment', 'amber'], STOCKTAKE: ['Stock count', 'amber'], RETURN_OUT: ['Returned to central', 'purple'], RETURN_IN: ['Return received', 'green'],
  REWARD_OUT: ['Reward redeemed', 'purple'], REWARD_RETURN: ['Reward reversed (back)', 'grey'],
};

export const RECEIPT_SOURCE: Record<ReceiptSource, string> = { SUPPLIER: 'Supplier delivery', OPENING_BALANCE: 'Opening balance' };

/** Value used for "central stock" in location filters. */
export const CENTRAL = 'CENTRAL';

export const signed = (n: number) => (n > 0 ? `+${n}` : String(n));
