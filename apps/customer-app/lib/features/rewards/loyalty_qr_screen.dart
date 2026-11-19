import 'package:lutiano_core/lutiano_core.dart';
import 'package:material_ui/material_ui.dart';
import 'package:qr_flutter/qr_flutter.dart';

import '../../core/l10n.dart';
import '../../state/app_scope.dart';

/// The customer's loyalty QR: the seller scans it at checkout so the purchase earns points.
class LoyaltyQrScreen extends StatelessWidget {
  const LoyaltyQrScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final me = AppScope.of(context).customer!;
    final l = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l.myLoyaltyQr)),
      body: ListView(padding: const EdgeInsets.all(24), children: [
        Center(
          child: Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(16),
                border: Border.all(color: AppColors.gold, width: 3)),
            child: QrImageView(
                data: me.loyaltyQr, size: 240, semanticsLabel: l.myLoyaltyQr),
          ),
        ),
        const SizedBox(height: 16),
        Text(me.fullName,
            textAlign: TextAlign.center,
            style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w800)),
        Text(me.code,
            textAlign: TextAlign.center,
            style: const TextStyle(color: AppColors.muted)),
        const SizedBox(height: 16),
        InfoNote(l.loyaltyQrHint),
      ]),
    );
  }
}
