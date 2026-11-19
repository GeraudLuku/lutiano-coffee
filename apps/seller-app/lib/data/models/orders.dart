import 'package:lutiano_core/lutiano_core.dart';

/// Where a customer order stands.
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

  /// Still needs the seller (not yet handed over or closed).
  bool get isOpen => this == placed || this == accepted || this == ready;
}

class OrderLineAddon {
  const OrderLineAddon(this.name);
  final String name;
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
        addons: asList(j['addons'])
            .map((a) => OrderLineAddon(asString(a['addon_name'])))
            .toList(),
      );
  final String name;
  final int quantity, unitXaf;
  final List<OrderLineAddon> addons;
  int get totalXaf => unitXaf * quantity;
}

class OrderStep {
  const OrderStep(this.status, this.at);
  final OrderStatus status;
  final DateTime at;
}

/// An order a customer placed at one of the seller's sales points.
class CustomerOrder {
  const CustomerOrder({
    required this.id,
    required this.reference,
    required this.status,
    required this.totalXaf,
    required this.customerName,
    required this.operationName,
    required this.placedAt,
    this.note,
    this.reason,
    this.lines = const [],
    this.timeline = const [],
  });

  factory CustomerOrder.fromJson(Map<String, dynamic> j) => CustomerOrder(
        id: asString(j['id']),
        reference: asString(j['reference']),
        status: OrderStatus.fromApi(asString(j['status'])),
        totalXaf: asInt(j['total_xaf']),
        customerName: asString(j['customer_name']),
        operationName: asString(j['operation_name']),
        placedAt: asTime(j['placed_at']),
        note: asStringOrNull(j['note']),
        reason: asStringOrNull(j['reason']),
        lines: asList(j['lines']).map(OrderLine.fromJson).toList(),
        timeline: asList(j['timeline'])
            .map((t) => OrderStep(
                OrderStatus.fromApi(asString(t['status'])), asTime(t['at'])))
            .toList(),
      );

  final String id, reference, customerName, operationName;
  final OrderStatus status;
  final int totalXaf;
  final DateTime placedAt;
  final String? note, reason;
  final List<OrderLine> lines;
  final List<OrderStep> timeline;
}

/// How many orders wait for the seller, for the Home badge.
class OrderCounts {
  const OrderCounts(this.placed, this.accepted, this.ready);
  final int placed, accepted, ready;
  int get open => placed + accepted + ready;
}
