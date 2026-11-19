import 'package:lutiano_core/lutiano_core.dart';
import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import 'reward_qr_screen.dart';

/// My rewards: those ready to use, used and expired.
class WalletScreen extends StatefulWidget {
  const WalletScreen({super.key});

  @override
  State<WalletScreen> createState() => _WalletScreenState();
}

class _WalletScreenState extends State<WalletScreen> {
  String? _state = 'AVAILABLE';

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final l = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l.myRewards)),
      body: Column(children: [
        FilterChips<String>(options: [
          ('AVAILABLE', l.stateAvailable),
          ('USED', l.stateUsed),
          ('EXPIRED', l.stateExpired)
        ], selected: _state, onSelected: (s) => setState(() => _state = s)),
        Expanded(
          child: PagedListView<WalletItem>(
            key: ValueKey('wallet-$_state-${app.loyaltyVersion}'),
            fetch: (page, size) =>
                app.loyalty.wallet(state: _state, page: page, size: size),
            empty: EmptyState(
                icon: Icons.wallet_giftcard,
                title: l.noRewardsHere,
                message: l.noRewardsHereMessage),
            itemBuilder: (context, w, _, __) => Padding(
              padding: const EdgeInsets.only(bottom: 10),
              child: AppCard(
                onTap: () => Navigator.of(context).push(MaterialPageRoute(
                    builder: (_) => RewardQrScreen(itemId: w.id))),
                child: Row(children: [
                  const Icon(Icons.card_giftcard, color: AppColors.amber),
                  const SizedBox(width: 12),
                  Expanded(
                      child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                        Text(w.name,
                            style:
                                const TextStyle(fontWeight: FontWeight.w800)),
                        Text(
                            w.isAvailable && w.validUntil != null
                                ? l.validUntilDate(dayMonthYear(w.validUntil!))
                                : (w.usedAt != null
                                    ? l.rewardUsedOn(dayMonthYear(w.usedAt!))
                                    : l.pointsValue(w.points)),
                            style: const TextStyle(
                                color: AppColors.muted, fontSize: 12.5)),
                      ])),
                  w.state == 'AVAILABLE'
                      ? StatusChip(l.stateAvailable)
                      : w.state == 'USED'
                          ? StatusChip(l.stateUsed,
                              color: AppColors.muted,
                              background: AppColors.background)
                          : StatusChip(l.stateExpired,
                              color: AppColors.red,
                              background: AppColors.redSoft),
                ]),
              ),
            ),
          ),
        ),
      ]),
    );
  }
}
