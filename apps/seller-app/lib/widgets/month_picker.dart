import 'package:material_ui/material_ui.dart';

import 'package:lutiano_core/lutiano_core.dart';
import '../core/l10n.dart';

/// Picks one of the last months (yyyy-MM), newest first.
class MonthPicker extends StatelessWidget {
  const MonthPicker(
      {super.key,
      required this.value,
      required this.onChanged,
      this.count = 12,
      this.padding = const EdgeInsets.fromLTRB(16, 12, 16, 0)});
  final String value;
  final ValueChanged<String> onChanged;
  final int count;
  final EdgeInsets padding;

  @override
  Widget build(BuildContext context) {
    final months = recentMonths(count);
    return Padding(
      padding: padding,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12),
        decoration: BoxDecoration(
            color: Colors.white,
            border: Border.all(color: AppColors.line),
            borderRadius: BorderRadius.circular(12)),
        child: DropdownButtonHideUnderline(
          child: DropdownButton<String>(
            isExpanded: true,
            value: months.contains(value) ? value : null,
            icon: const Icon(Icons.keyboard_arrow_down),
            hint: Text(context.l10n.monthLabelTitle),
            items: [
              for (final m in months)
                DropdownMenuItem(
                    value: m,
                    child: Row(children: [
                      const Icon(Icons.calendar_today_outlined,
                          size: 18, color: AppColors.muted),
                      const SizedBox(width: 10),
                      Text(monthLabel(m)),
                    ])),
            ],
            onChanged: (m) => m == null ? null : onChanged(m),
          ),
        ),
      ),
    );
  }
}
