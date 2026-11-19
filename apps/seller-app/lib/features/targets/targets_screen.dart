import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import 'bonus_history_screen.dart';
import 'campaigns_screen.dart';
import 'my_targets_screen.dart';
import 'performance_screen.dart';
import 'target_details_screen.dart';
import 'target_rules_screen.dart';
import 'target_trend_screen.dart';
import 'target_widgets.dart';

/// Batch 6 · Screen 1 — Targets overview for a month: the main target's progress ring, counts, and the way into the rest.
class TargetsScreen extends StatefulWidget {
  const TargetsScreen({super.key});

  @override
  State<TargetsScreen> createState() => _TargetsScreenState();
}

class _TargetsScreenState extends State<TargetsScreen> {
  String _month = currentMonth();

  void _open(Widget screen) =>
      Navigator.of(context).push(MaterialPageRoute(builder: (_) => screen));

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final l = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l.targetsAndBonuses)),
      body: Column(children: [
        MonthPicker(
            value: _month, onChanged: (m) => setState(() => _month = m)),
        Expanded(
          child: AsyncView<TargetsSummary>(
            key: ValueKey('targets-$_month-${app.dataVersion}'),
            load: () => app.targets.summary(month: _month),
            builder: (context, s, _) {
              final main = s.main;
              return ListView(padding: const EdgeInsets.all(16), children: [
                if (main == null)
                  EmptyState(
                      icon: Icons.emoji_events_outlined,
                      title: l.noTargetThisMonth,
                      message: l.noTargetThisMonthMessage)
                else
                  AppCard(
                    onTap: () => _open(TargetDetailsScreen(targetId: main.id)),
                    child: Row(children: [
                      _Ring(percent: main.progressPct),
                      const SizedBox(width: 16),
                      Expanded(
                          child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                            Text(main.name,
                                style: const TextStyle(
                                    fontWeight: FontWeight.w800)),
                            const SizedBox(height: 8),
                            _Figure(l.salesTargetLabel,
                                targetAmount(context, main, main.targetValue)),
                            _Figure(l.achievedLabel,
                                targetAmount(context, main, main.achieved)),
                            _Figure(l.remainingLabel,
                                targetAmount(context, main, main.remaining)),
                          ])),
                    ]),
                  ),
                const SizedBox(height: 12),
                AppCard(
                  child: Row(children: [
                    Expanded(
                        child: MetricTile(
                            icon: Icons.emoji_events_outlined,
                            label: l.activeTargets,
                            value: '${s.current}',
                            iconColor: AppColors.amber,
                            iconBg: AppColors.amberSoft)),
                    Expanded(
                        child: MetricTile(
                            icon: Icons.star_outline,
                            label: l.bonusEligible,
                            value: '${s.bonusPending}',
                            iconColor: AppColors.blue,
                            iconBg: AppColors.blueSoft)),
                    Expanded(
                        child: MetricTile(
                            icon: Icons.card_giftcard,
                            label: l.bonusIncluded,
                            value: '${s.bonusIncluded}',
                            iconColor: AppColors.red,
                            iconBg: AppColors.redSoft)),
                  ]),
                ),
                if (main != null) ...[
                  const SizedBox(height: 16),
                  FilledButton(
                    onPressed: () =>
                        _open(TargetDetailsScreen(targetId: main.id)),
                    child: Text(l.viewTargetDetails),
                  ),
                ],
                const SizedBox(height: 16),
                AppCard(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                  child: Column(children: [
                    MenuTile(
                        icon: Icons.flag_outlined,
                        title: l.myTargets,
                        onTap: () => _open(const MyTargetsScreen())),
                    MenuTile(
                        icon: Icons.star_border,
                        title: l.myBonuses,
                        onTap: () => _open(const BonusHistoryScreen())),
                    MenuTile(
                        icon: Icons.bar_chart,
                        title: l.performanceSummary,
                        onTap: () => _open(const PerformanceScreen())),
                    MenuTile(
                        icon: Icons.show_chart,
                        title: l.targetProgressTrend,
                        onTap: () => _open(const TargetTrendScreen())),
                    MenuTile(
                        icon: Icons.campaign_outlined,
                        title: l.salesCampaigns,
                        subtitle: l.salesCampaignsSub,
                        onTap: () => _open(const CampaignsScreen())),
                    MenuTile(
                        icon: Icons.rule,
                        title: l.targetRulesPolicy,
                        onTap: () => _open(const TargetRulesScreen())),
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

class _Figure extends StatelessWidget {
  const _Figure(this.label, this.value);
  final String label, value;

  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.only(bottom: 4),
        child: Row(children: [
          Expanded(
              child: Text(label,
                  style:
                      const TextStyle(color: AppColors.muted, fontSize: 12.5))),
          Text(value, style: const TextStyle(fontWeight: FontWeight.w800)),
        ]),
      );
}

/// A circle that fills to 100% (a seller past the target still sees a full ring, with the real percentage in the middle).
class _Ring extends StatelessWidget {
  const _Ring({required this.percent});
  final double percent;

  @override
  Widget build(BuildContext context) => Semantics(
        label:
            '${context.l10n.targetProgress} ${percentText(context, percent)}',
        child: SizedBox(
          width: 96,
          height: 96,
          child: Stack(alignment: Alignment.center, children: [
            SizedBox(
              width: 96,
              height: 96,
              child: CircularProgressIndicator(
                  value: (percent / 100).clamp(0, 1).toDouble(),
                  strokeWidth: 10,
                  color: percent >= 100 ? AppColors.green : AppColors.gold,
                  backgroundColor: AppColors.line),
            ),
            Text(percentText(context, percent),
                style:
                    const TextStyle(fontWeight: FontWeight.w800, fontSize: 20)),
          ]),
        ),
      );
}
