import 'package:lutiano_core/lutiano_core.dart';
import '../models.dart';

class SalesRepository {
  SalesRepository(this._api);
  final ApiClient _api;

  /// [idempotencyKey] must stay the same when retrying the same checkout so the sale posts once.
  Future<Sale> create(
      {required String operationId,
      required Map<String, int> lines,
      required int? cashReceivedXaf,
      required String idempotencyKey,
      String paymentMethod = 'CASH',
      String? customerToken}) async {
    final j = await _api.post('/sales', headers: {
      'Idempotency-Key': idempotencyKey
    }, body: {
      'operationId': operationId,
      'paymentMethod': paymentMethod,
      'cashReceivedXaf': cashReceivedXaf,
      if (customerToken != null) 'customerToken': customerToken,
      'lines': [
        for (final e in lines.entries) {'productId': e.key, 'quantity': e.value}
      ],
    });
    return Sale.fromJson((j as Map).cast<String, dynamic>());
  }

  /// Today's and this month's orders, sales, customers, points issued and commission for the signed-in seller.
  Future<SellerSummary> summary() async => SellerSummary.fromJson(
      ((await _api.get('/me/summary')) as Map).cast<String, dynamic>());

  /// One page of the seller's own sales, newest first; [from] and [to] are Douala dates (YYYY-MM-DD), inclusive.
  /// The summary covers every sale in the period: `sales`, `revenue_xaf` and `units`.
  Future<Paged<Sale>> salesPage(
          {String? from,
          String? to,
          String? operationId,
          int page = 1,
          int size = 20}) async =>
      Paged.fromJson(
          await _api.page('/sales',
              query: {
                if (from != null) 'from': from,
                if (to != null) 'to': to,
                if (operationId != null) 'operationId': operationId,
              },
              page: page,
              size: size),
          Sale.fromJson);

  Future<Sale> get(String id) async => Sale.fromJson(
      (await _api.get('/sales/$id') as Map).cast<String, dynamic>());

  Future<List<int>> receiptPdf(String id) =>
      _api.bytes('/sales/$id/receipt.pdf');

  /// Payment methods in display order, including ones that are not available yet.
  Future<List<PaymentMethodOption>> paymentMethods() async =>
      asList(await _api.get('/payment-methods'))
          .map(PaymentMethodOption.fromJson)
          .toList();
}
