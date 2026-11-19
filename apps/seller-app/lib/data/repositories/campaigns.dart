import 'package:lutiano_core/lutiano_core.dart';
import '../models.dart';

class CampaignsRepository {
  CampaignsRepository(this._api);
  final ApiClient _api;

  /// Campaigns that reach the seller, newest first; [state] narrows to running, upcoming or ended ones.
  Future<Paged<Campaign>> page(
          {TargetState? state, int page = 1, int size = 20}) async =>
      Paged.fromJson(
          await _api.page('/me/campaigns',
              query: {if (state != null) 'state': state.name.toUpperCase()},
              page: page,
              size: size),
          Campaign.fromJson);

  Future<Campaign> get(String id) async => Campaign.fromJson(
      (await _api.get('/me/campaigns/$id') as Map).cast<String, dynamic>());
}
