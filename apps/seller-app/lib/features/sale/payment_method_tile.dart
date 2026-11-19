import 'package:material_ui/material_ui.dart';

import 'package:lutiano_core/lutiano_core.dart';
import '../../data/models.dart';
import '../../widgets/widgets.dart';
import '../../core/l10n.dart';

/// One payment method option. Methods that are not available yet (Mobile Money) are greyed out and cannot be picked.
class PaymentMethodTile extends StatelessWidget {
  const PaymentMethodTile(
      {super.key,
      required this.method,
      required this.selected,
      required this.onTap});
  final PaymentMethodOption method;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final usable = method.usable;
    final tile = AppCard(
      color: selected
          ? AppColors.goldSoft
          : (usable ? null : AppColors.background),
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
      onTap: usable ? onTap : null,
      child: Row(children: [
        Icon(method.isCash ? Icons.payments_outlined : Icons.phone_android,
            color: usable ? AppColors.navy : AppColors.muted),
        const SizedBox(width: 12),
        Expanded(
          child:
              Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text(method.label,
                style: TextStyle(
                    fontWeight: FontWeight.w800,
                    color: usable ? AppColors.ink : AppColors.muted)),
            if (!usable)
              Text(context.l10n.comingSoon,
                  style: TextStyle(fontSize: 12, color: AppColors.muted)),
          ]),
        ),
        if (selected)
          const Icon(Icons.check_circle, color: AppColors.gold)
        else
          Icon(usable ? Icons.radio_button_unchecked : Icons.lock_outline,
              color: AppColors.muted, size: 20),
      ]),
    );
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Semantics(
        button: true,
        enabled: usable,
        selected: selected,
        child: Opacity(
          opacity: usable ? 1 : 0.6,
          child: tile,
        ),
      ),
    );
  }
}
