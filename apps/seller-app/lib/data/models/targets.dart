import 'package:lutiano_core/lutiano_core.dart';

/// Where a bonus stands: reached and waiting to join the earnings, already in a finalised month, taken back, or not (yet) reached.
enum BonusState {
  pending,
  included,
  reversed,
  inProgress,
  notReached,
  none;

  static BonusState fromApi(String v) => switch (v) {
        'PENDING' => pending,
        'INCLUDED' => included,
        'REVERSED' => reversed,
        'IN_PROGRESS' => inProgress,
        'NOT_REACHED' => notReached,
        _ => none,
      };
}

enum TargetState {
  current,
  upcoming,
  ended;

  static TargetState fromApi(String v) => switch (v) {
        'UPCOMING' => upcoming,
        'ENDED' => ended,
        _ => current,
      };
}

/// A sales target with its live progress and bonus.
class SalesTarget {
  const SalesTarget({
    required this.id,
    required this.name,
    required this.isValue,
    required this.periodType,
    required this.startsOn,
    required this.endsOn,
    required this.targetValue,
    required this.bonusIsFixed,
    required this.bonusXaf,
    required this.achieved,
    required this.progressPct,
    required this.reached,
    required this.bonusEarned,
    required this.state,
    required this.bonusState,
    required this.bonusMonth,
    this.productName,
    this.bonusPaidXaf,
  });

  factory SalesTarget.fromJson(Map<String, dynamic> j) => SalesTarget(
        id: asString(j['id']),
        name: asString(j['name']),
        isValue: j['metric'] == 'VALUE',
        periodType: asString(j['period_type']),
        startsOn: DateTime.parse(asString(j['starts_on'])),
        endsOn: DateTime.parse(asString(j['ends_on'])),
        targetValue: asInt(j['target_value']),
        bonusIsFixed: j['bonus_type'] == 'FIXED',
        bonusXaf: asInt(j['bonus_xaf']),
        achieved: asInt(j['achieved']),
        progressPct: (j['progress_pct'] as num? ?? 0).toDouble(),
        reached: asBool(j['reached']),
        bonusEarned: asInt(j['bonus_earned']),
        state: TargetState.fromApi(asString(j['state'])),
        bonusState: BonusState.fromApi(asString(j['bonus_state'])),
        bonusMonth: asString(j['bonus_month']),
        productName: asStringOrNull(j['product_name']),
        bonusPaidXaf:
            j['bonus_paid_xaf'] == null ? null : asInt(j['bonus_paid_xaf']),
      );

  final String id, name, periodType, bonusMonth;
  final bool isValue, bonusIsFixed, reached;
  final DateTime startsOn, endsOn;

  /// Money (XAF) when [isValue], otherwise units.
  final int targetValue, achieved, bonusXaf, bonusEarned;
  final double progressPct;
  final TargetState state;
  final BonusState bonusState;
  final String? productName;
  final int? bonusPaidXaf;

  /// What is left to reach the target (never negative).
  int get remaining => achieved >= targetValue ? 0 : targetValue - achieved;

  /// How much of the period has passed, 0 to 1.
  double elapsed(DateTime today) {
    final total = endsOn.difference(startsOn).inDays + 1;
    if (today.isBefore(startsOn)) return 0;
    if (today.isAfter(endsOn)) return 1;
    return (today.difference(startsOn).inDays + 1) / total;
  }
}

/// A month's targets at a glance, with the main target (for the ring).
class TargetsSummary {
  const TargetsSummary({
    required this.targets,
    required this.current,
    required this.reached,
    required this.bonusPending,
    required this.bonusPendingXaf,
    required this.bonusIncluded,
    required this.bonusIncludedXaf,
    this.main,
  });

  factory TargetsSummary.fromJson(Map<String, dynamic> j) => TargetsSummary(
        targets: asInt(j['targets']),
        current: asInt(j['current']),
        reached: asInt(j['reached']),
        bonusPending: asInt(j['bonus_pending']),
        bonusPendingXaf: asInt(j['bonus_pending_xaf']),
        bonusIncluded: asInt(j['bonus_included']),
        bonusIncludedXaf: asInt(j['bonus_included_xaf']),
        main: j['main'] is Map
            ? SalesTarget.fromJson((j['main'] as Map).cast<String, dynamic>())
            : null,
      );

  final int targets,
      current,
      reached,
      bonusPending,
      bonusPendingXaf,
      bonusIncluded,
      bonusIncludedXaf;
  final SalesTarget? main;
}

/// One month of sales and target results.
class MonthPerformance {
  const MonthPerformance(
      {required this.month,
      required this.salesXaf,
      required this.targets,
      required this.reached,
      this.avgProgressPct,
      this.monthlyTargetXaf,
      this.monthlyAchievedXaf});

  factory MonthPerformance.fromJson(Map<String, dynamic> j) => MonthPerformance(
        month: asString(j['month']),
        salesXaf: asInt(j['sales_xaf']),
        targets: asInt(j['targets']),
        reached: asInt(j['reached']),
        avgProgressPct: (j['avg_progress_pct'] as num?)?.toDouble(),
        monthlyTargetXaf: j['monthly_target_xaf'] == null
            ? null
            : asInt(j['monthly_target_xaf']),
        monthlyAchievedXaf: j['monthly_achieved_xaf'] == null
            ? null
            : asInt(j['monthly_achieved_xaf']),
      );

  final String month;
  final int salesXaf, targets, reached;

  /// Average progress of the targets that ended in the month; null when none did.
  final double? avgProgressPct;
  final int? monthlyTargetXaf, monthlyAchievedXaf;
}

class Performance {
  const Performance(
      {required this.totalSalesXaf,
      required this.targets,
      required this.reached,
      required this.bonusXaf,
      required this.months});

  factory Performance.fromJson(Map<String, dynamic> j) => Performance(
        totalSalesXaf: asInt(j['total_sales_xaf']),
        targets: asInt(j['targets']),
        reached: asInt(j['reached']),
        bonusXaf: asInt(j['bonus_xaf']),
        months: asList(j['months']).map(MonthPerformance.fromJson).toList(),
      );

  final int totalSalesXaf, targets, reached, bonusXaf;
  final List<MonthPerformance> months;
}
