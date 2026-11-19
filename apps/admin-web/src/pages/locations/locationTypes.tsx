import { Bike, Store, Users, type LucideIcon } from 'lucide-react';
import type { OperationType } from '../../types/models';

export const TYPE_ICON: Record<OperationType, LucideIcon> = { FIXED_POS: Store, MOBILE: Bike, MARKET_AGENT: Users };

export function TypeIcon({ type, size = 18 }: { type: OperationType; size?: number }) {
  const Icon = TYPE_ICON[type] ?? Store;
  return <span className="loc-ic" aria-hidden="true"><Icon size={size} /></span>;
}
