import 'package:lutiano_core/lutiano_core.dart';

class SaleLine {
  const SaleLine(
      {required this.productId,
      required this.productName,
      required this.unitPriceXaf,
      required this.quantity,
      required this.lineTotalXaf,
      this.imageUrl});

  factory SaleLine.fromJson(Map<String, dynamic> j) => SaleLine(
        productId: asString(j['product_id']),
        productName: asString(j['product_name']),
        unitPriceXaf: asInt(j['unit_price_xaf']),
        quantity: asInt(j['quantity']),
        lineTotalXaf: asInt(j['line_total_xaf']),
        imageUrl: asStringOrNull(j['image_url']),
      );

  final String productId, productName;
  final int unitPriceXaf, quantity, lineTotalXaf;
  final String? imageUrl;
}

class Sale {
  const Sale(
      {required this.id,
      required this.reference,
      required this.totalXaf,
      required this.createdAt,
      required this.operationName,
      required this.itemCount,
      this.operationCode,
      this.sellerName,
      this.cashReceivedXaf,
      this.changeXaf,
      this.lines = const [],
      this.paymentMethod = 'CASH',
      this.customerName,
      this.customerCode,
      this.pointsEarned});

  factory Sale.fromJson(Map<String, dynamic> j) {
    final lines = asList(j['lines']).map(SaleLine.fromJson).toList();
    return Sale(
      id: asString(j['id']),
      reference: asString(j['reference']),
      totalXaf: asInt(j['total_xaf']),
      createdAt: asTime(j['created_at']),
      operationName: asString(j['operation_name']),
      operationCode: asStringOrNull(j['operation_code']),
      sellerName: asStringOrNull(j['seller_name']),
      itemCount: j['item_count'] != null
          ? asInt(j['item_count'])
          : lines.fold(0, (n, l) => n + l.quantity),
      cashReceivedXaf: asIntOrNull(j['cash_received_xaf']),
      changeXaf: asIntOrNull(j['change_xaf']),
      lines: lines,
      paymentMethod: asStringOrNull(j['payment_method']) ?? 'CASH',
      customerName: asStringOrNull(j['customer_name']),
      customerCode: asStringOrNull(j['customer_code']),
      pointsEarned: j['loyalty'] is Map
          ? asIntOrNull((j['loyalty'] as Map)['total_points'])
          : null,
    );
  }

  final String id, reference, operationName, paymentMethod;
  final String? operationCode, sellerName, customerName, customerCode;
  final int totalXaf, itemCount;
  final int? cashReceivedXaf, changeXaf, pointsEarned;
  final DateTime createdAt;
  final List<SaleLine> lines;
}

/// A payment method from the server. Only [usable] methods can be chosen; Mobile Money is listed but not available yet.
class PaymentMethodOption {
  const PaymentMethodOption(
      {required this.code,
      required this.label,
      required this.available,
      required this.enabled,
      this.provider});

  factory PaymentMethodOption.fromJson(Map<String, dynamic> j) =>
      PaymentMethodOption(
        code: asString(j['code']),
        label: asString(j['label']),
        available: asBool(j['available']),
        enabled: asBool(j['enabled']),
        provider: asStringOrNull(j['provider']),
      );

  /// Shown when the list cannot be loaded: cash works offline from the server's point of view, Mobile Money is not available yet.
  static const fallback = [
    PaymentMethodOption(
        code: 'CASH', label: 'Cash', available: true, enabled: true),
    PaymentMethodOption(
        code: 'MTN_MOMO',
        label: 'MTN Mobile Money',
        available: false,
        enabled: false,
        provider: 'MTN Cameroon'),
    PaymentMethodOption(
        code: 'ORANGE_MONEY',
        label: 'Orange Money',
        available: false,
        enabled: false,
        provider: 'Orange Cameroon'),
  ];

  final String code, label;
  final String? provider;
  final bool available, enabled;
  bool get usable => available && enabled;
  bool get isCash => code == 'CASH';
}

/// One period of the signed-in seller's own figures (`GET /me/summary`).
class PeriodSummary {
  const PeriodSummary(
      {required this.orders,
      required this.salesXaf,
      required this.customers,
      required this.pointsIssued,
      required this.commissionXaf});

  factory PeriodSummary.fromJson(Map<String, dynamic> j) => PeriodSummary(
      orders: asInt(j['orders']),
      salesXaf: asInt(j['sales_xaf']),
      customers: asInt(j['customers']),
      pointsIssued: asInt(j['points_issued']),
      commissionXaf: asInt(j['commission_xaf']));

  final int orders, salesXaf, customers, pointsIssued, commissionXaf;
}

/// One Douala day's sales, for the Home trend.
class DayTotal {
  const DayTotal(this.day, this.salesXaf);
  final String day;
  final int salesXaf;
}

class SellerSummary {
  const SellerSummary(
      {required this.today, required this.month, this.week = const []});

  factory SellerSummary.fromJson(Map<String, dynamic> j) => SellerSummary(
          today: PeriodSummary.fromJson(
              (j['today'] as Map).cast<String, dynamic>()),
          month: PeriodSummary.fromJson(
              (j['month'] as Map).cast<String, dynamic>()),
          week: [
            for (final d in asList(j['week']))
              DayTotal(asString(d['day']), asInt(d['sales_xaf']))
          ]);

  final PeriodSummary today, month;

  /// The last seven days, oldest first.
  final List<DayTotal> week;
}
