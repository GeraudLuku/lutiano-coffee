import 'package:lutiano_core/lutiano_core.dart';
import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../state/app_scope.dart';
import '../orders/checkout_screen.dart';
import '../shell/main_shell.dart';

/// The cart of the sales point being ordered from.
class CartScreen extends StatelessWidget {
  const CartScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final cart = app.cart;
    final l = context.l10n;
    return ListenableBuilder(
      listenable: cart,
      builder: (context, _) => Scaffold(
        appBar: AppBar(title: Text(l.yourCart), actions: [
          if (!cart.isEmpty)
            TextButton(onPressed: cart.clear, child: Text(l.clear)),
        ]),
        body: cart.isEmpty
            ? Center(
                child: EmptyState(
                    icon: Icons.shopping_bag_outlined,
                    title: l.cartEmpty,
                    message: l.cartEmptyMessage,
                    action: FilledButton(
                        onPressed: () {
                          Navigator.of(context).popUntil((r) => r.isFirst);
                          MainShell.goTo(context, MainShell.menu);
                        },
                        child: Text(l.browseMenu))))
            : ListView(padding: const EdgeInsets.all(16), children: [
                if (app.salesPoint != null)
                  InfoRow(l.salesPoint, app.salesPoint!.name, bold: true),
                const SizedBox(height: 8),
                for (final line in cart.lines)
                  AppCard(
                    child: Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Expanded(
                              child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                Text(line.name,
                                    style: const TextStyle(
                                        fontWeight: FontWeight.w800)),
                                if (line.sizeName != null)
                                  Text(line.sizeName!,
                                      style: const TextStyle(
                                          color: AppColors.muted,
                                          fontSize: 12.5)),
                                if (line.addons.isNotEmpty)
                                  Text(
                                      '+ ${line.addons.map((a) => a.name).join(', ')}',
                                      style: const TextStyle(
                                          color: AppColors.muted,
                                          fontSize: 12.5)),
                                const SizedBox(height: 6),
                                QuantityStepper(
                                    value: line.quantity,
                                    onMinus: () => cart.setQuantity(
                                        line.key, line.quantity - 1),
                                    onPlus: () => cart.setQuantity(
                                        line.key, line.quantity + 1)),
                              ])),
                          Text(xaf(line.totalXaf),
                              style:
                                  const TextStyle(fontWeight: FontWeight.w800)),
                        ]),
                  ),
                const Divider(height: 28),
                InfoRow(l.total, xaf(cart.totalXaf), bold: true),
                const SizedBox(height: 12),
              ]),
        bottomNavigationBar: cart.isEmpty
            ? null
            : SafeArea(
                child: Padding(
                    padding: const EdgeInsets.all(12),
                    child: FilledButton(
                        onPressed: () => Navigator.of(context).push(
                            MaterialPageRoute(
                                builder: (_) => const CheckoutScreen())),
                        child: Text(l.proceedToCheckout)))),
      ),
    );
  }
}
