import 'package:material_ui/material_ui.dart';

import 'package:lutiano_core/lutiano_core.dart';
import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import 'target_widgets.dart';

/// Batch 6 · Screen 5 — Bonus details: what the bonus is, whether it was earned, and when it joins the earnings.
class BonusDetailsScreen extends StatelessWidget {
  const BonusDetailsScreen({super.key, required this.targetId});
  final String targetId;

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final l = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l.bonusDetails)),
      body: AsyncView<SalesTarget>(
        key: ValueKey('bonus-$targetId-${app.dataVersion}'),
        load: () => app.targets.get(targetId),
        builder: (context, t, _) {
          final month = monthLabel(t.bonusMonth);
          final (note, color, background, icon) = switch (t.bonusState) {
            BonusState.pending => (
                l.bonusPendingNote,
                AppColors.amber,
                AppColors.amberSoft,
                Icons.hourglass_empty
              ),
            BonusState.included => (
                l.bonusIncludedNote,
                AppColors.green,
                AppColors.greenSoft,
                Icons.check_circle_outline
              ),
            BonusState.reversed => (
                l.bonusReversedNote,
                AppColors.red,
                AppColors.redSoft,
                Icons.cancel_outlined
              ),
            BonusState.inProgress => (
                l.bonusInProgressNote,
                AppColors.blue,
                AppColors.blueSoft,
                Icons.info_outline
              ),
            _ => (
                l.bonusNotReachedNote,
                AppColors.muted,
                AppColors.background,
                Icons.info_outline
              ),
          };
          final amount = t.bonusPaidXaf ?? t.bonusEarned;
          return ListView(padding: const EdgeInsets.all(16), children: [
            AppCard(
              child: Row(children: [
                const Icon(Icons.card_giftcard,
                    color: AppColors.amber, size: 30),
                const SizedBox(width: 12),
                Expanded(
                    child: Text(t.name,
                        style: const TextStyle(
                            fontWeight: FontWeight.w800, fontSize: 16))),
                bonusStateChip(context, t.bonusState),
              ]),
            ),
            SectionLabel(l.bonusInformation),
            AppCard(
              child: Column(children: [
                InfoRow(l.bonusTypeLabel,
                    t.bonusIsFixed ? l.bonusFixed : l.bonusPerUnit),
                InfoRow(l.bonusRateLabel, bonusRate(context, t)),
                InfoRow(
                    l.targetAmount, targetAmount(context, t, t.targetValue)),
                InfoRow(
                    '${l.achievedSales} (${percentText(context, t.progressPct)})',
                    targetAmount(context, t, t.achieved)),
                InfoRow(l.bonusAmountLabel, xaf(amount), bold: true),
                InfoRow(
                    l.expectedPayout,
                    t.bonusState == BonusState.included
                        ? l.bonusPaidIn(month)
                        : l.afterMonthClosed(month)),
              ]),
            ),
            const SizedBox(height: 12),
            InfoNote(note, icon: icon, color: color, background: background),
          ]);
        },
      ),
    );
  }
}
