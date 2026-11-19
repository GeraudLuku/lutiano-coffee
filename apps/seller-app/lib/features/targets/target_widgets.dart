import 'package:material_ui/material_ui.dart';

import 'package:lutiano_core/lutiano_core.dart';
import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../widgets/widgets.dart';

/// A target amount in money, or in units for a quantity target.
String targetAmount(BuildContext context, SalesTarget t, int value) =>
    t.isValue ? xaf(value) : context.l10n.unitsValue(value);

String percentText(BuildContext context, double pct) =>
    context.l10n.percentValue(
        pct.toStringAsFixed(pct >= 100 || pct == pct.roundToDouble() ? 0 : 1));

/// "1 Mar 2026 – 31 Mar 2026".
String targetPeriod(SalesTarget t) =>
    '${plainDayMonthYear(t.startsOn)} – ${plainDayMonthYear(t.endsOn)}';

StatusChip targetStateChip(BuildContext context, TargetState s) {
  final l = context.l10n;
  return switch (s) {
    TargetState.current => StatusChip(l.targetStateActive),
    TargetState.upcoming => StatusChip(l.targetStateUpcoming,
        color: AppColors.blue, background: AppColors.blueSoft),
    TargetState.ended => StatusChip(l.targetStateCompleted,
        color: AppColors.muted, background: AppColors.background),
  };
}

StatusChip bonusStateChip(BuildContext context, BonusState s) {
  final l = context.l10n;
  return switch (s) {
    BonusState.pending => StatusChip(l.bonusStatePending,
        color: AppColors.amber, background: AppColors.amberSoft),
    BonusState.included => StatusChip(l.bonusStateIncluded),
    BonusState.reversed => StatusChip(l.bonusStateReversed,
        color: AppColors.red, background: AppColors.redSoft),
    BonusState.inProgress => StatusChip(l.bonusStateInProgress,
        color: AppColors.blue, background: AppColors.blueSoft),
    BonusState.notReached || BonusState.none => StatusChip(
        l.bonusStateNotReached,
        color: AppColors.muted,
        background: AppColors.background),
  };
}

/// What the bonus is, in words: a fixed amount, or an amount for each unit.
String bonusRate(BuildContext context, SalesTarget t) => t.bonusIsFixed
    ? context.l10n.fixedRate(xaf(t.bonusXaf))
    : context.l10n.perUnitRate(xaf(t.bonusXaf));

/// A progress bar that fills to 100% however far past the target the seller is.
class TargetBar extends StatelessWidget {
  const TargetBar({super.key, required this.percent, this.height = 10});
  final double percent;
  final double height;

  @override
  Widget build(BuildContext context) => Semantics(
        label: percentText(context, percent),
        child: ClipRRect(
          borderRadius: BorderRadius.circular(6),
          child: LinearProgressIndicator(
              value: (percent / 100).clamp(0, 1).toDouble(),
              minHeight: height,
              color: percent >= 100 ? AppColors.green : AppColors.gold,
              backgroundColor: AppColors.line),
        ),
      );
}

/// A target in a list: its name, goal, progress and state.
class TargetTile extends StatelessWidget {
  const TargetTile({super.key, required this.target, required this.onTap});
  final SalesTarget target;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final t = target;
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: AppCard(
        color:
            t.state == TargetState.current ? AppColors.goldSoft : Colors.white,
        onTap: onTap,
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Row(children: [
            const Icon(Icons.emoji_events_outlined, color: AppColors.amber),
            const SizedBox(width: 10),
            Expanded(
                child: Text(t.name,
                    style: const TextStyle(fontWeight: FontWeight.w800))),
            targetStateChip(context, t.state),
          ]),
          const SizedBox(height: 6),
          Row(children: [
            Expanded(
                child: Text(targetAmount(context, t, t.targetValue),
                    style: const TextStyle(
                        fontWeight: FontWeight.w800, fontSize: 16))),
            Text(percentText(context, t.progressPct),
                style: const TextStyle(fontWeight: FontWeight.w800)),
            const Icon(Icons.chevron_right, color: AppColors.muted),
          ]),
          const SizedBox(height: 8),
          TargetBar(percent: t.progressPct),
        ]),
      ),
    );
  }
}
