import 'package:lutiano_core/lutiano_core.dart';
import 'package:material_ui/material_ui.dart';
import 'package:qr_flutter/qr_flutter.dart';

import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';

/// A wallet reward: the QR to show the seller while it is usable, or when it was used or expired.
class RewardQrScreen extends StatelessWidget {
  const RewardQrScreen(
      {super.key, required this.itemId, this.justRedeemed = false});
  final String itemId;
  final bool justRedeemed;

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final l = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(justRedeemed ? l.rewardRedeemed : l.myReward)),
      body: AsyncView<WalletItem>(
        key: ValueKey('reward-$itemId-${app.loyaltyVersion}'),
        load: () => app.loyalty.walletItem(itemId),
        builder: (context, w, _) =>
            ListView(padding: const EdgeInsets.all(24), children: [
          Text(w.name,
              textAlign: TextAlign.center,
              style:
                  const TextStyle(fontSize: 22, fontWeight: FontWeight.w800)),
          const SizedBox(height: 4),
          Text(l.pointsValue(w.points),
              textAlign: TextAlign.center,
              style: const TextStyle(color: AppColors.muted)),
          const SizedBox(height: 16),
          if (w.isAvailable && w.qr != null) ...[
            Center(
              child: Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(16),
                    border: Border.all(color: AppColors.gold, width: 3)),
                child: QrImageView(
                    data: w.qr!, size: 230, semanticsLabel: l.rewardQr),
              ),
            ),
            const SizedBox(height: 12),
            Text(l.showThisToSeller, textAlign: TextAlign.center),
            if (w.validUntil != null)
              Text(l.validUntilDate(dayMonthYear(w.validUntil!)),
                  textAlign: TextAlign.center,
                  style: const TextStyle(color: AppColors.muted)),
          ] else
            InfoNote(
                w.state == 'USED'
                    ? l.rewardUsedOn(
                        w.usedAt == null ? '' : dayMonthYear(w.usedAt!))
                    : l.rewardExpiredMessage,
                color: w.state == 'USED' ? AppColors.green : AppColors.red,
                background: w.state == 'USED'
                    ? AppColors.greenSoft
                    : AppColors.redSoft),
        ]),
      ),
    );
  }
}
