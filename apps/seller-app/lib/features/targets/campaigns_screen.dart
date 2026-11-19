import 'package:material_ui/material_ui.dart';

import 'package:lutiano_core/lutiano_core.dart';
import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import 'campaign_details_screen.dart';
import 'target_widgets.dart';

/// Batch 6 · Screen 10 — Sales Campaigns: the campaigns that reach the seller (running, upcoming, past) and their progress.
class CampaignsScreen extends StatefulWidget {
  const CampaignsScreen({super.key});

  @override
  State<CampaignsScreen> createState() => _CampaignsScreenState();
}

class _CampaignsScreenState extends State<CampaignsScreen> {
  TargetState? _state = TargetState.current;

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final l = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l.salesCampaigns)),
      body: Column(children: [
        FilterChips<TargetState>(
          options: [
            (TargetState.current, l.targetStateActive),
            (TargetState.upcoming, l.targetStateUpcoming),
            (TargetState.ended, l.targetStateCompleted),
          ],
          selected: _state,
          onSelected: (s) => setState(() => _state = s),
        ),
        Expanded(
          child: PagedListView<Campaign>(
            key: ValueKey('campaigns-$_state-${app.dataVersion}'),
            fetch: (page, size) =>
                app.campaigns.page(state: _state, page: page, size: size),
            empty: EmptyState(
                icon: Icons.campaign_outlined,
                title: l.noCampaigns,
                message: l.noCampaignsMessage),
            itemBuilder: (context, c, _, __) => _CampaignCard(
                campaign: c,
                onTap: () => Navigator.of(context).push(MaterialPageRoute(
                    builder: (_) => CampaignDetailsScreen(campaignId: c.id)))),
          ),
        ),
      ]),
    );
  }
}

class _CampaignCard extends StatelessWidget {
  const _CampaignCard({required this.campaign, required this.onTap});
  final Campaign campaign;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final c = campaign, l = context.l10n;
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: AppCard(
        onTap: onTap,
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Row(children: [
            const Icon(Icons.campaign_outlined, color: AppColors.amber),
            const SizedBox(width: 10),
            Expanded(
                child: Text(c.name,
                    style: const TextStyle(fontWeight: FontWeight.w800))),
            targetStateChip(context, c.state),
          ]),
          const SizedBox(height: 4),
          Text(campaignPeriod(c),
              style: const TextStyle(color: AppColors.muted, fontSize: 12.5)),
          const Divider(height: 20),
          InfoRow(l.campaignReward, xaf(c.paidBonusXaf ?? c.bonusXaf),
              bold: true),
          Text(l.yourProgress,
              style: const TextStyle(fontWeight: FontWeight.w800)),
          const SizedBox(height: 6),
          Row(children: [
            Expanded(
                child: Text(
                    c.nextThresholdXaf == null
                        ? xaf(c.salesXaf)
                        : '${xaf(c.salesXaf)} / ${xaf(c.nextThresholdXaf!)}',
                    style: const TextStyle(fontWeight: FontWeight.w700))),
            Text(percentText(context, c.progressPct),
                style: const TextStyle(fontWeight: FontWeight.w800)),
          ]),
          const SizedBox(height: 6),
          TargetBar(percent: c.progressPct),
        ]),
      ),
    );
  }
}

/// "1 Mar 2026 – 31 Mar 2026".
String campaignPeriod(Campaign c) =>
    '${plainDayMonthYear(c.startsOn)} – ${plainDayMonthYear(c.endsOn)}';
