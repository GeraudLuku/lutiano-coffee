import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import 'package:lutiano_core/lutiano_core.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import 'stock_detail_screen.dart';
import 'stock_widgets.dart';

/// Batch 2 · Screens 2 & 6 — My Inventory list: searched and filtered by the server, loaded a page at a time.
class StockListScreen extends StatefulWidget {
  const StockListScreen({super.key});

  @override
  State<StockListScreen> createState() => _StockListScreenState();
}

class _StockListScreenState extends State<StockListScreen> {
  String? _category;
  String _query = '';
  List<String> _categories = const [];

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final op = app.operation!;
    return Scaffold(
      appBar: AppBar(title: Text(context.l10n.myInventory)),
      body: Column(children: [
        SearchFilterBar(
          hint: context.l10n.searchProduct,
          categories: _categories,
          category: _category,
          onSearch: (q) => setState(() => _query = q),
          onCategory: (c) => setState(() => _category = c),
        ),
        Align(
            alignment: Alignment.centerLeft,
            child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16),
                child: Text(op.name,
                    style: const TextStyle(color: AppColors.muted)))),
        Expanded(
          child: PagedListView<StockItem>(
            key: ValueKey(
                'stock-list-${op.id}-${app.dataVersion}-$_category-$_query'),
            fetch: (page, size) => app.inventory.stockPage(op.id,
                q: _query, category: _category, page: page, size: size),
            onPage: (p) {
              final cats = [
                for (final c in (p.summary['categories'] as List? ?? const []))
                  '$c'
              ];
              if (cats.length != _categories.length) {
                WidgetsBinding.instance.addPostFrameCallback(
                    (_) => mounted ? setState(() => _categories = cats) : null);
              }
            },
            skeleton: const SkeletonCard(),
            empty: EmptyState(
                icon: Icons.inventory_2_outlined,
                title: context.l10n.noProductsMatch),
            itemBuilder: (context, s, _, __) => StockTile(
                item: s,
                onTap: () => Navigator.of(context).push(MaterialPageRoute(
                    builder: (_) => StockDetailScreen(item: s)))),
          ),
        ),
      ]),
    );
  }
}
