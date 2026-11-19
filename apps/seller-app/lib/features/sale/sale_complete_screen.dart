import 'package:material_ui/material_ui.dart';

import 'package:lutiano_core/lutiano_core.dart';
import '../../data/models.dart';
import '../../widgets/widgets.dart';
import 'product_selection_screen.dart';
import '../../state/app_scope.dart';
import 'receipt_actions.dart';
import '../../core/l10n.dart';

/// Batch 1 · Screen 9 — Sale Confirmation.
class SaleCompleteScreen extends StatelessWidget {
  const SaleCompleteScreen({super.key, required this.sale});
  final Sale sale;

  @override
  Widget build(BuildContext context) => Scaffold(
        backgroundColor: AppColors.navyDeep,
        body: SafeArea(
          child: ListView(padding: const EdgeInsets.all(20), children: [
            const SizedBox(height: 24),
            const Center(
                child: CircleAvatar(
                    radius: 40,
                    backgroundColor: AppColors.green,
                    child: Icon(Icons.check, color: Colors.white, size: 46))),
            const SizedBox(height: 16),
            Text(context.l10n.saleCompleted,
                textAlign: TextAlign.center,
                style: TextStyle(
                    color: Colors.white,
                    fontSize: 26,
                    fontWeight: FontWeight.w800)),
            Text(context.l10n.thePurchaseHasBeenRecorded,
                textAlign: TextAlign.center,
                style: TextStyle(color: AppColors.white70)),
            const SizedBox(height: 24),
            AppCard(
              child: Column(children: [
                InfoRow(context.l10n.saleReference, sale.reference),
                InfoRow(context.l10n.dateTime, dateTime(sale.createdAt)),
                InfoRow(context.l10n.items, '${sale.itemCount}'),
                InfoRow(context.l10n.paymentMethod,
                    paymentMethodLabel(context.l10n, sale.paymentMethod)),
                if (sale.customerName != null)
                  InfoRow(context.l10n.customer, sale.customerName!),
                if (sale.pointsEarned != null && sale.pointsEarned! > 0)
                  InfoRow(context.l10n.loyaltyPointsEarned,
                      context.l10n.pointsEarnedValue(sale.pointsEarned!)),
                if (sale.cashReceivedXaf != null)
                  InfoRow(
                      context.l10n.cashReceived, xaf(sale.cashReceivedXaf!)),
                if (sale.changeXaf != null)
                  InfoRow(context.l10n.changeGiven, xaf(sale.changeXaf!)),
                const Divider(),
                InfoRow(context.l10n.totalAmount, xaf(sale.totalXaf),
                    bold: true),
              ]),
            ),
            const SizedBox(height: 20),
            // the receipt is offered here unless the seller switched it off in Settings (it stays in the sale's details)
            if (AppScope.of(context).settings.showReceipt) ...[
              OutlinedButton.icon(
                style: OutlinedButton.styleFrom(backgroundColor: Colors.white),
                onPressed: () => openReceipt(context, sale.id, sale.reference),
                icon: const Icon(Icons.receipt_long_outlined),
                label: Text(context.l10n.viewReceipt),
              ),
              const SizedBox(height: 10),
            ],
            FilledButton.icon(
              onPressed: () => Navigator.of(context).pushAndRemoveUntil(
                MaterialPageRoute(
                    builder: (_) => const ProductSelectionScreen(),
                    settings: const RouteSettings(
                        name: ProductSelectionScreen.routeName)),
                (route) => route.isFirst,
              ),
              icon: const Icon(Icons.add),
              label: Text(context.l10n.newSale),
            ),
            TextButton(
              onPressed: () => Navigator.of(context).popUntil((r) => r.isFirst),
              child: Text(context.l10n.backToHome,
                  style: TextStyle(color: Colors.white)),
            ),
          ]),
        ),
      );
}
