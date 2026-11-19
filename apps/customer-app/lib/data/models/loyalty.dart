import 'package:lutiano_core/lutiano_core.dart';

class Tier {
  const Tier(
      {required this.code,
      required this.name,
      required this.minSpendXaf,
      required this.multiplierPct,
      required this.color,
      this.benefits,
      this.current = false});
  factory Tier.fromJson(Map<String, dynamic> j) => Tier(
      code: asString(j['code']),
      name: asString(j['name']),
      minSpendXaf: asInt(j['min_annual_spend_xaf']),
      multiplierPct: asInt(j['multiplier_pct']),
      color: asString(j['color']),
      benefits: asStringOrNull(j['benefits']),
      current: asBool(j['current']));
  final String code, name, color;
  final int minSpendXaf, multiplierPct;
  final String? benefits;
  final bool current;
}

/// Points, tier and progress, as shown on the Rewards tab.
class LoyaltyOverview {
  const LoyaltyOverview(
      {required this.balance,
      required this.annualSpendXaf,
      required this.remainingToNextXaf,
      required this.expiringPoints,
      required this.expiryNoticeDays,
      required this.rewardsAvailable,
      required this.minRedeem,
      this.tier,
      this.nextTier});
  factory LoyaltyOverview.fromJson(Map<String, dynamic> j) => LoyaltyOverview(
      balance: asInt(j['points_balance']),
      annualSpendXaf: asInt(j['annual_spend_xaf']),
      remainingToNextXaf: asInt(j['remaining_to_next_xaf']),
      expiringPoints: asInt(j['expiring_points']),
      expiryNoticeDays: asInt(j['expiry_notice_days']),
      rewardsAvailable: asInt(j['rewards_available']),
      minRedeem: asInt(j['min_redeem_points']),
      tier: j['tier'] == null
          ? null
          : Tier.fromJson((j['tier'] as Map).cast<String, dynamic>()),
      nextTier: j['next_tier'] == null
          ? null
          : Tier.fromJson((j['next_tier'] as Map).cast<String, dynamic>()));
  final int balance,
      annualSpendXaf,
      remainingToNextXaf,
      expiringPoints,
      expiryNoticeDays,
      rewardsAvailable,
      minRedeem;
  final Tier? tier, nextTier;

  /// 0 to 1: how far this year's spend is toward the next tier.
  double get progress {
    final next = nextTier, now = tier;
    if (next == null || now == null) return 1;
    final span = next.minSpendXaf - now.minSpendXaf;
    return span <= 0
        ? 1
        : ((annualSpendXaf - now.minSpendXaf) / span).clamp(0, 1).toDouble();
  }
}

class LedgerEntry {
  const LedgerEntry(
      {required this.type,
      required this.points,
      required this.balanceAfter,
      required this.at,
      this.description,
      this.reference});
  factory LedgerEntry.fromJson(Map<String, dynamic> j) => LedgerEntry(
      type: asString(j['entry_type']),
      points: asInt(j['points']),
      balanceAfter: asInt(j['balance_after']),
      at: asTime(j['created_at']),
      description: asStringOrNull(j['description']),
      reference: asStringOrNull(j['reference']));
  final String type;
  final int points, balanceAfter;
  final DateTime at;
  final String? description, reference;
}

class Reward {
  const Reward(
      {required this.id,
      required this.name,
      required this.type,
      required this.cost,
      required this.canRedeem,
      this.description,
      this.blockedBy,
      this.minPurchases = 0,
      this.discountPct});
  factory Reward.fromJson(Map<String, dynamic> j) => Reward(
      id: asString(j['id']),
      name: asString(j['name']),
      type: asString(j['reward_type']),
      cost: asInt(j['point_cost']),
      canRedeem: asBool(j['can_redeem']),
      description: asStringOrNull(j['description']),
      blockedBy: asStringOrNull(j['blocked_by']),
      minPurchases: asInt(j['min_purchases']),
      discountPct: asIntOrNull(j['discount_pct']));
  final String id, name, type;
  final int cost, minPurchases;
  final bool canRedeem;
  final String? description, blockedBy;
  final int? discountPct;
}

/// A reward in the wallet: usable at a sales point while [state] is AVAILABLE (show [qr]).
class WalletItem {
  const WalletItem(
      {required this.id,
      required this.reference,
      required this.name,
      required this.points,
      required this.state,
      this.validUntil,
      this.usedAt,
      this.qr,
      this.description});
  factory WalletItem.fromJson(Map<String, dynamic> j) => WalletItem(
      id: asString(j['id']),
      reference: asString(j['reference']),
      name: asString(j['reward_name']),
      points: asInt(j['points']),
      state: asString(j['state']),
      validUntil: asTimeOrNull(j['valid_until']),
      usedAt: asTimeOrNull(j['used_at']),
      qr: asStringOrNull(j['qr']),
      description: asStringOrNull(j['description']));
  final String id, reference, name, state;
  final int points;
  final DateTime? validUntil, usedAt;
  final String? qr, description;
  bool get isAvailable => state == 'AVAILABLE';
}

class EarningRule {
  const EarningRule(this.name);
  final String name;
}

class LoyaltyRules {
  const LoyaltyRules(
      {required this.basePoints,
      required this.baseAmountXaf,
      required this.expiryMonths,
      required this.minRedeem,
      required this.rules});
  factory LoyaltyRules.fromJson(Map<String, dynamic> j) => LoyaltyRules(
      basePoints: asInt(j['base_points']),
      baseAmountXaf: asInt(j['base_amount_xaf']),
      expiryMonths: asInt(j['expiry_months']),
      minRedeem: asInt(j['min_redeem_points']),
      rules: asList(j['rules'])
          .map((r) => EarningRule(asString(r['name'])))
          .toList());
  final int basePoints, baseAmountXaf, expiryMonths, minRedeem;
  final List<EarningRule> rules;
}
