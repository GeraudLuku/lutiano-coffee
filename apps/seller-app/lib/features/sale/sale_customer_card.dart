import 'package:material_ui/material_ui.dart';

import 'package:lutiano_core/lutiano_core.dart';
import '../../state/app_scope.dart';
import '../../state/cart_controller.dart';
import '../../widgets/widgets.dart';
import '../customer/scan_customer_screen.dart';
import '../../core/l10n.dart';

/// Optional customer for the sale (Batch 1 · Screen 5): scan their loyalty QR so they earn points.
/// The customer must show their own QR, so points can never be awarded to someone who did not agree.
class SaleCustomerCard extends StatefulWidget {
  const SaleCustomerCard({super.key, this.onScan = scanCustomerQr});

  /// Returns the scanned QR text, or null. Replaceable so the flow can be tested without a camera.
  final Future<String?> Function(BuildContext context) onScan;

  @override
  State<SaleCustomerCard> createState() => _SaleCustomerCardState();
}

class _SaleCustomerCardState extends State<SaleCustomerCard>
    with SubmitState<SaleCustomerCard> {
  Future<void> _scan() async {
    final app = AppScope.read(context);
    final token = await widget.onScan(context);
    if (token == null || !mounted) return;
    await submit(() async =>
        app.cart.attachCustomer(await app.customers.scan(token), token));
  }

  @override
  Widget build(BuildContext context) {
    final cart = AppScope.of(context).cart;
    return ListenableBuilder(
        listenable: cart, builder: (context, _) => _card(cart));
  }

  Widget _card(CartController cart) {
    final customer = cart.customer;
    return AppCard(
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Text(context.l10n.customerOptional,
            style: TextStyle(fontWeight: FontWeight.w800, fontSize: 16)),
        const SizedBox(height: 10),
        if (customer == null)
          OutlinedButton.icon(
            onPressed: busy ? null : _scan,
            icon: const Icon(Icons.qr_code_scanner),
            label: Text(
                busy ? context.l10n.checking : context.l10n.scanCustomerQr),
          )
        else
          Row(children: [
            CircleAvatar(
                backgroundColor: AppColors.goldSoft,
                child: Text(
                    customer.fullName.isEmpty
                        ? '?'
                        : customer.fullName[0].toUpperCase(),
                    style: const TextStyle(
                        color: AppColors.navy, fontWeight: FontWeight.w800))),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(customer.fullName,
                        style: const TextStyle(fontWeight: FontWeight.w800)),
                    Text(
                        [
                          customer.code,
                          if (customer.tierName != null) customer.tierName!,
                          context.l10n
                              .pointsValue(groupDigits(customer.pointsBalance))
                        ].join(' · '),
                        style: const TextStyle(
                            color: AppColors.muted, fontSize: 12.5)),
                  ]),
            ),
            IconButton(
                tooltip: context.l10n.removeCustomer,
                onPressed: cart.detachCustomer,
                icon: const Icon(Icons.close, color: AppColors.muted)),
          ]),
        if (error != null) ...[const SizedBox(height: 8), ErrorBanner(error!)],
      ]),
    );
  }
}
