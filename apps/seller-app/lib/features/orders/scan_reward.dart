import 'package:lutiano_core/lutiano_core.dart';
import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../state/app_scope.dart';
import '../customer/scan_customer_screen.dart';

/// Scan a customer's reward QR, show what it is, and give it on confirmation.
Future<void> scanReward(BuildContext context) async {
  final app = AppScope.read(context);
  final l = context.l10n;
  final op = app.operation;
  if (op == null) return;
  final code = await Navigator.of(context).push<String>(
      MaterialPageRoute(builder: (_) => const ScanCustomerScreen()));
  if (code == null || !context.mounted) return;
  try {
    final r = await app.rewards.lookup(code);
    if (!context.mounted) return;
    final give = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(r.rewardName),
        content: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(l.rewardFor(r.customerName)),
              if (r.discountPct != null)
                Padding(
                    padding: const EdgeInsets.only(top: 8),
                    child: Text(l.takeOffPercent(r.discountPct!),
                        style: const TextStyle(fontWeight: FontWeight.w800))),
              if (r.requiresStock)
                Padding(
                    padding: const EdgeInsets.only(top: 8),
                    child: Text(l.fromThisStock)),
              if (!r.usable)
                Padding(
                    padding: const EdgeInsets.only(top: 8),
                    child: Text(
                        r.state == 'USED'
                            ? l.rewardAlreadyUsed
                            : l.rewardExpiredCode,
                        style: const TextStyle(
                            color: AppColors.red,
                            fontWeight: FontWeight.w700))),
            ]),
        actions: [
          TextButton(
              onPressed: () => Navigator.of(ctx).pop(false),
              child: Text(l.cancel)),
          FilledButton(
              onPressed: r.usable ? () => Navigator.of(ctx).pop(true) : null,
              child: Text(l.giveReward)),
        ],
      ),
    );
    if (give != true || !context.mounted) return;
    await app.rewards.use(code, op.id);
    app.dataChanged();
    if (context.mounted) showMessage(context, l.rewardGiven);
  } catch (e) {
    if (context.mounted) showMessage(context, errorMessage(e), error: true);
  }
}
