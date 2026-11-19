import 'dart:async';

import 'package:material_ui/material_ui.dart';

import '../l10n/core_l10n.dart';
import '../core/theme.dart';

/// A search box (sent to the server after a short pause in typing) and category chips ("All" plus the given categories).
class SearchFilterBar extends StatefulWidget {
  const SearchFilterBar({
    super.key,
    required this.hint,
    required this.categories,
    required this.category,
    required this.onSearch,
    required this.onCategory,
    this.padding = const EdgeInsets.fromLTRB(16, 8, 16, 0),
  });

  final String hint;

  /// Category names without "All"; empty until the first page has told us which exist.
  final List<String> categories;

  /// The selected category, or null for all.
  final String? category;
  final ValueChanged<String> onSearch;
  final ValueChanged<String?> onCategory;
  final EdgeInsets padding;

  @override
  State<SearchFilterBar> createState() => _SearchFilterBarState();
}

class _SearchFilterBarState extends State<SearchFilterBar> {
  Timer? _pause;

  @override
  void dispose() {
    _pause?.cancel();
    super.dispose();
  }

  void _typed(String text) {
    _pause?.cancel();
    _pause = Timer(
        const Duration(milliseconds: 350), () => widget.onSearch(text.trim()));
  }

  @override
  Widget build(BuildContext context) => Padding(
        padding: widget.padding,
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          TextField(
            decoration: InputDecoration(
                hintText: widget.hint, prefixIcon: const Icon(Icons.search)),
            textInputAction: TextInputAction.search,
            onChanged: _typed,
            onSubmitted: (v) {
              _pause?.cancel();
              widget.onSearch(v.trim());
            },
          ),
          if (widget.categories.isNotEmpty) ...[
            const SizedBox(height: 10),
            SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              child: Row(children: [
                for (final c in [null, ...widget.categories])
                  Padding(
                    padding: const EdgeInsets.only(right: 8),
                    child: ChoiceChip(
                      label: Text(c ?? context.coreL10n.all),
                      selected: widget.category == c,
                      showCheckmark: false,
                      selectedColor: AppColors.gold,
                      onSelected: (_) => widget.onCategory(c),
                    ),
                  ),
              ]),
            ),
          ],
          const SizedBox(height: 8),
        ]),
      );
}
