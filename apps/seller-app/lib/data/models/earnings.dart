import 'package:lutiano_core/lutiano_core.dart';

/// What is still to be paid to the seller from finalised months.
class PayoutBalance {
  const PayoutBalance(
      {this.owed = 0, this.paid = 0, this.pending = 0, this.outstanding = 0});

  factory PayoutBalance.fromJson(Map<String, dynamic> j) => PayoutBalance(
        owed: asInt(j['owed_xaf']),
        paid: asInt(j['paid_xaf']),
        pending: asInt(j['pending_xaf']),
        outstanding: asInt(j['outstanding_xaf']),
      );

  /// Payable from finalised months, what was paid, what is in payouts waiting to be paid, and what is left to be paid out.
  final int owed, paid, pending, outstanding;
}

/// One month's earnings: final once an administrator finalised the month, provisional until then.
class EarningsMonth {
  const EarningsMonth({
    required this.month,
    required this.isFinal,
    required this.salesCount,
    required this.salesXaf,
    required this.commission,
    required this.bonus,
    required this.adjustments,
    required this.total,
    this.previousTotal,
    this.balance,
  });

  factory EarningsMonth.fromJson(Map<String, dynamic> j) => EarningsMonth(
        month: asString(j['month']),
        isFinal: j['status'] == 'FINAL',
        salesCount: asInt(j['sales_count']),
        salesXaf: asInt(j['sales_xaf']),
        commission: asInt(j['commission_xaf']),
        bonus: asInt(j['bonus_xaf']),
        adjustments: asInt(j['adjustments_xaf']),
        total: asInt(j['total_xaf']),
        previousTotal: j['previous_total_xaf'] == null
            ? null
            : asInt(j['previous_total_xaf']),
        balance: j['balance'] is Map
            ? PayoutBalance.fromJson(
                (j['balance'] as Map).cast<String, dynamic>())
            : null,
      );

  /// yyyy-MM, as the API names it.
  final String month;
  final bool isFinal;
  final int salesCount, salesXaf, commission, bonus, adjustments, total;
  final int? previousTotal;
  final PayoutBalance? balance;

  /// The first day of the month.
  DateTime get start => DateTime.parse('$month-01');

  /// Change against the month before, in percent; null when there was nothing before to compare with.
  double? get changePercent {
    final before = previousTotal;
    if (before == null || before <= 0) return null;
    return (total - before) * 100 / before;
  }
}

/// Earnings over a day, a week or a month, for charts.
class EarningsPoint {
  const EarningsPoint(
      {required this.start,
      required this.end,
      required this.commission,
      required this.bonus,
      required this.adjustments,
      required this.total});

  factory EarningsPoint.fromJson(Map<String, dynamic> j) => EarningsPoint(
        start: DateTime.parse(asString(j['starts_on'])),
        end: DateTime.parse(asString(j['ends_on'])),
        commission: asInt(j['commission_xaf']),
        bonus: asInt(j['bonus_xaf']),
        adjustments: asInt(j['adjustments_xaf']),
        total: asInt(j['total_xaf']),
      );

  final DateTime start, end;
  final int commission, bonus, adjustments, total;
}

/// Where a period's earnings came from.
class EarningsBreakdown {
  const EarningsBreakdown(
      {required this.commission,
      required this.productCommission,
      required this.addonCommission,
      required this.bonus,
      required this.adjustments,
      required this.top});

  factory EarningsBreakdown.fromJson(Map<String, dynamic> j) =>
      EarningsBreakdown(
        commission: asInt(j['commission_xaf']),
        productCommission: asInt(j['product_commission_xaf']),
        addonCommission: asInt(j['addon_commission_xaf']),
        bonus: asInt(j['bonus_xaf']),
        adjustments: asInt(j['adjustments_xaf']),
        top: [
          for (final t in asList(j['top_products']))
            (
              name: asString(t['product_name']),
              units: asInt(t['units']),
              commission: asInt(t['commission_xaf'])
            )
        ],
      );

  final int commission, productCommission, addonCommission, bonus, adjustments;
  final List<({String name, int units, int commission})> top;
}

/// A sales operation's sales and the commission they earned.
class OperationEarnings {
  const OperationEarnings(
      {required this.id,
      required this.name,
      required this.salesCount,
      required this.salesXaf,
      required this.commission});

  factory OperationEarnings.fromJson(Map<String, dynamic> j) =>
      OperationEarnings(
        id: asString(j['operation_id']),
        name: asString(j['operation_name']),
        salesCount: asInt(j['sales_count']),
        salesXaf: asInt(j['sales_xaf']),
        commission: asInt(j['commission_xaf']),
      );

  final String id, name;
  final int salesCount, salesXaf, commission;

  /// Commission as a share of the sales, in percent.
  double get ratePercent => salesXaf <= 0 ? 0 : commission * 100 / salesXaf;
}

/// A sale that earned commission, with the amount (after anything a refund took back).
class SaleEarning {
  const SaleEarning(
      {required this.saleId,
      required this.reference,
      required this.createdAt,
      required this.totalXaf,
      required this.operationName,
      required this.commission,
      required this.reversed});

