import 'package:lutiano_core/lutiano_core.dart';

/// What a scanned reward QR is, before the seller gives it.
class RewardLookup {
  const RewardLookup(
      {required this.rewardName,
      required this.customerName,
      required this.state,
      required this.type,
      this.discountPct,
      this.requiresStock = false});
  factory RewardLookup.fromJson(Map<String, dynamic> j) => RewardLookup(
      rewardName: asString(j['reward_name']),
      customerName: asString(j['customer_name']),
      state: asString(j['state']),
      type: asString(j['reward_type']),
      discountPct: asIntOrNull(j['discount_pct']),
      requiresStock: asBool(j['requires_stock']));
  final String rewardName, customerName, state, type;
  final int? discountPct;
  final bool requiresStock;
  bool get usable => state == 'AVAILABLE';
}

/// Giving customers the rewards they redeemed.
class RewardsRepository {
  RewardsRepository(this._api);
  final ApiClient _api;

  Future<RewardLookup> lookup(String code) async => RewardLookup.fromJson(
      (await _api.get('/seller/rewards/lookup', query: {'code': code}) as Map)
          .cast<String, dynamic>());

  Future<void> use(String code, String operationId) =>
      _api.post('/seller/rewards/use',
          body: {'code': code, 'operationId': operationId});
}
