import 'package:material_ui/material_ui.dart';

import 'package:lutiano_core/lutiano_core.dart';
import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../widgets/widgets.dart';
import '../history/sale_detail_screen.dart';

/// Batch 2 · Screen 8 — Consumption detail: the stock a sale used, with a link to the sale.
class ConsumptionDetailScreen extends StatelessWidget {
  const ConsumptionDetailScreen({super.key, required this.movement});
  final StockMovement movement;

  @override
  Widget build(BuildContext context) {
    final m = movement;
    return Scaffold(
      appBar: AppBar(title: Text(context.l10n.consumptionDetail)),
      body: ListView(padding: const EdgeInsets.all(16), children: [
        InfoNote(context.l10n.linkedToSale,
            icon: Icons.receipt_long_outlined,
            color: AppColors.navy,
            background: AppColors.goldSoft),
        const SizedBox(height: 12),
        AppCard(
          child: Column(children: [
            InfoRow(context.l10n.saleReference, m.reference ?? ''),
            InfoRow(context.l10n.dateAndTime, dateTime(m.occurredAt)),
            InfoRow(context.l10n.productLabel, m.productName),
            InfoRow(context.l10n.quantityConsumed, '${m.quantity}', bold: true),
            InfoRow(context.l10n.remainingStock, '${m.balanceAfter}'),
          ]),
        ),
        const SizedBox(height: 16),
        FilledButton.icon(
          onPressed: () => Navigator.of(context).push(MaterialPageRoute(
              builder: (_) => SaleDetailScreen(saleId: m.saleId!))),
          icon: const Icon(Icons.open_in_new),
          label: Text(context.l10n.viewSaleDetails),
        ),
      ]),
    );
  }
}
