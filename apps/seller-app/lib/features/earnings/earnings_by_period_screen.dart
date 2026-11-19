import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import 'earnings_widgets.dart';

enum _Grain { day, week, month }

/// Batch 5 · Screen 3 — Earnings by period: per day, week or month, as a bar chart with the totals of the range.
/// A bonus counts on the day its target ended; commission and adjustments on the day they happened.
class EarningsByPeriodScreen extends StatefulWidget {
  const EarningsByPeriodScreen({super.key});

  @override
  State<EarningsByPeriodScreen> createState() => _EarningsByPeriodScreenState();
}

class _EarningsByPeriodScreenState extends State<EarningsByPeriodScreen> {
  _Grain _grain = _Grain.month;

  int get _count =>
      switch (_grain) { _Grain.day => 14, _Grain.week => 8, _Grain.month => 6 };

  String _label(EarningsPoint p) => switch (_grain) {
        _Grain.day => '${p.start.day}',
        _Grain.week => plainDayMonth(p.start),
        _Grain.month => monthShort(
            '${p.start.year}-${p.start.month.toString().padLeft(2, '0')}'),
      };

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final l = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l.earningsByPeriod)),
      body: Column(children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
          child: SegmentedButton<_Grain>(
            showSelectedIcon: false,
            segments: [
              ButtonSegment(value: _Grain.day, label: Text(l.periodDaily)),
              ButtonSegment(value: _Grain.week, label: Text(l.periodWeekly)),
              ButtonSegment(value: _Grain.month, label: Text(l.periodMonthly)),
            ],
            selected: {_grain},
            onSelectionChanged: (s) => setState(() => _grain = s.first),
          ),
        ),
        Expanded(
          child: AsyncView<List<EarningsPoint>>(
            key: ValueKey('by-period-$_grain-${app.dataVersion}'),
            load: () =>
                app.earnings.series(_grain.name.toUpperCase(), count: _count),
            builder: (context, points, _) {
              final commission = points.fold(0, (s, p) => s + p.commission);
              final bonus = points.fold(0, (s, p) => s + p.bonus);
              final adjustments = points.fold(0, (s, p) => s + p.adjustments);
              final total = commission + bonus + adjustments;
              final change = _grain == _Grain.month &&
                      points.length >= 2 &&
                      points[points.length - 2].total > 0
                  ? (points.last.total - points[points.length - 2].total) *
                      100 /
                      points[points.length - 2].total
                  : null;
              return ListView(padding: const EdgeInsets.all(16), children: [
                Text(
                    switch (_grain) {
                      _Grain.day => l.lastNDays(_count),
                      _Grain.week => l.lastNWeeks(_count),
                      _Grain.month => l.lastNMonths(_count),
                    },
                    style: const TextStyle(fontWeight: FontWeight.w700)),
                const SizedBox(height: 12),
                AppCard(
                  child: MiniBarChart(points: [
                    for (final p in points) BarPoint(_label(p), p.total)
                  ], height: 130),
                ),
                const SizedBox(height: 12),
                EarningsFigures(
                    commission: commission,
                    bonus: bonus,
                    adjustments: adjustments,
                    total: total),
                if (change != null) ...[
                  const SizedBox(height: 10),
                  Align(
                      alignment: Alignment.centerLeft,
                      child: ChangeLine(percent: change)),
                ],
              ]);
            },
          ),
        ),
      ]),
    );
  }
}
