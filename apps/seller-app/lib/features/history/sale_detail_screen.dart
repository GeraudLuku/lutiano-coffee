import 'package:material_ui/material_ui.dart';

import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import '../earnings/earning_detail_screen.dart';
import '../sale/receipt_actions.dart';
import '../../core/l10n.dart';

class SaleDetailScreen extends StatelessWidget {
  const SaleDetailScreen({super.key, required this.saleId});
  final String saleId;

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    return Scaffold(
      appBar: AppBar(title: Text(context.l10n.saleDetails)),
      body: AsyncView<Sale>(
        load: () => app.sales.get(saleId),
        builder: (context, sale, _) =>
            ListView(padding: const EdgeInsets.all(16), children: [
          AppCard(
            child: Column(children: [
              Row(children: [
                Expanded(
                    child: Text(sale.reference,
                        style: const TextStyle(
                            fontWeight: FontWeight.w800, fontSize: 18))),
                completedChip(context),
              ]),
              const SizedBox(height: 6),
              InfoRow(context.l10n.dateTime, dateTime(sale.createdAt)),
              InfoRow(context.l10n.salesOperation, sale.operationName),
              InfoRow(context.l10n.paymentMethod,
                  paymentMethodLabel(context.l10n, sale.paymentMethod)),
              if (sale.customerName != null)
                InfoRow(context.l10n.customer, sale.customerName!),
              if (sale.pointsEarned != null && sale.pointsEarned! > 0)
                InfoRow(context.l10n.loyaltyPointsEarned,
                    context.l10n.pointsEarnedValue(sale.pointsEarned!)),
              if (sale.cashReceivedXaf != null)
                InfoRow(context.l10n.cashReceived, xaf(sale.cashReceivedXaf!)),
              if (sale.changeXaf != null)
                InfoRow(context.l10n.changeGiven, xaf(sale.changeXaf!)),
            ]),
          ),
          SectionLabel(context.l10n.itemsCount(sale.itemCount)),
          AppCard(
            child: Column(children: [
              for (final l in sale.lines)
                Padding(
                  padding: const EdgeInsets.symmetric(vertical: 6),
                  child: Row(children: [
                    ProductImage(url: l.imageUrl, size: 44),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(l.productName,
                                style: const TextStyle(
                                    fontWeight: FontWeight.w700)),
                            Text('${l.quantity} × ${xaf(l.unitPriceXaf)}',
                                style: const TextStyle(
                                    color: AppColors.muted, fontSize: 12.5)),
                          ]),
                    ),
                    Text(xaf(l.lineTotalXaf),
                        style: const TextStyle(fontWeight: FontWeight.w800)),
                  ]),
                ),
              const Divider(),
              InfoRow(context.l10n.totalAmount, xaf(sale.totalXaf), bold: true),
            ]),
          ),
          const SizedBox(height: 16),
          OutlinedButton.icon(
            onPressed: () => openReceipt(context, sale.id, sale.reference),
            icon: const Icon(Icons.picture_as_pdf_outlined),
            label: Text(context.l10n.viewPrintReceipt),
          ),
          const SizedBox(height: 8),
          OutlinedButton.icon(
            onPressed: () => Navigator.of(context).push(MaterialPageRoute(
                builder: (_) => EarningDetailScreen(saleId: sale.id))),
            icon: const Icon(Icons.savings_outlined),
            label: Text(context.l10n.myEarnings),
          ),
        ]),
      ),
    );
  }
}
