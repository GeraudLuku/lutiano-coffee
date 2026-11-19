import 'package:lutiano_core/lutiano_core.dart';

import '../models.dart';

/// Customer orders at the seller's sales points: see them, accept, prepare and hand them over.
class OrdersRepository {
  OrdersRepository(this._api);
  final ApiClient _api;

  /// [group] is PENDING (still to handle), COMPLETED or CANCELLED. Orders still to handle come first.
  Future<Paged<CustomerOrder>> page(
          {String? group, int page = 1, int size = 20}) async =>
      Paged.fromJson(
          await _api.page('/seller/orders',
              query: {if (group != null) 'group': group},
              page: page,
              size: size),
          CustomerOrder.fromJson);

  /// Placed, accepted and ready orders (from the page summary of a one-row page).
  Future<OrderCounts> counts() async {
    final p =
        await _api.page('/seller/orders', query: {'group': 'PENDING'}, size: 1);
    final s = (p['summary'] as Map?)?.cast<String, dynamic>() ?? const {};
    return OrderCounts(
        asInt(s['placed']), asInt(s['accepted']), asInt(s['ready']));
  }

  Future<CustomerOrder> get(String id) async => CustomerOrder.fromJson(
      (await _api.get('/seller/orders/$id') as Map).cast<String, dynamic>());

  Future<CustomerOrder> _act(String id, String action, [Object? body]) async =>
      CustomerOrder.fromJson(
          (await _api.post('/seller/orders/$id/$action', body: body) as Map)
              .cast<String, dynamic>());

  Future<CustomerOrder> accept(String id) => _act(id, 'accept');
  Future<CustomerOrder> ready(String id) => _act(id, 'ready');
  Future<CustomerOrder> reject(String id, String reason) =>
      _act(id, 'reject', {'reason': reason});
  Future<CustomerOrder> collect(String id, {int? cashReceivedXaf}) => _act(
      id,
      'collect',
      {if (cashReceivedXaf != null) 'cashReceivedXaf': cashReceivedXaf});
}
