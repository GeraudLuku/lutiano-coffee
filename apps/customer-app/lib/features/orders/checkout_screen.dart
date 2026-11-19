import 'package:lutiano_core/lutiano_core.dart';
import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import 'order_placed_screen.dart';

/// Checkout: review the order, choose how to pay (cash at the sales point for now), add a note and place it.
class CheckoutScreen extends StatefulWidget {
  const CheckoutScreen({super.key});

  @override
  State<CheckoutScreen> createState() => _CheckoutScreenState();
}

class _CheckoutScreenState extends State<CheckoutScreen>
    with SubmitState<CheckoutScreen> {
  final _note = TextEditingController();

  @override
  void dispose() {
    _note.dispose();
    super.dispose();
  }

  Future<void> _place() async {
    final app = AppScope.read(context);
    final point = app.salesPoint;
    if (point == null || app.cart.isEmpty) return;
    Order? order;
    // the key stays the same while the cart does: a retry after a lost answer returns the order already placed
    final ok = await submit(() async => order = await app.orders.place(
        operationId: point.id,
        lines: app.cart.lines,
        key: app.cart.checkoutKey,
        note: _note.text.trim()));
    if (!ok || order == null || !mounted) return;
    final placed = order!;
    app.cart.clear();
    app.ordersChanged();
    await Navigator.of(context).pushReplacement(
        MaterialPageRoute(builder: (_) => OrderPlacedScreen(order: placed)));
  }

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final cart = app.cart;
    final l = context.l10n;
    final point = app.salesPoint;
    return Scaffold(
      appBar: AppBar(title: Text(l.checkout)),
      body: ListView(padding: const EdgeInsets.all(16), children: [
        if (point != null)
          AppCard(
              color: AppColors.goldSoft,
              child: Row(children: [
                const Icon(Icons.storefront_outlined),
                const SizedBox(width: 10),
                Expanded(
                    child: Text(point.name,
                        style: const TextStyle(fontWeight: FontWeight.w800)))
              ])),
        SectionLabel(l.orderReview),
        AppCard(
          child: Column(children: [
            for (final line in cart.lines)
              Padding(
                padding: const EdgeInsets.symmetric(vertical: 6),
                child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('${line.quantity}×  ',
                          style: const TextStyle(fontWeight: FontWeight.w800)),
                      Expanded(
                          child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                            Text(
                                line.sizeName == null
                                    ? line.name
                                    : '${line.name} · ${line.sizeName}',
                                style: const TextStyle(
                                    fontWeight: FontWeight.w700)),
                            if (line.addons.isNotEmpty)
                              Text(
                                  '+ ${line.addons.map((a) => a.name).join(', ')}',
                                  style: const TextStyle(
                                      color: AppColors.muted, fontSize: 12.5)),
                          ])),
                      Text(xaf(line.totalXaf)),
                    ]),
              ),
            const Divider(),
            InfoRow(l.totalToPay, xaf(cart.totalXaf), bold: true),
          ]),
        ),
        SectionLabel(l.paymentMethod),
        AppCard(
          child: Column(children: [
            ListTile(
              contentPadding: EdgeInsets.zero,
              leading:
                  const Icon(Icons.payments_outlined, color: AppColors.green),
              title: Text(l.cashAtSalesPoint),
              subtitle: Text(l.payAtTheCounter),
              trailing:
                  const Icon(Icons.radio_button_checked, color: AppColors.gold),
            ),
            ListTile(
              enabled: false,
              contentPadding: EdgeInsets.zero,
              leading: const Icon(Icons.phone_android),
              title: const Text('MTN MoMo · Orange Money'),
              subtitle: Text(l.comingSoon),
            ),
          ]),
        ),
        const SizedBox(height: 12),
        TextField(
            controller: _note,
            maxLength: 200,
            maxLines: 2,
            decoration: InputDecoration(
                labelText: l.noteForTheSeller, hintText: l.noteHint)),
        InfoNote(l.confirmedBySeller),
        const SizedBox(height: 12),
        if (error != null) ErrorBanner(error!),
      ]),
      bottomNavigationBar: SafeArea(
        child: Padding(
            padding: const EdgeInsets.all(12),
            child: FilledButton(
                onPressed: busy || cart.isEmpty ? null : _place,
                child: Text(busy ? l.placingOrder : l.placeOrder))),
      ),
    );
  }
}
