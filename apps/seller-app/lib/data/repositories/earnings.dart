import 'package:lutiano_core/lutiano_core.dart';
import '../models.dart';

/// The seller's own earnings: months, breakdown, trends, sales that earned commission, payouts and commission rules.
class EarningsRepository {
  EarningsRepository(this._api);
  final ApiClient _api;

  Map<String, String> _month(String? month) =>
      {if (month != null) 'month': month};

  /// A month's earnings (this month when [month] is null, yyyy-MM otherwise), the month before it, and what is owed.
  Future<EarningsMonth> overview({String? month}) async =>
      EarningsMonth.fromJson(
          (await _api.get('/me/earnings/overview', query: _month(month)) as Map)
              .cast<String, dynamic>());

  /// The last [count] months, oldest first.
  Future<List<EarningsMonth>> months({int count = 6}) async =>
      asList(await _api.get('/me/earnings/months', query: {'count': '$count'}))
          .map(EarningsMonth.fromJson)
          .toList();

  Future<EarningsBreakdown> breakdown({String? month}) async =>
      EarningsBreakdown.fromJson((await _api.get('/me/earnings/breakdown',
              query: _month(month)) as Map)
          .cast<String, dynamic>());

  /// [granularity]: DAY, WEEK or MONTH; the last [count] of them, oldest first.
  Future<List<EarningsPoint>> series(String granularity,
          {required int count}) async =>
      asList(await _api.get('/me/earnings/series',
              query: {'granularity': granularity, 'count': '$count'}))
          .map(EarningsPoint.fromJson)
          .toList();

  Future<List<OperationEarnings>> operations({String? month}) async =>
      asList(await _api.get('/me/earnings/operations', query: _month(month)))
          .map(OperationEarnings.fromJson)
          .toList();

  /// Sales that earned commission in the month, newest first.
  Future<Paged<SaleEarning>> salesPage(
          {String? month, int page = 1, int size = 20}) async =>
      Paged.fromJson(
          await _api.page('/me/earnings/sales',
              query: _month(month), page: page, size: size),
          SaleEarning.fromJson);

  Future<SaleEarningDetail> sale(String saleId) async =>
      SaleEarningDetail.fromJson((await _api.get('/me/earnings/sales/$saleId')
              as Map)
          .cast<String, dynamic>());

  /// The seller's payouts, newest first; the page summary counts all of them.
  Future<Paged<PayoutItem>> payoutsPage(
          {PayoutGroup? group, int page = 1, int size = 20}) async =>
      Paged.fromJson(
          await _api.page('/me/earnings/payouts',
              query: {if (group != null) 'status': group.name.toUpperCase()},
              page: page,
              size: size),
          PayoutItem.fromJson);

  Future<PayoutItem> payout(String id) async =>
      PayoutItem.fromJson((await _api.get('/me/earnings/payouts/$id') as Map)
          .cast<String, dynamic>());

  Future<Paged<CommissionRule>> rulesPage(
          {int page = 1, int size = 20}) async =>
      Paged.fromJson(
          await _api.page('/me/earnings/rules', page: page, size: size),
          CommissionRule.fromJson);

  /// The PDF statement for months [from] to [to] (yyyy-MM) with the chosen [sections] (SUMMARY, BREAKDOWN, BONUSES, ADJUSTMENTS, SALES).
  Future<List<int>> statementPdf(
          {required String from,
          required String to,
          required Set<String> sections,
          required String language}) =>
      _api.bytes(
          '/me/earnings/statement.pdf?from=$from&to=$to&sections=${sections.join(',')}&lang=$language');
}
