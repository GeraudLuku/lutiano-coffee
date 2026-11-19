import 'package:lutiano_core/lutiano_core.dart';

/// Where a stock request stands. The server's statuses map onto what a seller sees:
/// approved = stock set aside and being prepared, in fulfillment = on its way (confirm each delivery), completed = all confirmed.
enum RequestStatus {
  pending('PENDING_APPROVAL'),
  approved('APPROVED'),
  inFulfillment('IN_TRANSIT'),
  completed('RECEIVED'),
  rejected('REJECTED'),
  cancelled('CANCELLED');

  const RequestStatus(this.api);
  final String api;

  static RequestStatus fromApi(String value) =>
      values.firstWhere((s) => s.api == value, orElse: () => pending);
}

/// One product of a request.
class RequestItem {
  const RequestItem(
      {required this.productId,
      required this.name,
      required this.quantity,
      this.receivedQuantity,
      this.imageUrl});

  factory RequestItem.fromJson(Map<String, dynamic> j) => RequestItem(
        productId: asString(j['product_id']),
        name: asString(j['product_name']),
        quantity: asInt(j['sent_quantity']),
        receivedQuantity: j['received_quantity'] == null
            ? null
            : asInt(j['received_quantity']),
        imageUrl: asStringOrNull(j['image_url']),
      );

  final String productId, name;
  final int quantity;
  final int? receivedQuantity;
  final String? imageUrl;
}

/// Several products asked for together for one sales operation, decided as a whole (reference SR-yyyy-NNNNN).
class StockRequest {
  const StockRequest({
    required this.id,
    required this.reference,
    required this.status,
    required this.operationName,
    required this.createdAt,
    required this.itemCount,
    required this.totalQuantity,
    required this.items,
    this.neededBy,
    this.reason,
    this.decisionNote,
    this.approvedAt,
    this.dispatchedAt,
    this.completedAt,
    this.closedAt,
  });

  factory StockRequest.fromJson(Map<String, dynamic> j) => StockRequest(
        id: asString(j['id']),
        reference: asString(j['reference']),
        status: RequestStatus.fromApi(asString(j['status'])),
        operationName: asString(j['operation_name']),
        createdAt: asTime(j['created_at']),
        itemCount: asInt(j['item_count']),
        totalQuantity: asInt(j['total_quantity']),
        items: asList(j['items']).map(RequestItem.fromJson).toList(),
        neededBy: asTimeOrNull(j['needed_by']),
        reason: asStringOrNull(j['reason']),
        decisionNote: asStringOrNull(j['decision_note']),
        approvedAt: asTimeOrNull(j['approved_at']),
        dispatchedAt: asTimeOrNull(j['dispatched_at']),
        completedAt: asTimeOrNull(j['completed_at']),
        closedAt: asTimeOrNull(j['closed_at']),
      );

  final String id, reference, operationName;
  final RequestStatus status;
  final DateTime createdAt;
  final int itemCount, totalQuantity;
  final List<RequestItem> items;

  /// The day the seller asked for delivery by; null means as soon as possible.
  final DateTime? neededBy;
  final String? reason, decisionNote;
  final DateTime? approvedAt, dispatchedAt, completedAt, closedAt;
}

/// The seller's requests counted by status (over all of them, whatever the list is filtered to).
class RequestCounts {
  const RequestCounts(
      {this.total = 0,
      this.pending = 0,
      this.approved = 0,
      this.inFulfillment = 0,
      this.completed = 0,
      this.rejected = 0});

  factory RequestCounts.fromJson(Map<String, dynamic> j) => RequestCounts(
        total: asInt(j['total']),
        pending: asInt(j['pending']),
        approved: asInt(j['approved']),
        inFulfillment: asInt(j['in_fulfillment']),
        completed: asInt(j['completed']),
        rejected: asInt(j['rejected']),
      );

  final int total, pending, approved, inFulfillment, completed, rejected;
}
