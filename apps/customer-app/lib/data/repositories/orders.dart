import 'package:lutiano_core/lutiano_core.dart';

import '../../state/cart_controller.dart';
import '../models.dart';

/// Placing orders and following them.
class OrdersRepository {
  OrdersRepository(this._api);
  final ApiClient _api;

  /// Places the cart as an order. The same [key] sent again returns the order already placed, so a retry never orders twice.
  Future<Order> place(
          {required String operationId,
          required List<CartLine> lines,
          required String key,
          String? note}) async =>
      Order.fromJson((await _api.post('/customer/orders', headers: {
        'Idempotency-Key': key
      }, body: {
        'operationId': operationId,
        if (note != null && note.isNotEmpty) 'note': note,
        'lines': [
          for (final l in lines)
            {
              'productId': l.productId,
              'quantity': l.quantity,
              'addonIds': l.addons.map((a) => a.id).toList()
            }
        ],
      }) as Map)
          .cast<String, dynamic>());

  /// [group] is PENDING, COMPLETED or CANCELLED; newest first.
  Future<Paged<Order>> page(
          {String? group, int page = 1, int size = 20}) async =>
      Paged.fromJson(
          await _api.page('/customer/orders',
              query: {if (group != null) 'group': group},
              page: page,
              size: size),
          Order.fromJson);

  Future<Order> get(String id) async => Order.fromJson(
      (await _api.get('/customer/orders/$id') as Map).cast<String, dynamic>());

  Future<Order> cancel(String id) async =>
      Order.fromJson((await _api.post('/customer/orders/$id/cancel') as Map)
          .cast<String, dynamic>());

  Future<List<ReorderLine>> reorder(String id) async =>
      asList(await _api.get('/customer/orders/$id/reorder'))
          .map(ReorderLine.fromJson)
          .toList();

  Future<List<int>> receiptPdf(String id) =>
      _api.bytes('/customer/orders/$id/receipt.pdf');
}
