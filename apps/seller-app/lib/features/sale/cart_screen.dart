import 'package:material_ui/material_ui.dart';

import 'package:lutiano_core/lutiano_core.dart';
import '../../state/app_scope.dart';
import '../../state/cart_controller.dart';
import '../../widgets/widgets.dart';
import 'payment_screen.dart';
import 'sale_customer_card.dart';
import '../../core/l10n.dart';

/// Batch 1 · Screen 7 — Cart & Order Summary.
class CartScreen extends StatelessWidget {
  const CartScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final cart = app.cart;
    return ListenableBuilder(
      listenable: cart,
      builder: (context, _) => Scaffold(
        appBar: AppBar(
          title: Text(context.l10n.yourCart),
          actions: [
            if (!cart.isEmpty)
              TextButton(onPressed: cart.clear, child: Text(context.l10n.clear))
          ],
        ),
        body: cart.isEmpty
            ? EmptyState(
                icon: Icons.shopping_cart_outlined,
                title: context.l10n.yourCartIsEmpty,
                action: OutlinedButton(
                    onPressed: () => Navigator.pop(context),
                    child: Text(context.l10n.addProducts)))
            : AsyncView<Map<String, int>>(
                key: ValueKey(
                    'cart-stock-${app.operation!.id}-${app.dataVersion}'),
                load: () async => {
                  for (final s in (await app.inventory.stockPage(
                          app.operation!.id,
                          ids: [for (final l in cart.lines) l.product.id],
                          size: 200))
                      .items)
                    s.product.id: s.quantity
                },
                skeleton: const SkeletonList(count: 3),
                builder: (context, available, _) =>
                    ListView(padding: const EdgeInsets.all(16), children: [
                  for (final line in cart.lines)
                    _CartLineTile(
                        line: line, available: available[line.product.id] ?? 0),
                  const SizedBox(height: 8),
                  const SaleCustomerCard(),
                  const SizedBox(height: 12),
                  AppCard(
                    child: Column(children: [
                      Align(
                          alignment: Alignment.centerLeft,
                          child: Text(context.l10n.orderSummary,
                              style: TextStyle(
                                  fontWeight: FontWeight.w800, fontSize: 16))),
                      InfoRow(context.l10n.itemsCount(cart.itemCount),
                          xaf(cart.totalXaf)),
                      const Divider(),
                      InfoRow(context.l10n.total, xaf(cart.totalXaf),
                          bold: true),
                    ]),
                  ),
                  const SizedBox(height: 10),
                  InfoNote(context.l10n.pricesAreSetCentrallyAnd),
                ]),
              ),
        bottomNavigationBar: cart.isEmpty
            ? null
            : SafeArea(
                child: Padding(
                  padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
                  child: FilledButton(
                    onPressed: () => Navigator.of(context).push(
                        MaterialPageRoute(
                            builder: (_) => const PaymentScreen())),
                    child: Text(context.l10n.proceedToPayment),
                  ),
                ),
              ),
      ),
    );
  }
}

class _CartLineTile extends StatelessWidget {
  const _CartLineTile({required this.line, required this.available});
  final CartLine line;
  final int available;

  @override
  Widget build(BuildContext context) {
    final cart = AppScope.read(context).cart;
    final p = line.product;
    final qty = line.quantity;
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: AppCard(
        padding: const EdgeInsets.all(10),
        child: Row(children: [
          ProductImage(url: p.imageUrl, size: 56),
          const SizedBox(width: 12),
          Expanded(
            child:
                Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(p.name, style: const TextStyle(fontWeight: FontWeight.w800)),
              Text(xaf(p.priceXaf),
                  style:
                      const TextStyle(color: AppColors.muted, fontSize: 12.5)),
              const SizedBox(height: 6),
              QuantityStepper(
                  value: qty,
                  onMinus: () => cart.remove(p.id),
                  onPlus: qty >= available
                      ? null
                      : () => cart.add(p, available: available)),
              if (qty > available)
                Text(context.l10n.notEnoughStockReduceThe,
                    style: TextStyle(color: AppColors.red, fontSize: 12)),
            ]),
          ),
          Column(crossAxisAlignment: CrossAxisAlignment.end, children: [
            IconButton(
                tooltip: context.l10n.remove,
                onPressed: () => cart.deleteLine(p.id),
                icon:
                    const Icon(Icons.close, size: 20, color: AppColors.muted)),
            Text(xaf(p.priceXaf * qty),
                style: const TextStyle(fontWeight: FontWeight.w800)),
          ]),
        ]),
      ),
    );
  }
}
