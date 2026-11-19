import 'package:material_ui/material_ui.dart';

import 'package:lutiano_core/lutiano_core.dart';
import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import 'target_widgets.dart';

/// Batch 6 · Screen 7 — Performance summary over the last 3, 6 or 12 months: sales, targets reached, bonuses, month by month.
class PerformanceScreen extends StatefulWidget {
  const PerformanceScreen({super.key});

  @override
  State<PerformanceScreen> createState() => _PerformanceScreenState();
}

class _PerformanceScreenState extends State<PerformanceScreen> {
  int _count = 3;

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final l = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l.performanceSummary)),
      body: Column(children: [
        FilterChips<int>(
          options: [
            (3, l.last3Months),
            (6, l.last6Months),
            (12, l.last12Months)
          ],
          selected: _count,
          onSelected: (c) => setState(() => _count = c ?? 3),
        ),
        Expanded(
          child: AsyncView<Performance>(
            key: ValueKey('performance-$_count-${app.dataVersion}'),
            load: () => app.targets.performance(count: _count),
            builder: (context, p, _) =>
                ListView(padding: const EdgeInsets.all(16), children: [
              AppCard(
                child: Column(children: [
                  MetricTile(
                      icon: Icons.bar_chart,
                      label: l.totalSalesLabel,
                      value: xaf(p.totalSalesXaf),
                      iconColor: AppColors.green,
                      iconBg: AppColors.greenSoft),
                  const Divider(),
                  MetricTile(
                      icon: Icons.emoji_events_outlined,
                      label: l.targetsReachedLabel,
                      value: l.targetsReachedValue(p.reached, p.targets),
                      iconColor: AppColors.amber,
                      iconBg: AppColors.amberSoft),
                  const Divider(),
                  MetricTile(
                      icon: Icons.card_giftcard,
                      label: l.bonusesEarnedLabel,
                      value: xaf(p.bonusXaf),
                      iconColor: AppColors.red,
                      iconBg: AppColors.redSoft),
                ]),
              ),
              SectionLabel(l.monthlyPerformance),
              if (p.targets == 0) InfoNote(l.noPerformanceYet),
              for (final m in p.months.reversed)
                Padding(
                  padding: const EdgeInsets.only(bottom: 8),
                  child: AppCard(
                    child: Column(children: [
                      Row(children: [
                        Expanded(
                            child: Text(monthLabel(m.month),
                                style: const TextStyle(
                                    fontWeight: FontWeight.w700))),
                        Text(xaf(m.salesXaf),
                            style:
                                const TextStyle(fontWeight: FontWeight.w800)),
                        if (m.avgProgressPct != null) ...[
                          const SizedBox(width: 12),
                          Text(percentText(context, m.avgProgressPct!),
                              style:
                                  const TextStyle(fontWeight: FontWeight.w800)),
                        ],
                      ]),
                      if (m.avgProgressPct != null) ...[
                        const SizedBox(height: 8),
                        TargetBar(percent: m.avgProgressPct!, height: 8),
                      ],
                    ]),
                  ),
                ),
            ]),
          ),
        ),
      ]),
    );
  }
}
