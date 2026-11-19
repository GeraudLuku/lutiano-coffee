import 'package:material_ui/material_ui.dart';

import '../core/theme.dart';
import '../l10n/core_l10n.dart';

class QuantityStepper extends StatelessWidget {
  const QuantityStepper(
      {super.key, required this.value, this.onMinus, this.onPlus, this.onEdit});
  final int value;
  final VoidCallback? onMinus, onPlus;

  /// When given, tapping the number calls it (to type a quantity instead of tapping + many times).
  final VoidCallback? onEdit;

  @override
  Widget build(BuildContext context) =>
      Row(mainAxisSize: MainAxisSize.min, children: [
        _btn(Icons.remove, onMinus, context.coreL10n.decreaseQuantity),
        InkWell(
          onTap: onEdit,
          child: SizedBox(
              width: onEdit == null ? 30 : 44,
              height: 40,
              child: Center(
                  child: Text('$value',
                      style: const TextStyle(fontWeight: FontWeight.w800)))),
        ),
        _btn(Icons.add, onPlus, context.coreL10n.increaseQuantity),
      ]);

  Widget _btn(IconData icon, VoidCallback? onTap, String label) => SizedBox(
        width: 40,
        height: 40,
        child: IconButton.outlined(
          tooltip: label,
          padding: EdgeInsets.zero,
          iconSize: 18,
          onPressed: onTap,
          icon: Icon(icon),
          style: IconButton.styleFrom(
              side: const BorderSide(color: AppColors.line)),
        ),
      );
}

/// Round gold "+" used on product cards.
class AddButton extends StatelessWidget {
  const AddButton({super.key, this.onPressed});
  final VoidCallback? onPressed;

  @override
  Widget build(BuildContext context) => SizedBox(
        width: 40,
        height: 40,
        child: IconButton.filled(
          tooltip: context.coreL10n.addToCart,
          padding: EdgeInsets.zero,
          onPressed: onPressed,
          icon: const Icon(Icons.add, size: 20),
          style: IconButton.styleFrom(
              backgroundColor: AppColors.gold,
              foregroundColor: AppColors.navy,
              disabledBackgroundColor: AppColors.line),
        ),
      );
}
