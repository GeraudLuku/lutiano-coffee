import type { LedgerType, RewardType, RuleMethod } from '../types/models';

/** [label, pill tone] */
export const LEDGER_TYPE: Record<LedgerType, [string, string]> = {
  EARNED: ['Earned', 'green'], REDEEMED: ['Redeemed', 'red'], REDEMPTION_REVERSED: ['Redemption reversed', 'blue'],
  REFUND_REVERSAL: ['Refund reversal', 'purple'], EXPIRED: ['Expired', 'grey'], ADJUSTED: ['Adjustment', 'amber'],
};
export const RULE_METHOD: Record<RuleMethod, [string, string]> = {
  RATE: ['Points per amount spent', 'Replaces the default rate for a product or category'],
  PER_UNIT: ['Bonus per unit', 'Extra points for each unit sold'],
  MULTIPLIER: ['Promotion multiplier', 'Multiplies base points on promotion-eligible products'],
  FIRST_PURCHASE: ['First purchase bonus', 'One-time points on a member’s first earning purchase'],
};
export const REWARD_TYPE: Record<RewardType, [string, string]> = {
  FREE_PRODUCT: ['Free Product', 'purple'], PRODUCT_EXCHANGE: ['Product Exchange', 'red'], MERCHANDISE: ['Merchandise', 'navy'], DISCOUNT_VOUCHER: ['Discount', 'gold'],
};
export const ADJUSTMENT_REASON: Record<string, string> = {
  GOODWILL: 'Goodwill (service issue)', CORRECTION: 'Correction', DUPLICATE_POINTS: 'Duplicate points', PROMOTION: 'Promotion campaign',
  FRAUD: 'Fraudulent activity', PROGRAM_REVIEW: 'Loyalty programme review', OTHER: 'Other',
};

export const pts = (n: number) => `${Math.round(n).toLocaleString('en-US')} pts`;
export const signedPts = (n: number) => `${n > 0 ? '+' : ''}${pts(n)}`;
