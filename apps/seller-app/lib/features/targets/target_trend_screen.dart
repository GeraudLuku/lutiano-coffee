import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import 'target_widgets.dart';

/// Batch 6 · Screen 9 — Target progress trend: monthly sales and average achievement over 3 or 6 months, against the months before.
class TargetTrendScreen extends StatefulWidget {
  const TargetTrendScreen({super.key});

  @override
  State<TargetTrendScreen> createState() => _TargetTrendScreenState();
}

class _TargetTrendScreenState extends State<TargetTrendScreen> {
  int _count = 6;

  double? _avg(List<MonthPerformance> months) {
    final withTargets = months.where((m) => m.avgProgressPct != null).toList();
    if (withTargets.isEmpty) return null;
    return withTargets.fold(0.0, (s, m) => s + m.avgProgressPct!) /
        withTargets.length;
  }

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final l = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l.targetProgressTrend)),
      body: Column(children: [
        FilterChips<int>(
          options: [(3, l.last3Months), (6, l.last6Months)],
          selected: _count,
          onSelected: (c) => setState(() => _count = c ?? 6),
        ),
        Expanded(
          child: AsyncView<Performance>(
            key: ValueKey('trend-$_count-${app.dataVersion}'),
            // twice as many months, to compare the range with the one before it
            load: () => app.targets.performance(count: _count * 2),
            builder: (context, p, _) {
              final months = p.months.sublist(p.months.length - _count);
              final before = p.months.sublist(0, p.months.length - _count);
              final avg = _avg(months), previous = _avg(before);
              final change = avg != null && previous != null && previous > 0
                  ? (avg - previous) * 100 / previous
                  : null;
              return ListView(padding: const EdgeInsets.all(16), children: [
                AppCard(
                  child: Column(children: [
                    MiniBarChart(points: [
                      for (final m in months)
                        BarPoint(monthShort(m.month), m.salesXaf)
                    ], height: 120),
                    const SizedBox(height: 12),
                    MiniLineChart(
                      height: 110,
                      labels: [for (final m in months) monthShort(m.month)],
                      series: [
                        LineSeries(l.achievementLegend, AppColors.gold, [
                          for (final m in months)
                            (m.avgProgressPct ?? 0).round()
                        ]),
                      ],
                    ),
                  ]),
                ),
                const SizedBox(height: 12),
                Row(children: [
                  Expanded(
                    child: AppCard(
                        child: MetricTile(
                            icon: Icons.emoji_events_outlined,
                            label: l.avgAchievement,
                            value:
                                avg == null ? '—' : percentText(context, avg),
                            iconColor: AppColors.amber,
                            iconBg: AppColors.amberSoft)),
                  ),
                  if (change != null) ...[
                    const SizedBox(width: 10),
                    Expanded(
                      child: AppCard(
                          child: MetricTile(
                              icon: change >= 0
                                  ? Icons.arrow_upward
                                  : Icons.arrow_downward,
                              label: l.vsPreviousRange,
                              value: signedPercent(change),
                              iconColor:
                                  change >= 0 ? AppColors.green : AppColors.red,
                              iconBg: change >= 0
                                  ? AppColors.greenSoft
                                  : AppColors.redSoft)),
                    ),
                  ],
                ]),
              ]);
            },
          ),
        ),
      ]),
    );
  }
}
