import type { DestinationStatus, PayoutMethod, PayoutStatus } from '../types/models';

/** [label, pill tone]. "Approved" means approved and waiting to be paid. */
export const PAYOUT_STATUS: Record<PayoutStatus, [string, string]> = {
  PENDING_APPROVAL: ['Pending approval', 'amber'], APPROVED: ['Approved · to pay', 'blue'], PAID: ['Paid', 'green'],
  REJECTED: ['Rejected', 'red'], CANCELLED: ['Cancelled', 'grey'], REVERSAL_PENDING: ['Reversal pending', 'purple'], REVERSED: ['Reversed', 'grey'],
};

export const PAYOUT_METHOD: Record<PayoutMethod, string> = { CASH: 'Cash', MTN_MOMO: 'MTN MoMo', ORANGE_MONEY: 'Orange Money' };

export const DESTINATION_STATUS: Record<DestinationStatus, [string, string]> = {
  PENDING: ['Awaiting verification', 'amber'], VERIFIED: ['Verified', 'green'], REJECTED: ['Rejected', 'red'], REPLACED: ['Replaced', 'grey'],
};

export const PAYOUT_EVENT: Record<string, [string, string]> = {
  RECORDED: ['Payout recorded', 'dot-gold'], APPROVED: ['Approved', 'dot-blue'], REJECTED: ['Rejected', ''], CANCELLED: ['Cancelled', ''],
  PAID: ['Paid', 'dot-green'], EVIDENCE_ADDED: ['Evidence added', 'dot-purple'], REVERSAL_REQUESTED: ['Reversal requested', 'dot-purple'],
  REVERSED: ['Reversed', ''], REVERSAL_REJECTED: ['Reversal refused', 'dot-blue'],
};

/** "1 Oct – 31 Oct 2026" style label for a monthly period. */
export const periodLabel = (startsOn: string, endsOn: string) => {
  const d = (s: string, y: boolean) => new Date(`${s}T12:00:00`).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', ...(y ? { year: 'numeric' } : {}) });
  return `${d(startsOn, false)} – ${d(endsOn, true)}`;
};
