import { Fragment, type ReactNode } from 'react';

/** What each search type is called and where its result opens. Types a role cannot reach are simply never returned. */
export const SEARCH_TYPES: Record<string, { label: string; to: (id: string) => string }> = {
  PRODUCT: { label: 'Product', to: id => `/products/${id}` },
  CATEGORY: { label: 'Category', to: () => '/products/categories' },
  ADDON: { label: 'Add-on', to: () => '/products/addons' },
  OPERATION: { label: 'Location', to: id => `/locations/${id}` },
  SALE: { label: 'Sale', to: id => `/sales/${id}` },
  PAYMENT: { label: 'Payment', to: id => `/payments/${id}` },
  REFUND: { label: 'Refund', to: id => `/sales/refunds/${id}` },
  STAFF: { label: 'User', to: id => `/users/${id}` },
  CUSTOMER: { label: 'Customer', to: id => `/loyalty/members/${id}` },
  PAYOUT: { label: 'Payout', to: id => `/payouts/${id}` },
  PAYOUT_DESTINATION: { label: 'Payout number', to: () => '/payouts?tab=numbers' },
  TARGET: { label: 'Target', to: () => '/targets' },
  CASH_OUTFLOW: { label: 'Cash outflow', to: () => '/sales/shifts' },
  CASH_REMITTANCE: { label: 'Cash remittance', to: () => '/sales/shifts' },
  STOCKTAKE: { label: 'Stocktake', to: id => `/inventory/stocktakes/${id}` },
  ADJUSTMENT: { label: 'Stock adjustment', to: () => '/inventory' },
  REWARD: { label: 'Reward', to: () => '/loyalty' },
  EARNING_RULE: { label: 'Earning rule', to: () => '/loyalty' },
  REDEMPTION: { label: 'Redemption', to: () => '/loyalty' },
  POINTS_ADJUSTMENT: { label: 'Points adjustment', to: () => '/loyalty' },
};

export const searchLabel = (type: string) => SEARCH_TYPES[type]?.label ?? type;
export const searchTarget = (type: string, id: string) => (SEARCH_TYPES[type]?.to ?? (() => '/'))(id);

const unescape = (s: string) => s.replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&quot;/g, '"').replace(/&#x27;|&#39;/g, "'").replace(/&#x2F;/g, '/').replace(/&amp;/g, '&');

/** The server's highlighted title as React nodes: only <mark> is honoured, everything else is plain text (never injected as HTML). */
export function Highlighted({ html, fallback }: { html: string | null; fallback: string }): ReactNode {
  if (!html) return fallback;
  return html.split(/(<mark>.*?<\/mark>)/g).map((part, i) =>
    part.startsWith('<mark>') ? <mark key={i}>{unescape(part.slice(6, -7))}</mark> : <Fragment key={i}>{unescape(part)}</Fragment>);
}
