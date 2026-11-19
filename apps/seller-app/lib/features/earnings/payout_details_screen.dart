import 'package:material_ui/material_ui.dart';

import 'package:lutiano_core/lutiano_core.dart';
import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import 'earnings_widgets.dart';
import 'payouts_screen.dart';

/// Batch 5 · Screen 6 — Payout details: when, how and where it was paid, and the earnings of the month it settles.
class PayoutDetailsScreen extends StatelessWidget {
  const PayoutDetailsScreen({super.key, required this.payoutId});
  final String payoutId;

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final l = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l.payoutDetails)),
      body: AsyncView<PayoutItem>(
        key: ValueKey('payout-$payoutId-${app.dataVersion}'),
        load: () => app.earnings.payout(payoutId),
        builder: (context, p, _) {
          final month =
              '${p.periodStart.year}-${p.periodStart.month.toString().padLeft(2, '0')}';
          final account = [
            if (p.destinationName != null) p.destinationName!,
            if (p.destinationLast4 != null) '•••• ${p.destinationLast4}',
          ].join(' ');
          return ListView(padding: const EdgeInsets.all(16), children: [
            AppCard(
              child: Row(children: [
                const Icon(Icons.account_balance_outlined,
                    color: AppColors.navy),
                const SizedBox(width: 12),
                Expanded(
                    child: Text(p.reference,
                        style: const TextStyle(
                            fontWeight: FontWeight.w800, fontSize: 16))),
                payoutStatusChip(context, p),
              ]),
            ),
            SectionLabel(l.currentInformation),
            AppCard(
              child: Column(children: [
                InfoRow(l.requestedOn, dateTime(p.requestedAt)),
                if (p.paidAt != null)
                  InfoRow(l.payoutDate, dateTime(p.paidAt!)),
                InfoRow(l.paymentMethod, paymentMethodLabel(l, p.method)),
                if (p.providerReference != null)
                  InfoRow(l.referenceLabel, p.providerReference!),
                if (account.isNotEmpty) InfoRow(l.accountLabel, account),
                InfoRow(l.monthStatement, l.payoutForMonth(monthLabel(month))),
              ]),
            ),
            SectionLabel(l.amountBreakdown),
            AppCard(
              child: Column(children: [
                if (p.statement != null) ...[
                  InfoRow(l.commissionsLabel, xaf(p.statement!.commission)),
                  InfoRow(l.bonusesLabel, xaf(p.statement!.bonus)),
                  InfoRow(
                      l.adjustmentsLabel, signedXaf(p.statement!.adjustments)),
                  InfoRow(l.monthStatement, xaf(p.statement!.total)),
                  const Divider(),
                ],
                InfoRow(l.payoutAmount, xaf(p.amountXaf), bold: true),
              ]),
            ),
          ]);
        },
      ),
    );
  }
}
