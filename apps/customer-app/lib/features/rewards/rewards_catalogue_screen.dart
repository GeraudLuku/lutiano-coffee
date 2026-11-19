import 'package:lutiano_core/lutiano_core.dart';
import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import 'reward_qr_screen.dart';

String _blockedMessage(AppLocalizations l, Reward r) => switch (r.blockedBy) {
      'MIN_POINTS' => l.blockedMinPoints,
      'NOT_ENOUGH_POINTS' => l.blockedNotEnough,
      'MIN_PURCHASES' => l.blockedPurchases(r.minPurchases),
      'MIN_SPEND' => l.blockedSpend,
      _ => '',
    };

/// Rewards the customer can spend points on, cheapest first.
class RewardsCatalogueScreen extends StatelessWidget {
  const RewardsCatalogueScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final l = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l.rewardsCatalogue)),
      body: PagedListView<Reward>(
        key: ValueKey('catalogue-${app.loyaltyVersion}'),
        fetch: (page, size) => app.loyalty.rewards(page: page, size: size),
        empty: EmptyState(
            icon: Icons.card_giftcard,
            title: l.noRewards,
            message: l.noRewardsMessage),
        itemBuilder: (context, r, _, __) => Padding(
          padding: const EdgeInsets.only(bottom: 10),
          child: AppCard(
            onTap: () => _confirm(context, r),
            child: Row(children: [
              Container(
                  width: 48,
                  height: 48,
                  decoration: BoxDecoration(
                      color: AppColors.goldSoft,
                      borderRadius: BorderRadius.circular(12)),
                  child: Icon(
                      r.type == 'DISCOUNT_VOUCHER'
                          ? Icons.sell_outlined
                          : Icons.local_cafe_outlined,
                      color: AppColors.amber)),
              const SizedBox(width: 12),
              Expanded(
                  child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                    Text(r.name,
                        style: const TextStyle(fontWeight: FontWeight.w800)),
                    Row(children: [
                      const Icon(Icons.star, size: 16, color: AppColors.gold),
                      const SizedBox(width: 4),
                      Text(l.pointsValue(r.cost),
                          style: const TextStyle(fontWeight: FontWeight.w700))
                    ]),
                    if (!r.canRedeem)
                      Text(_blockedMessage(l, r),
                          style: const TextStyle(
                              color: AppColors.muted, fontSize: 12.5)),
                  ])),
              const Icon(Icons.chevron_right, color: AppColors.muted),
            ]),
          ),
        ),
      ),
    );
  }

  /// Shows what redeeming costs and, when allowed, redeems and shows the QR.
  Future<void> _confirm(BuildContext context, Reward r) async {
    final l = context.l10n;
    final app = AppScope.read(context);
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(r.name),
        content: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              if (r.description != null) Text(r.description!),
              const SizedBox(height: 8),
              Text(l.redeemCost(r.cost),
                  style: const TextStyle(fontWeight: FontWeight.w800)),
              if (!r.canRedeem) ...[
                const SizedBox(height: 8),
                Text(_blockedMessage(l, r),
                    style: const TextStyle(color: AppColors.red))
              ],
              const SizedBox(height: 8),
              Text(l.redeemHint,
                  style:
                      const TextStyle(color: AppColors.muted, fontSize: 12.5)),
            ]),
        actions: [
          TextButton(
              onPressed: () => Navigator.of(ctx).pop(false),
              child: Text(l.cancel)),
          FilledButton(
              onPressed: r.canRedeem ? () => Navigator.of(ctx).pop(true) : null,
              child: Text(l.confirmRedemption)),
        ],
      ),
    );
    if (ok != true || !context.mounted) return;
    try {
      final item = await app.loyalty.redeem(r.id);
      app.loyaltyChanged();
      if (!context.mounted) return;
      await Navigator.of(context).pushReplacement(MaterialPageRoute(
          builder: (_) => RewardQrScreen(itemId: item.id, justRedeemed: true)));
    } catch (e) {
      if (context.mounted) showMessage(context, errorMessage(e), error: true);
    }
  }
}
