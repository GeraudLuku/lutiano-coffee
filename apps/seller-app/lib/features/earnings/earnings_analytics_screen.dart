import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import 'earnings_widgets.dart';

/// Batch 5 · Screen 8 — Earnings trends: earnings and commissions per month over the last 3, 6 or 12 months, and what stands out.
class EarningsAnalyticsScreen extends StatefulWidget {
  const EarningsAnalyticsScreen({super.key});

  @override
  State<EarningsAnalyticsScreen> createState() =>
      _EarningsAnalyticsScreenState();
}

class _EarningsAnalyticsScreenState extends State<EarningsAnalyticsScreen> {
  int _count = 6;

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final l = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l.earningsAnalytics)),
      body: Column(children: [
        FilterChips<int>(
          options: [
            (3, l.last3Months),
            (6, l.last6Months),
            (12, l.last12Months)
          ],
          selected: _count,
          onSelected: (c) => setState(() => _count = c ?? 6),
        ),
        Expanded(
          child: AsyncView<List<EarningsMonth>>(
            key: ValueKey('analytics-$_count-${app.dataVersion}'),
            // twice as many months, so the range can be compared with the one before it
            load: () => app.earnings.months(count: _count * 2),
            builder: (context, all, _) {
              final months = all.sublist(all.length - _count);
              final before = all.sublist(0, all.length - _count);
              final total = months.fold(0, (s, m) => s + m.total);
              final commission = months.fold(0, (s, m) => s + m.commission);
              final previous = before.fold(0, (s, m) => s + m.total);
              if (total <= 0 && previous <= 0) {
                return ListView(children: [
                  EmptyState(icon: Icons.show_chart, title: l.noAnalyticsYet)
                ]);
              }
              final change =
                  previous > 0 ? (total - previous) * 100 / previous : null;
              final last = months.last,
                  prior = months.length > 1 ? months[months.length - 2] : null;
              final best = months.reduce((a, b) => b.total > a.total ? b : a);
              return ListView(padding: const EdgeInsets.all(16), children: [
                AppCard(
                  child: Row(children: [
                    Expanded(
                        child: MetricTile(
                            icon: Icons.savings_outlined,
                            label: l.totalEarnings,
                            value: xaf(total),
                            iconColor: AppColors.green,
                            iconBg: AppColors.greenSoft)),
                    if (change != null)
                      Text(signedPercent(change),
                          style: TextStyle(
                              fontWeight: FontWeight.w800,
                              color: change >= 0
                                  ? AppColors.green
                                  : AppColors.red)),
                  ]),
                ),
                const SizedBox(height: 12),
                AppCard(
                  child: MiniLineChart(
                    labels: [for (final m in months) monthShort(m.month)],
                    series: [
                      LineSeries(l.legendEarnings, AppColors.gold,
                          [for (final m in months) m.total]),
                      LineSeries(l.legendCommissions, AppColors.blue,
                          [for (final m in months) m.commission]),
                    ],
                  ),
                ),
                SectionLabel(l.keyInsights),
                AppCard(
                  child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        if (prior != null && prior.total > 0)
                          _Insight(l.insightChange(signedPercent(
                              (last.total - prior.total) * 100 / prior.total))),
                        if (total > 0)
                          _Insight(l.insightCommissionShare(
                              sharePercent(commission, total))),
                        if (best.total > 0)
                          _Insight(l.insightBestMonth(
                              monthLabel(best.month), xaf(best.total))),
                      ]),
                ),
              ]);
            },
          ),
        ),
      ]),
    );
  }
}

class _Insight extends StatelessWidget {
  const _Insight(this.text);
  final String text;

  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 4),
        child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
          const Icon(Icons.lightbulb_outline, size: 18, color: AppColors.amber),
          const SizedBox(width: 8),
          Expanded(child: Text(text)),
        ]),
      );
}
