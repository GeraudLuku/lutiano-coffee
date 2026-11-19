import 'package:material_ui/material_ui.dart';

import 'package:lutiano_core/lutiano_core.dart';
import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import 'campaigns_screen.dart';
import 'target_widgets.dart';

/// A campaign in full: the seller's progress, every tier with what it pays, and how the bonus is paid.
class CampaignDetailsScreen extends StatelessWidget {
  const CampaignDetailsScreen({super.key, required this.campaignId});
  final String campaignId;

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final l = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l.salesCampaigns)),
      body: AsyncView<Campaign>(
        key: ValueKey('campaign-$campaignId-${app.dataVersion}'),
        load: () => app.campaigns.get(campaignId),
        builder: (context, c, _) =>
            ListView(padding: const EdgeInsets.all(16), children: [
          AppCard(
            child:
                Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Row(children: [
                Expanded(
                    child: Text(c.name,
                        style: const TextStyle(
                            fontWeight: FontWeight.w800, fontSize: 17))),
                targetStateChip(context, c.state),
              ]),
              const SizedBox(height: 4),
              Text(campaignPeriod(c),
                  style: const TextStyle(color: AppColors.muted)),
              if (c.description != null) ...[
                const SizedBox(height: 8),
                Text(c.description!),
              ],
              const Divider(height: 24),
              InfoRow(l.campaignOperation, c.operationName ?? l.allSellers),
              InfoRow(l.campaignNetSales, xaf(c.salesXaf)),
              InfoRow(l.campaignReward, xaf(c.bonusXaf), bold: true),
              if (c.paidBonusXaf != null)
                InfoRow(l.campaignBonusPaid(xaf(c.paidBonusXaf!)), ''),
              const SizedBox(height: 8),
              TargetBar(percent: c.progressPct),
            ]),
          ),
          const SizedBox(height: 12),
          InfoNote(c.nextThresholdXaf == null
              ? l.allTiersReached
              : l.nextTierHint(
                  xaf(c.nextThresholdXaf!), xaf(c.nextBonusXaf ?? 0))),
          const SizedBox(height: 12),
          AppCard(
            child:
                Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(l.campaignTiersTitle,
                  style: const TextStyle(fontWeight: FontWeight.w800)),
              const SizedBox(height: 6),
              for (final t in c.tiers)
                ListTile(
                  contentPadding: EdgeInsets.zero,
                  leading: Icon(
                      c.salesXaf >= t.thresholdXaf
                          ? Icons.check_circle
                          : Icons.radio_button_unchecked,
                      color: c.salesXaf >= t.thresholdXaf
                          ? AppColors.green
                          : AppColors.muted),
                  title: Text(xaf(t.thresholdXaf)),
                  trailing: Text(l.tierBonus(xaf(t.bonusXaf)),
                      style: const TextStyle(fontWeight: FontWeight.w700)),
                ),
            ]),
          ),
          const SizedBox(height: 12),
          InfoNote(l.campaignBonusNote),
        ]),
      ),
    );
  }
}
