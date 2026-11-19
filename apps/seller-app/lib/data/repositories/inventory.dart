import 'package:lutiano_core/lutiano_core.dart';
import '../models.dart';

class InventoryRepository {
  InventoryRepository(this._api);
  final ApiClient _api;

  /// One page of an operation's stock. The summary covers the whole operation, whatever the filter:
  /// `products`, `units`, `value_xaf`, `attention` (low or out) and `categories`.
  /// [status] is `out`, `low` or `attention`; [ids] restricts the list to those products.
  Future<Paged<StockItem>> stockPage(String operationId,
          {String? q,
          String? category,
          String? status,
          List<String>? ids,
          bool lowestFirst = false,
          int page = 1,
          int size = 20}) async =>
      Paged.fromJson(
          await _api.page('/inventory',
              query: {
                'operationId': operationId,
                if (q != null && q.trim().isNotEmpty) 'q': q.trim(),
                if (category != null) 'category': category,
                if (status != null) 'status': status,
                if (ids != null && ids.isNotEmpty) 'ids': ids.join(','),
                if (lowestFirst) 'sort': 'quantity',
              },
              page: page,
              size: size),
          StockItem.fromJson);

  /// Stock in and out at the operation, newest first; [direction] is `IN` or `OUT`; [from] and [to] are inclusive yyyy-MM-dd days.
  Future<Paged<StockMovement>> movementsPage(String operationId,
          {String? productId,
          String? direction,
          String? from,
          String? to,
          int page = 1,
          int size = 20}) async =>
      Paged.fromJson(
          await _api.page('/inventory/movements',
              query: {
                'operationId': operationId,
                if (productId != null) 'productId': productId,
                if (direction != null) 'direction': direction,
                if (from != null) 'from': from,
                if (to != null) 'to': to,
              },
              page: page,
              size: size),
          StockMovement.fromJson);

  /// Units received and consumed over a period (everything when no dates are given), for the operation or one product.
  Future<StockSummary> summary(String operationId,
          {String? productId, String? from, String? to}) async =>
      StockSummary.fromJson((await _api.get('/inventory/summary', query: {
        'operationId': operationId,
        if (productId != null) 'productId': productId,
        if (from != null) 'from': from,
        if (to != null) 'to': to,
      }) as Map)
          .cast<String, dynamic>());

  /// Deliveries on their way to the operation.
  Future<Paged<IncomingAllocation>> incomingPage(String operationId,
          {int page = 1, int size = 20}) async =>
      Paged.fromJson(
          await _api.page('/inventory/allocations/mine',
              query: {'operationId': operationId}, page: page, size: size),
          IncomingAllocation.fromJson);

  /// Returns true when the received quantity differed and a discrepancy was recorded.
  Future<bool> confirmReceipt(String allocationId, int quantity,
      {String? note}) async {
    final j = (await _api.post('/inventory/allocations/confirm', body: {
      'allocationId': allocationId,
      'quantity': quantity,
      'note': note
    }) as Map);
    return asBool(j['discrepancy']);
  }
}