  factory SaleEarning.fromJson(Map<String, dynamic> j) => SaleEarning(
        saleId: asString(j['sale_id']),
        reference: asString(j['reference']),
        createdAt: asTime(j['created_at']),
        totalXaf: asInt(j['total_xaf']),
        operationName: asString(j['operation_name']),
        commission: asInt(j['commission_xaf']),
        reversed: asInt(j['reversed_xaf']),
      );

  final String saleId, reference, operationName;
  final DateTime createdAt;
  final int totalXaf, commission, reversed;
  int get net => commission - reversed;
}

/// One line of a sale and the commission rule applied to it.
class EarningLine {
  const EarningLine(
      {required this.productName,
      required this.quantity,
      required this.lineValueXaf,
      required this.basis,
      required this.rate,
      required this.commission,
      required this.reversed});

  factory EarningLine.fromJson(Map<String, dynamic> j) => EarningLine(
        productName: asString(j['product_name']),
        quantity: asInt(j['quantity']),
        lineValueXaf: asInt(j['line_value_xaf']),
        basis: asString(j['basis']),
        rate: asInt(j['rate']),
        commission: asInt(j['commission_xaf']),
        reversed: asInt(j['reversed_xaf']),
      );

  final String productName, basis;
  final int quantity, lineValueXaf, rate, commission, reversed;
}

class SaleEarningDetail {
  const SaleEarningDetail({required this.sale, required this.lines});

  factory SaleEarningDetail.fromJson(Map<String, dynamic> j) =>
      SaleEarningDetail(
        sale: SaleEarning.fromJson(j),
        lines: asList(j['lines']).map(EarningLine.fromJson).toList(),
      );

  final SaleEarning sale;
  final List<EarningLine> lines;
}

/// The seller's payouts grouped the way they read them.
enum PayoutGroup { completed, pending, cancelled }

class PayoutItem {
  const PayoutItem({
    required this.id,
    required this.reference,
    required this.amountXaf,
    required this.method,
    required this.status,
    required this.requestedAt,
    required this.periodStart,
    required this.periodEnd,
    this.paidAt,
    this.destinationName,
    this.destinationLast4,
    this.providerReference,
    this.statement,
  });

  factory PayoutItem.fromJson(Map<String, dynamic> j) => PayoutItem(
        id: asString(j['id']),
        reference: asString(j['reference']),
        amountXaf: asInt(j['amount_xaf']),
        method: asString(j['method']),
        status: asString(j['status']),
        requestedAt: asTime(j['requested_at']),
        paidAt: asTimeOrNull(j['paid_at']),
        periodStart: DateTime.parse(asString(j['period_starts_on'])),
        periodEnd: DateTime.parse(asString(j['period_ends_on'])),
        destinationName: asStringOrNull(j['destination_name']),
        destinationLast4: asStringOrNull(j['destination_last4']),
        providerReference: asStringOrNull(j['provider_reference']),
        statement: j['statement'] is Map
            ? PayoutStatement.fromJson(
                (j['statement'] as Map).cast<String, dynamic>())
            : null,
      );

  final String id, reference, method, status;
  final int amountXaf;
  final DateTime requestedAt, periodStart, periodEnd;
  final DateTime? paidAt;
  final String? destinationName, destinationLast4, providerReference;

  /// The earnings of the month this payout is for (when that month is final).
  final PayoutStatement? statement;

  PayoutGroup get group => switch (status) {
        'PAID' => PayoutGroup.completed,
        'REJECTED' || 'CANCELLED' || 'REVERSED' => PayoutGroup.cancelled,
        _ => PayoutGroup.pending,
      };
}

class PayoutStatement {
  const PayoutStatement(
      {required this.commission,
      required this.bonus,
      required this.adjustments,
      required this.total});

  factory PayoutStatement.fromJson(Map<String, dynamic> j) => PayoutStatement(
        commission: asInt(j['commission_xaf']),
        bonus: asInt(j['bonus_xaf']),
        adjustments: asInt(j['adjustments_xaf']),
        total: asInt(j['total_xaf']),
      );

  final int commission, bonus, adjustments, total;
}

class PayoutCounts {
  const PayoutCounts(
      {this.completed = 0,
      this.pending = 0,
      this.cancelled = 0,
      this.paidOut = 0});

  factory PayoutCounts.fromJson(Map<String, dynamic> j) => PayoutCounts(
        completed: asInt(j['completed']),
        pending: asInt(j['pending']),
        cancelled: asInt(j['cancelled']),
        paidOut: asInt(j['paid_out_xaf']),
      );

  final int completed, pending, cancelled, paidOut;
}

/// A commission rule in force for the seller's role: a percentage of the sale value or a fixed amount per unit.
class CommissionRule {
  const CommissionRule(
      {required this.productName,
      required this.category,
      required this.basis,
      required this.rate});

  factory CommissionRule.fromJson(Map<String, dynamic> j) => CommissionRule(
        productName: asString(j['product_name']),
        category: asString(j['category']),
        basis: asString(j['basis']),
        rate: asInt(j['rate']),
      );

  final String productName, category, basis;

  /// Basis points for a percentage (500 = 5%), XAF per unit for a fixed rule.
  final int rate;
  bool get isPercent => basis == 'PERCENT';
}
