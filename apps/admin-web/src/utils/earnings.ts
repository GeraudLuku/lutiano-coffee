import type { CommissionBasis, EarningAdjustmentType, SellerTarget } from '../types/models';
import { num, xaf } from './format';

export const rateLabel = (basis: CommissionBasis, rate: number) =>
  basis === 'FIXED_PER_UNIT' ? `${xaf(rate)} / unit` : `${(rate / 100).toLocaleString('en-US', { maximumFractionDigits: 2 })}%`;

/** [label, pill tone] */
export const ADJUSTMENT_TYPE: Record<EarningAdjustmentType, [string, string]> = {
  REFUND_REVERSAL: ['Refund reversal', 'purple'], BONUS_REVERSAL: ['Bonus reversal', 'red'], RECOVERY: ['Recovery carried', 'amber'], MANUAL: ['Manual', 'blue'], CAMPAIGN: ['Campaign bonus', 'green'],
};
export const PERIOD_TYPE: Record<SellerTarget['period_type'], string> = { DAILY: 'Daily', WEEKLY: 'Weekly', MONTHLY: 'Monthly', CUSTOM: 'Custom' };

export const targetAmount = (t: Pick<SellerTarget, 'metric'>, v: number) => (t.metric === 'VALUE' ? xaf(v) : `${num(v)} units`);
export const bonusLabel = (t: Pick<SellerTarget, 'bonus_type' | 'bonus_xaf'>) => (t.bonus_type === 'FIXED' ? xaf(t.bonus_xaf) : `${xaf(t.bonus_xaf)} / unit`);
/** One commission cell for several roles: a single rate, a range when roles differ in percentage, or the roles' rates joined. */
export function commissionSummary(rows: { basis?: CommissionBasis; rate?: number }[]): string {
  const set = rows.filter((r): r is { basis: CommissionBasis; rate: number } => !!r.basis && r.rate != null);
  if (!set.length) return 'No rule';
  const labels = [...new Set(set.map(r => rateLabel(r.basis, r.rate)))];
  if (labels.length === 1) return labels[0]!;
  const bases = new Set(set.map(r => r.basis));
  if (bases.size === 1) {
    const rates = set.map(r => r.rate), lo = rateLabel(set[0]!.basis, Math.min(...rates)), hi = rateLabel(set[0]!.basis, Math.max(...rates));
    return set[0]!.basis === 'PERCENT' ? `${lo.replace('%', '')} – ${hi}` : `${lo} – ${hi}`;
  }
  return labels.join(' / ');
}
export const signedXaf = (v: number) => `${v > 0 ? '+' : ''}${xaf(v)}`;
