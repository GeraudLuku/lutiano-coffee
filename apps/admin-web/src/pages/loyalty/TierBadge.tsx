import { Crown } from 'lucide-react';

/** Tier name with its colour (design: crown icons per tier). */
export const TierBadge = ({ name, color }: { name: string | null; color: string | null }) =>
  name ? <span className="tier-badge"><Crown size={14} style={{ color: color ?? undefined }} aria-hidden="true" />{name}</span> : <span className="muted">—</span>;
