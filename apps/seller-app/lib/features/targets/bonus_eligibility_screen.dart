import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import 'package:lutiano_core/lutiano_core.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import 'target_widgets.dart';

/// Batch 6 · Screen 4 — Bonus eligibility. The one condition the system applies is reaching the target, so that is all it lists.
class BonusEligibilityScreen extends StatelessWidget {
  const BonusEligibilityScreen({super.key, required this.targetId});
  final String targetId;

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final l = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l.bonusEligibility)),
      body: AsyncView<SalesTarget>(
        key: ValueKey('eligibility-$targetId-${app.dataVersion}'),
        load: () => app.targets.get(targetId),
        builder: (context, t, _) {
          final today = DateTime.now();
          final onTrack = t.progressPct >= t.elapsed(today) * 100;
          // where the seller stands right now, in words
          final (status, color, background) = t.reached
              ? (l.statusReached, AppColors.green, AppColors.greenSoft)
              : t.state == TargetState.ended
                  ? (l.statusNotReached, AppColors.red, AppColors.redSoft)
                  : t.state == TargetState.upcoming
                      ? (l.statusNotStarted, AppColors.blue, AppColors.blueSoft)
                      : onTrack
                          ? (
                              l.statusOnTrack,
                              AppColors.amber,
                              AppColors.goldSoft
                            )
                          : (
                              l.statusBehind,
                              AppColors.amber,
                              AppColors.amberSoft
                            );
          return ListView(padding: const EdgeInsets.all(16), children: [
            InfoNote(t.reached ? l.eligibleTitle : l.notEligibleYet,
                icon: t.reached ? Icons.card_giftcard : Icons.hourglass_empty,
                color: t.reached ? AppColors.green : AppColors.amber,
                background:
                    t.reached ? AppColors.greenSoft : AppColors.amberSoft),
            SectionLabel(l.eligibilityStatus),
            AppCard(
              child: ListTile(
                contentPadding: EdgeInsets.zero,
                leading: Icon(
                    t.reached
                        ? Icons.check_circle
                        : Icons.radio_button_unchecked,
                    color: t.reached ? AppColors.green : AppColors.muted),
                title: Text(l.conditionReachTarget),
                trailing: Text(percentText(context, t.progressPct),
                    style: TextStyle(
                        fontWeight: FontWeight.w800,
                        color: t.reached ? AppColors.green : AppColors.muted)),
              ),
            ),
            const SizedBox(height: 12),
            AppCard(
              color: background,
              child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(l.currentStatus,
                        style: const TextStyle(
                            color: AppColors.muted, fontSize: 12.5)),
                    Text(status,
                        style: TextStyle(
                            fontWeight: FontWeight.w800,
                            fontSize: 20,
                            color: color)),
                    if (!t.reached && t.state != TargetState.ended) ...[
                      const SizedBox(height: 6),
                      Text(
                          l.awayFromBonus(
                              percentText(context, 100 - t.progressPct)),
                          style: const TextStyle(fontSize: 13)),
                      Text(
                          '${l.remainingLabel}: ${targetAmount(context, t, t.remaining)}',
                          style: const TextStyle(fontWeight: FontWeight.w700)),
                    ],
                  ]),
            ),
            const SizedBox(height: 10),
            Text(targetPeriod(t),
                style: const TextStyle(color: AppColors.muted)),
          ]);
        },
      ),
    );
  }
}
