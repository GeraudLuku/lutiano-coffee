import 'package:material_ui/material_ui.dart';

import '../core/theme.dart';

/// A small coloured pill with a status word (green by default).
class StatusChip extends StatelessWidget {
  const StatusChip(this.label,
      {super.key,
      this.color = AppColors.green,
      this.background = AppColors.greenSoft});

  final String label;
  final Color color, background;

  @override
  Widget build(BuildContext context) => Container(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
        decoration: BoxDecoration(
            color: background, borderRadius: BorderRadius.circular(20)),
        child: Text(label,
            style: TextStyle(
                color: color, fontWeight: FontWeight.w700, fontSize: 12)),
      );
}
