import type { CashEntryType, OutflowStatus, RemittanceStatus, ShiftReviewStatus } from '../types/models';

/** [label, pill tone] */
export const REVIEW_STATUS: Record<ShiftReviewStatus, [string, string]> = {
  NOT_REQUIRED: ['Balanced', 'green'], PENDING_REVIEW: ['Needs review', 'amber'], RECOUNT_REQUESTED: ['Recount requested', 'blue'], RESOLVED: ['Reviewed', 'green'],
};
export const ENTRY_TYPE: Record<CashEntryType, [string, string]> = {
  OPENING_FLOAT: ['Opening float', 'grey'], CASH_SALE: ['Cash sale', 'green'], REFUND_PAID: ['Refund paid', 'purple'],
  OUTFLOW: ['Approved outflow', 'amber'], REMITTANCE: ['Remittance', 'blue'],
};
export const OUTFLOW_STATUS: Record<OutflowStatus, [string, string]> = {
  REQUESTED: ['Awaiting approval', 'amber'], APPROVED: ['Approved', 'green'], REJECTED: ['Rejected', 'red'], CANCELLED: ['Cancelled', 'grey'],
};
export const OUTFLOW_CATEGORY: Record<string, string> = {
  SUPPLIES: 'Supplies', TRANSPORT: 'Transport', MAINTENANCE: 'Maintenance', CLEANING: 'Cleaning', OTHER: 'Other',
};
export const REMITTANCE_STATUS: Record<RemittanceStatus, [string, string]> = {
  OUTSTANDING: ['Awaiting acknowledgement', 'amber'], ACKNOWLEDGED: ['Acknowledged', 'green'], DISPUTED: ['Disputed', 'red'],
};
export const REMITTANCE_METHOD = { HANDOVER: 'Cash handover', BANK_DEPOSIT: 'Bank deposit' } as const;
