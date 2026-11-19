import type { PaymentStatus } from '../types/models';

export const PAYMENT_STATUS: Record<PaymentStatus, [string, string]> = {
  PENDING: ['Pending', 'amber'], CONFIRMED: ['Confirmed', 'green'], FAILED: ['Failed', 'red'], CANCELLED: ['Cancelled', 'grey'],
};
/** Who caused a payment event. */
export const EVENT_SOURCE: Record<string, string> = { SELLER: 'Seller', SYSTEM: 'System', PROVIDER: 'Provider', ADMIN: 'Administrator' };
export const EVENT_LABEL: Record<string, string> = { CASH_CONFIRMED: 'Cash received and confirmed by seller' };
