import 'package:lutiano_core/lutiano_core.dart';
import '../models.dart';

/// Stock requests: ask for several products at once, follow their status.
class RequestsRepository {
  RequestsRepository(this._api);
  final ApiClient _api;

  /// The signed-in seller's requests at one operation, newest first. The page summary carries the counts per status.
  Future<Paged<StockRequest>> minePage(String operationId,
          {RequestStatus? status,
          String? reference,
          int page = 1,
          int size = 20}) async =>
      Paged.fromJson(
          await _api.page('/inventory/requests/mine',
              query: {
                'operationId': operationId,
                if (status != null) 'status': status.api,
                if (reference != null && reference.trim().isNotEmpty)
                  'reference': reference.trim(),
              },
              page: page,
              size: size),
          StockRequest.fromJson);

  Future<StockRequest> get(String id) async =>
      StockRequest.fromJson((await _api.get('/inventory/requests/$id') as Map)
          .cast<String, dynamic>());

  /// Sends a request. The same [idempotencyKey] sent again returns the same request, so a retry after a lost answer never duplicates it.
  Future<StockRequest> create(String operationId,
          {required Map<String, int> quantities,
          required String idempotencyKey,
          DateTime? neededBy,
          String? reason}) async =>
      StockRequest.fromJson((await _api.post('/inventory/requests', body: {
        'operationId': operationId,
        'idempotencyKey': idempotencyKey,
        'items': [
          for (final e in quantities.entries)
            {'productId': e.key, 'quantity': e.value}
        ],
        if (neededBy != null)
          'neededBy':
              '${neededBy.year.toString().padLeft(4, '0')}-${neededBy.month.toString().padLeft(2, '0')}-${neededBy.day.toString().padLeft(2, '0')}',
        if (reason != null && reason.trim().isNotEmpty) 'reason': reason.trim(),
      }) as Map)
          .cast<String, dynamic>());
}
