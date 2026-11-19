import 'package:lutiano_core/lutiano_core.dart';
import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import 'history_screen.dart';
import 'how_it_works_screen.dart';
import 'loyalty_qr_screen.dart';
import 'rewards_catalogue_screen.dart';
import 'tiers_screen.dart';
import 'wallet_screen.dart';

/// The Rewards tab: points, tier and progress, and the way into the catalogue, the wallet, the history and how it all works.
class RewardsScreen extends StatelessWidget {
  const RewardsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final l = context.l10n;
    void push(Widget w) =>
        Navigator.of(context).push(MaterialPageRoute(builder: (_) => w));
    return Scaffold(
      appBar: AppBar(title: Text(l.rewards)),
      body: AsyncView<LoyaltyOverview>(
        key: ValueKey('loyalty-${app.ordersVersion}-${app.loyaltyVersion}'),
        load: app.loyalty.overview,
        builder: (context, o, _) =>
            ListView(padding: const EdgeInsets.all(16), children: [
          AppCard(
            color: AppColors.navy,
            child:
                Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(l.yourPointsBalance,
                  style: const TextStyle(color: AppColors.white70)),
              const SizedBox(height: 4),
              Row(children: [
                const Icon(Icons.star, color: AppColors.gold, size: 30),
                const SizedBox(width: 8),
                Text(l.pointsValue(o.balance),
                    style: const TextStyle(
                        color: Colors.white,
                        fontSize: 34,
                        fontWeight: FontWeight.w800)),
              ]),
              if (o.balance == 0) ...[
                const SizedBox(height: 6),
                Text(l.noPointsYetMessage,
                    style: const TextStyle(color: AppColors.white70))
              ],
              if (o.expiringPoints > 0) ...[
                const SizedBox(height: 6),
                Text(l.pointsExpiringSoon(o.expiringPoints, o.expiryNoticeDays),
                    style: const TextStyle(color: AppColors.gold))
              ],
            ]),
          ),
          const SizedBox(height: 12),
          AppCard(
            onTap: () => push(const TiersScreen()),
            child:
                Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Row(children: [
                const Icon(Icons.workspace_premium, color: AppColors.amber),
                const SizedBox(width: 8),
                Expanded(
                    child: Text(l.membershipTier(o.tier?.name ?? '—'),
                        style: const TextStyle(
                            fontWeight: FontWeight.w800, fontSize: 16))),
                const Icon(Icons.chevron_right, color: AppColors.muted),
              ]),
              if (o.nextTier != null) ...[
                const SizedBox(height: 10),
                ClipRRect(
                    borderRadius: BorderRadius.circular(6),
                    child: LinearProgressIndicator(
                        value: o.progress,
                        minHeight: 10,
                        color: AppColors.gold,
                        backgroundColor: AppColors.line)),
                const SizedBox(height: 6),
                Text(
                    l.spendToReachTier(
                        xaf(o.remainingToNextXaf), o.nextTier!.name),
                    style: const TextStyle(
                        color: AppColors.muted, fontSize: 12.5)),
              ] else
                Padding(
                    padding: const EdgeInsets.only(top: 6),
                    child: Text(l.topTierReached,
                        style: const TextStyle(color: AppColors.muted))),
            ]),
          ),
          const SizedBox(height: 12),
          FilledButton.icon(
              onPressed: () => push(const LoyaltyQrScreen()),
              icon: const Icon(Icons.qr_code_2),
              label: Text(l.showMyLoyaltyQr)),
          const SizedBox(height: 8),
          AppCard(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
            child: Column(children: [
              MenuTile(
                  icon: Icons.card_giftcard,
                  title: l.rewardsCatalogue,
                  onTap: () => push(const RewardsCatalogueScreen())),
              MenuTile(
                  icon: Icons.wallet_giftcard,
                  title: l.myRewards,
                  subtitle: l.rewardsReady(o.rewardsAvailable),
                  onTap: () => push(const WalletScreen())),
              MenuTile(
                  icon: Icons.history,
                  title: l.pointsHistory,
                  onTap: () => push(const HistoryScreen())),
              MenuTile(
                  icon: Icons.emoji_events_outlined,
                  title: l.membershipTiers,
                  onTap: () => push(const TiersScreen())),
              MenuTile(
                  icon: Icons.help_outline,
                  title: l.howPointsWork,
                  onTap: () => push(const HowItWorksScreen())),
            ]),
          ),
        ]),
      ),
    );
  }
}
