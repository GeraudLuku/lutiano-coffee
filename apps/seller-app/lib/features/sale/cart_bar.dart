import 'package:material_ui/material_ui.dart';

import 'package:lutiano_core/lutiano_core.dart';
import '../../state/app_scope.dart';

/// Sticky bottom bar with item count, total and a primary action; hidden while the cart is empty.
class CartBar extends StatelessWidget {
  const CartBar({super.key, required this.label, required this.onPressed});
  final String label;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    final cart = AppScope.of(context).cart;
    return ListenableBuilder(
      listenable: cart,
      builder: (context, _) => cart.isEmpty
          ? const SizedBox.shrink()
          : SafeArea(
              child: Container(
                margin: const EdgeInsets.fromLTRB(16, 0, 16, 12),
                padding: const EdgeInsets.fromLTRB(16, 10, 10, 10),
                decoration: BoxDecoration(
                    color: AppColors.navy,
                    borderRadius: BorderRadius.circular(14)),
                child: Row(children: [
                  Expanded(
                    child: Column(
                        mainAxisSize: MainAxisSize.min,
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                              '${cart.itemCount} ${cart.itemCount == 1 ? 'item' : 'items'}',
                              style: const TextStyle(color: AppColors.white70)),
                          Text(xaf(cart.totalXaf),
                              style: const TextStyle(
                                  color: Colors.white,
                                  fontSize: 18,
                                  fontWeight: FontWeight.w800)),
                        ]),
                  ),
                  SizedBox(
                    width: 170,
                    child: FilledButton(
                      onPressed: onPressed,
                      style: FilledButton.styleFrom(
                          minimumSize: const Size(0, 46)),
                      child: Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Text(label),
                            const Icon(Icons.chevron_right)
                          ]),
                    ),
                  ),
                ]),
              ),
            ),
    );
  }
}
