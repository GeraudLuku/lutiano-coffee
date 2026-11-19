import 'package:lutiano_core/lutiano_core.dart';

import '../models.dart';

/// The customer's points, tiers, rewards and wallet.
class LoyaltyRepository {
  LoyaltyRepository(this._api);
  final ApiClient _api;

  Map<String, dynamic> _map(Object? d) => (d as Map).cast<String, dynamic>();

  Future<LoyaltyOverview> overview() async => LoyaltyOverview.fromJson(
      _map(await _api.get('/customer/loyalty/overview')));

  Future<List<Tier>> tiers() async =>
      asList(await _api.get('/customer/loyalty/tiers'))
          .map(Tier.fromJson)
          .toList();

  /// [type] is EARNED, USED or EXPIRED.
  Future<Paged<LedgerEntry>> history(
          {String? type, int page = 1, int size = 20}) async =>
      Paged.fromJson(
          await _api.page('/customer/loyalty/history',
              query: {if (type != null) 'type': type}, page: page, size: size),
          LedgerEntry.fromJson);

  Future<LoyaltyRules> rules() async =>
      LoyaltyRules.fromJson(_map(await _api.get('/customer/loyalty/rules')));

  Future<int> estimate(int amountXaf) async =>
      asInt(_map(await _api.get('/customer/loyalty/estimate',
          query: {'amountXaf': '$amountXaf'}))['points']);

  Future<Paged<Reward>> rewards({int page = 1, int size = 20}) async =>
      Paged.fromJson(
          await _api.page('/customer/loyalty/rewards', page: page, size: size),
          Reward.fromJson);

  /// Spends the points and puts the reward in the wallet.
  Future<WalletItem> redeem(String rewardId) async => WalletItem.fromJson(
      _map(await _api.post('/customer/loyalty/rewards/$rewardId/redeem')));

  /// [state] is AVAILABLE, USED or EXPIRED.
  Future<Paged<WalletItem>> wallet(
          {String? state, int page = 1, int size = 20}) async =>
      Paged.fromJson(
          await _api.page('/customer/loyalty/redemptions',
              query: {if (state != null) 'state': state},
              page: page,
              size: size),
          WalletItem.fromJson);

  Future<WalletItem> walletItem(String id) async => WalletItem.fromJson(
      _map(await _api.get('/customer/loyalty/redemptions/$id')));
}
