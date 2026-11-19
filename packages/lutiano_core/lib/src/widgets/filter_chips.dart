import 'package:material_ui/material_ui.dart';

import '../l10n/core_l10n.dart';
import '../core/theme.dart';

/// A row of choice chips: "All" plus the given options. [selected] null means all.
class FilterChips<T> extends StatelessWidget {
  const FilterChips(
      {super.key,
      required this.options,
      required this.selected,
      required this.onSelected,
      this.padding = const EdgeInsets.fromLTRB(16, 12, 16, 4)});

  /// Each option's value and its label.
  final List<(T, String)> options;
  final T? selected;
  final ValueChanged<T?> onSelected;
  final EdgeInsets padding;

  @override
  Widget build(BuildContext context) => Padding(
        padding: padding,
        child: SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          child: Row(children: [
            for (final (value, label) in [
              (null, context.coreL10n.all),
              ...options
            ])
              Padding(
                padding: const EdgeInsets.only(right: 8),
                child: ChoiceChip(
                  label: Text(label),
                  selected: selected == value,
                  showCheckmark: false,
                  selectedColor: AppColors.gold,
                  onSelected: (_) => onSelected(value),
                ),
              ),
          ]),
        ),
      );
}
