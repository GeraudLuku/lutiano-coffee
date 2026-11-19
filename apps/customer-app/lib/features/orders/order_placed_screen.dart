import 'package:lutiano_core/lutiano_core.dart';
import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../data/models.dart';
import '../shell/main_shell.dart';
import 'order_detail_screen.dart';

/// The order was sent: its number, where, how to pay, and the way to follow it.
class OrderPlacedScreen extends StatelessWidget {
  const OrderPlacedScreen({super.key, required this.order});
  final Order order;

  @override
  Widget build(BuildContext context) {
    final l = context.l10n;
    return Scaffold(
      appBar: AppBar(automaticallyImplyLeading: false),
      body: ListView(padding: const EdgeInsets.all(20), children: [
        const SizedBox(height: 12),
        const Icon(Icons.check_circle, color: AppColors.green, size: 84),
        const SizedBox(height: 12),
        Text(l.orderSent,
            textAlign: TextAlign.center,
            style: const TextStyle(fontSize: 26, fontWeight: FontWeight.w800)),
        const SizedBox(height: 6),
        Text(l.orderSentMessage,
            textAlign: TextAlign.center,
            style: const TextStyle(color: AppColors.muted)),
        const SizedBox(height: 20),
        AppCard(
          child: Column(children: [
            InfoRow(l.orderNumber, order.reference, bold: true),
            InfoRow(l.salesPoint, order.operationName),
            InfoRow(l.paymentMethod, l.cashAtSalesPoint),
            InfoRow(l.totalToPay, xaf(order.totalXaf), bold: true),
          ]),
        ),
        const SizedBox(height: 20),
        FilledButton(
            onPressed: () => Navigator.of(context).pushReplacement(
                MaterialPageRoute(
                    builder: (_) => OrderDetailScreen(orderId: order.id))),
            child: Text(l.viewOrderDetails)),
        OutlinedButton(
            onPressed: () {
              Navigator.of(context).popUntil((r) => r.isFirst);
              MainShell.goTo(context, MainShell.menu);
            },
            child: Text(l.continueShopping)),
      ]),
    );
  }
}
