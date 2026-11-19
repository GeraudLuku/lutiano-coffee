import 'package:lutiano_core/lutiano_core.dart';

import 'catalogue.dart';

enum OrderStatus {
  placed,
  accepted,
  ready,
  collected,
  rejected,
  cancelled,
  expired;

  static OrderStatus fromApi(String v) => switch (v) {
        'ACCEPTED' => accepted,
        'READY' => ready,
        'COLLECTED' => collected,
        'REJECTED' => rejected,
        'CANCELLED' => cancelled,
        'EXPIRED' => expired,
        _ => placed,
      };

  /// Still in progress.
  bool get isOpen => this == placed || this == accepted || this == ready;
}

class OrderLine {
  const OrderLine(
      {required this.name,
      required this.quantity,
      required this.unitXaf,
      required this.addons});
  factory OrderLine.fromJson(Map<String, dynamic> j) => OrderLine(
      name: asString(j['product_name']),
      quantity: asInt(j['quantity']),
      unitXaf: asInt(j['unit_price_xaf']) + asInt(j['addons_unit_xaf']),
      addons:
          asList(j['addons']).map((a) => asString(a['addon_name'])).toList());
  final String name;
  final int quantity, unitXaf;
  final List<String> addons;
  int get totalXaf => unitXaf * quantity;
}

class OrderStep {
  const OrderStep(this.status, this.at);
  final OrderStatus status;
  final DateTime at;
}

/// An order the customer placed at a sales point.
class Order {
  const Order({
    required this.id,
    required this.reference,
    required this.status,
    required this.totalXaf,
    required this.operationId,
    required this.operationName,
    required this.placedAt,
    this.note,
    this.reason,
    this.saleId,
    this.lines = const [],
    this.timeline = const [],
  });

  factory Order.fromJson(Map<String, dynamic> j) => Order(
        id: asString(j['id']),
        reference: asString(j['reference']),
        status: OrderStatus.fromApi(asString(j['status'])),
        totalXaf: asInt(j['total_xaf']),
        operationId: asString(j['operation_id']),
        operationName: asString(j['operation_name']),
        placedAt: asTime(j['placed_at']),
        note: asStringOrNull(j['note']),
        reason: asStringOrNull(j['reason']),
        saleId: asStringOrNull(j['sale_id']),
        lines: asList(j['lines']).map(OrderLine.fromJson).toList(),
        timeline: asList(j['timeline'])
            .map((t) => OrderStep(
                OrderStatus.fromApi(asString(t['status'])), asTime(t['at'])))
            .toList(),
      );

  final String id, reference, operationId, operationName;
  final OrderStatus status;
  final int totalXaf;
  final DateTime placedAt;
  final String? note, reason, saleId;
  final List<OrderLine> lines;
  final List<OrderStep> timeline;
}

/// A line of an earlier order at today's price, ready to go back into the cart when [available].
class ReorderLine {
  const ReorderLine(
      {required this.productId,
      required this.name,
      required this.quantity,
      required this.unitXaf,
      required this.available,
      required this.addons});
  factory ReorderLine.fromJson(Map<String, dynamic> j) => ReorderLine(
      productId: asString(j['product_id']),
      name: asString(j['name']),
      quantity: asInt(j['quantity']),
      unitXaf: asInt(j['unit_xaf']),
      available: asBool(j['available']),
      addons: asList(j['addons']).map(Addon.fromJson).toList());
  final String productId, name;
  final int quantity, unitXaf;
  final bool available;
  final List<Addon> addons;
}
