import type { DeliveryStatus } from '../types/models';

/** [label, pill tone] */
export const DELIVERY_STATUS: Record<DeliveryStatus, [string, string]> = {
  QUEUED: ['Queued', 'blue'], SENT: ['Sent', 'green'], LOGGED: ['Dev: logged', 'purple'], FAILED: ['Failed', 'red'], SKIPPED: ['Skipped', 'grey'],
};

export const AUDIENCE_LABEL = { PERSON: 'The person concerned', APPROVERS_FINANCE: 'Approval alert recipients (administrators)', APPROVERS_STOCK: 'Approval alert recipients (administrators, inventory managers)', STOCK_ALERT: 'Stock approvers and the sellers at that location' } as const;

/** One SMS holds 160 plain characters, or 70 when the text has accents, which French almost always does. */
export const smsParts = (text: string) => {
  const unicode = /[^\x20-\x7e\n]/.test(text);
  const [single, multi] = unicode ? [70, 67] : [160, 153];
  return text.length <= single ? 1 : Math.ceil(text.length / multi);
};

/** The screen a message is about, when it has one. */
export function notificationLink(type: string | null, id: string | null): string | null {
  if (!type) return null;
  switch (type) {
    case 'payout': return id ? `/payouts/${id}` : '/payouts';
    case 'payout_destination': return '/payouts?tab=numbers';
    case 'refund': return id ? `/sales/refunds/${id}` : '/sales/refunds';
    case 'cash_outflow': case 'cash_remittance': case 'cash_shift': return '/sales/shifts';
    case 'allocation': case 'adjustment': return '/inventory?tab=approvals';
    case 'stock_alert': return '/inventory?tab=reorder';
    case 'earning_period': return id ? `/commissions/periods/${id}` : '/commissions?tab=earnings';
    case 'seller_target': return '/targets';
    default: return null;
  }
}
