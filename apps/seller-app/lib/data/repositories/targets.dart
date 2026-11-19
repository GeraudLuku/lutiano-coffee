import 'package:lutiano_core/lutiano_core.dart';
import '../models.dart';

/// The seller's own targets, bonuses and performance.
class TargetsRepository {
  TargetsRepository(this._api);
  final ApiClient _api;

  /// Targets newest first; [state] narrows to running, upcoming or ended ones, [month] (yyyy-MM) to those overlapping it.
  Future<Paged<SalesTarget>> page(
          {TargetState? state,
          String? month,
          int page = 1,
          int size = 20}) async =>
      Paged.fromJson(
          await _api.page('/me/targets',
              query: {
                if (state != null) 'state': state.name.toUpperCase(),
                if (month != null) 'month': month,
              },
              page: page,
              size: size),
          SalesTarget.fromJson);

  Future<SalesTarget> get(String id) async => SalesTarget.fromJson(
      (await _api.get('/me/targets/$id') as Map).cast<String, dynamic>());

  Future<TargetsSummary> summary({String? month}) async =>
      TargetsSummary.fromJson((await _api.get('/me/targets/summary',
              query: {if (month != null) 'month': month}) as Map)
          .cast<String, dynamic>());

  /// Targets whose bonus was earned or taken back; [status] PENDING, INCLUDED or REVERSED.
  Future<Paged<SalesTarget>> bonusesPage(
          {BonusState? status, int page = 1, int size = 20}) async =>
      Paged.fromJson(
          await _api.page('/me/targets/bonuses',
              query: {if (status != null) 'status': status.name.toUpperCase()},
              page: page,
              size: size),
          SalesTarget.fromJson);

  /// The last [count] months (1 to 12) of sales and target results.
  Future<Performance> performance({int count = 6}) async =>
      Performance.fromJson(
          (await _api.get('/me/targets/performance', query: {'count': '$count'})
                  as Map)
              .cast<String, dynamic>());
}
