import { Pill } from '../ui';
import { ROLE_LABEL, SELLER_ROLE_LABEL, TYPE_LABEL } from '../../utils/labels';
import { stockStatus } from '../../utils/stock';
import type { Eligibility, OperationType, Role, SellerRole } from '../../types/models';

export const StatusPill = ({ active }: { active: unknown }) => (active ? <Pill tone="green">Active</Pill> : <Pill tone="red">Inactive</Pill>);

const TYPE_TONE: Record<OperationType, string> = { FIXED_POS: 'blue', MOBILE: 'purple', MARKET_AGENT: 'gold' };
export const TypePill = ({ type }: { type?: OperationType }) => (type ? <Pill tone={TYPE_TONE[type]}>{TYPE_LABEL[type]}</Pill> : null);

const ROLE_TONE: Record<Role, string> = { ADMIN: 'navy', INVENTORY_MANAGER: 'purple', SELLER: 'gold' };
/** For sellers, shows the seller role (e.g. "Mobile Barista") when known. */
export const RolePill = ({ role, sellerRole }: { role: Role; sellerRole?: SellerRole | null }) =>
  <Pill tone={ROLE_TONE[role]}>{role === 'SELLER' && sellerRole ? SELLER_ROLE_LABEL[sellerRole] : ROLE_LABEL[role]}</Pill>;

export function EligibilityPill({ eligibility }: { eligibility?: Eligibility | null }) {
  if (!eligibility) return null;
  if (eligibility.eligible) return <Pill tone="green">Eligible</Pill>;
  if (eligibility.expired.length) return <Pill tone="red">Document expired</Pill>;
  if (eligibility.pending.length && !eligibility.missing.length) return <Pill tone="amber">Awaiting review</Pill>;
  return <Pill tone="amber">Documents missing</Pill>;
}

export function StockPill({ quantity, threshold }: { quantity: number; threshold: number }) {
  const [tone, label] = stockStatus(quantity, threshold);
  return <Pill tone={tone}>{label}</Pill>;
}

export const CompletedPill = () => <Pill tone="green">Completed</Pill>;
