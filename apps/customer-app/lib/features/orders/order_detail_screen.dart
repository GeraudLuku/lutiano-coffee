import 'package:lutiano_core/lutiano_core.dart';
import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../state/cart_controller.dart';
import '../cart/cart_screen.dart';
import 'order_widgets.dart';

/// One order: where it stands, what was ordered, the receipt, buy again, and cancelling while it is still new.
class OrderDetailScreen extends StatefulWidget {
  const OrderDetailScreen({super.key, required this.orderId});
  final String orderId;

  @override
  State<OrderDetailScreen> createState() => _OrderDetailScreenState();
}

class _OrderDetailScreenState extends State<OrderDetailScreen>
    with SubmitState<OrderDetailScreen> {
  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final l = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l.orderDetails)),
      body: AsyncView<Order>(
        key: ValueKey('order-${widget.orderId}-${app.ordersVersion}'),
        load: () => app.orders.get(widget.orderId),
        builder: (context, o, _) =>
            ListView(padding: const EdgeInsets.all(16), children: [
          Row(children: [
            Expanded(
                child: Text(o.reference,
                    style: const TextStyle(
                        fontSize: 20, fontWeight: FontWeight.w800))),
            orderStatusChip(context, o.status),
          ]),
          const SizedBox(height: 4),
          Text('${o.operationName} · ${dateTime(o.placedAt)}',
              style: const TextStyle(color: AppColors.muted)),
          if (o.reason != null &&
              o.reason!.isNotEmpty &&
              (o.status == OrderStatus.rejected)) ...[
            const SizedBox(height: 10),
            InfoNote(o.reason!,
                color: AppColors.red, background: AppColors.redSoft),
          ],
          SectionLabel(l.items),
          AppCard(
            child: Column(children: [
              for (final line in o.lines)
                Padding(
                  padding: const EdgeInsets.symmetric(vertical: 6),
                  child: Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('${line.quantity}×  ',
                            style:
                                const TextStyle(fontWeight: FontWeight.w800)),
                        Expanded(
                            child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                              Text(line.name,
                                  style: const TextStyle(
                                      fontWeight: FontWeight.w700)),
                              if (line.addons.isNotEmpty)
                                Text('+ ${line.addons.join(', ')}',
                                    style: const TextStyle(
                                        color: AppColors.muted,
                                        fontSize: 12.5)),
                            ])),
                        Text(xaf(line.totalXaf)),
                      ]),
                ),
              const Divider(),
              InfoRow(l.total, xaf(o.totalXaf), bold: true),
              InfoRow(l.paymentMethod, l.cashAtSalesPoint),
              if (o.note != null && o.note!.isNotEmpty)
                InfoRow(l.noteForTheSeller, o.note!),
            ]),
          ),
          SectionLabel(l.orderTracking),
          AppCard(child: OrderTimeline(steps: o.timeline)),
          const SizedBox(height: 16),
          if (error != null) ErrorBanner(error!),
          if (o.status == OrderStatus.collected)
            OutlinedButton.icon(
                icon: const Icon(Icons.receipt_outlined),
                label: Text(l.viewReceipt),
                onPressed: () => openPdf(context,
                    fetch: () => app.orders.receiptPdf(o.id),
                    fileName: 'lutiano-${o.reference}.pdf',
                    preparing: l.preparingReceipt)),
          OutlinedButton.icon(
              icon: const Icon(Icons.replay),
              label: Text(l.buyAgain),
              onPressed: busy ? null : () => _buyAgain(o)),
          if (o.status == OrderStatus.placed)
            TextButton(
                style: TextButton.styleFrom(foregroundColor: AppColors.red),
                onPressed: busy ? null : () => _cancel(o),
                child: Text(l.cancelOrder)),
        ]),
      ),
    );
  }

  Future<void> _cancel(Order o) async {
    final l = context.l10n;
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(l.cancelOrderQuestion),
        content: Text(l.cancelOrderMessage),
        actions: [
          TextButton(
              onPressed: () => Navigator.of(ctx).pop(false),
              child: Text(l.keepOrder)),
          FilledButton(
              style: FilledButton.styleFrom(backgroundColor: AppColors.red),
              onPressed: () => Navigator.of(ctx).pop(true),
              child: Text(l.cancelOrder)),
        ],
      ),
    );
    if (ok != true || !mounted) return;
    final app = AppScope.read(context);
    final done = await submit(() => app.orders.cancel(o.id));
    if (done) app.ordersChanged();
  }

  /// Puts the lines of this order, at today's prices, back in the cart for its sales point.
  Future<void> _buyAgain(Order o) async {
    final app = AppScope.read(context);
    final l = context.l10n;
    final nav = Navigator.of(context);
    late List<ReorderLine> lines;
    late SalesPoint point;
    final ok = await submit(() async {
      lines = await app.orders.reorder(o.id);
      point = await app.catalogue.salesPoint(o.operationId);
    });
    if (!ok || !mounted) return;
    final available = lines.where((r) => r.available).toList();
    if (available.isEmpty) {
      showMessage(context, l.nothingToReorder, error: true);
      return;
    }
    app.selectSalesPoint(point);
    for (final r in available) {
      app.cart.add(CartLine(
          productId: r.productId,
          name: r.name,
          baseXaf: r.unitXaf - r.addons.fold(0, (s, a) => s + a.priceXaf),
          addons: r.addons,
          quantity: r.quantity));
    }
    if (available.length < lines.length && mounted) {
      showMessage(context, l.someItemsUnavailable);
    }
    await nav.push(MaterialPageRoute(builder: (_) => const CartScreen()));
  }
}
