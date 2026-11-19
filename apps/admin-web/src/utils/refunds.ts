import type { RefundReason, RefundStatus } from '../types/models';

/** [label, pill tone]. "Approved" means approved but not yet paid. */
export const REFUND_STATUS: Record<RefundStatus, [string, string]> = {
  REQUESTED: ['Pending approval', 'amber'], APPROVED: ['Approved · unpaid', 'blue'], PAID: ['Completed', 'green'],
  REJECTED: ['Rejected', 'red'], CANCELLED: ['Cancelled', 'grey'],
};

export const REFUND_REASON: Record<RefundReason, string> = {
  DAMAGED_PRODUCT: 'Damaged product', WRONG_ITEM: 'Wrong item / variant', PRODUCT_DEFECT: 'Product defect', CUSTOMER_REQUEST: 'Customer request',
  SERVICE_ISSUE: 'Service issue', PRICING_ERROR: 'Pricing error', OTHER: 'Other',
};

export const REFUND_EVENT: Record<string, [string, string]> = {
  REQUESTED: ['Refund requested', 'dot-gold'], APPROVED: ['Approved', 'dot-blue'], REJECTED: ['Rejected', ''], CANCELLED: ['Cancelled', ''],
  PAID: ['Cash refund paid', 'dot-green'], RECIPIENT_ACKNOWLEDGED: ['Recipient acknowledged receipt', 'dot-green'],
  EVIDENCE_ADDED: ['Evidence added', 'dot-purple'], STOCK_RETURN_REQUESTED: ['Returned stock submitted for approval', 'dot-purple'],
};

export const FUND_SOURCE: Record<'SHIFT' | 'ADVANCE', string> = { SHIFT: 'Cash shift drawer', ADVANCE: 'Authorised advance' };
