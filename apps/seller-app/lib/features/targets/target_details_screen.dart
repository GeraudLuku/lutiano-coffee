import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import 'package:lutiano_core/lutiano_core.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import 'bonus_details_screen.dart';
import 'bonus_eligibility_screen.dart';
import 'target_widgets.dart';

/// Batch 6 · Screen 2 — Target details and progress, with the reward and the way to the bonus screens.
class TargetDetailsScreen extends StatelessWidget {
  const TargetDetailsScreen({super.key, required this.targetId});
  final String targetId;

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final l = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l.targetDetails)),
      body: AsyncView<SalesTarget>(
        key: ValueKey('target-$targetId-${app.dataVersion}'),
        load: () => app.targets.get(targetId),
        builder: (context, t, _) =>
            ListView(padding: const EdgeInsets.all(16), children: [
          AppCard(
            color: AppColors.goldSoft,
            child: Row(children: [
              const Icon(Icons.savings_outlined,
                  color: AppColors.amber, size: 30),
              const SizedBox(width: 12),
              Expanded(
                  child: Text(t.name,
                      style: const TextStyle(
                          fontWeight: FontWeight.w800, fontSize: 16))),
              targetStateChip(context, t.state),
            ]),
          ),
          SectionLabel(l.targetInformation),
          AppCard(
            child: Column(children: [
              InfoRow(l.targetAmount, targetAmount(context, t, t.targetValue)),
              InfoRow(l.achievedSales, targetAmount(context, t, t.achieved)),
              InfoRow(l.progressLabel, percentText(context, t.progressPct),
                  bold: true),
              const SizedBox(height: 4),
              TargetBar(percent: t.progressPct),
              const SizedBox(height: 8),
              InfoRow(l.periodLabel, targetPeriod(t)),
              if (t.productName != null)
                InfoRow(l.targetProduct, t.productName!),
            ]),
          ),
          if (t.bonusXaf > 0) ...[
            SectionLabel(l.rewardLabel),
            AppCard(
              child: Column(children: [
                InfoRow(l.bonusOnTarget, bonusRate(context, t), bold: true),
              ]),
            ),
            const SizedBox(height: 10),
            if (!t.reached && t.state != TargetState.ended)
              InfoNote(l.reachTargetHint),
            const SizedBox(height: 12),
            AppCard(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
              child: Column(children: [
                MenuTile(
                    icon: Icons.verified_outlined,
                    title: l.bonusEligibility,
                    onTap: () => Navigator.of(context).push(MaterialPageRoute(
                        builder: (_) =>
                            BonusEligibilityScreen(targetId: t.id)))),
                MenuTile(
                    icon: Icons.card_giftcard,
                    title: l.bonusDetails,
                    onTap: () => Navigator.of(context).push(MaterialPageRoute(
                        builder: (_) => BonusDetailsScreen(targetId: t.id)))),
              ]),
            ),
          ],
        ]),
      ),
    );
  }
}
