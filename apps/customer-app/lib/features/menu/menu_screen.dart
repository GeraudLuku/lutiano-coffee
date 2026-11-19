import 'package:lutiano_core/lutiano_core.dart';
import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../cart/cart_screen.dart';
import '../scan/sales_point_banner.dart';
import 'menu_widgets.dart';
import 'product_screen.dart';

/// The menu: search and categories over a lazily loaded list. With a sales point chosen it is the menu of that point (with what is
/// available there now); otherwise the whole menu. [standalone] adds its own back arrow (when opened from Home).
class MenuScreen extends StatefulWidget {
  const MenuScreen({super.key, this.initialCategory, this.standalone = false});
  final String? initialCategory;
  final bool standalone;

  @override
  State<MenuScreen> createState() => _MenuScreenState();
}

class _MenuScreenState extends State<MenuScreen> {
  late String? _category = widget.initialCategory;
  String _query = '';
  List<String> _categories = const [];

  @override
  void initState() {
    super.initState();
    AppScope.read(context).catalogue.categories().then((c) {
      if (mounted) setState(() => _categories = c.map((e) => e.name).toList());
    }).catchError((_) {});
  }

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final l = context.l10n;
    final point = app.salesPoint;
    return Scaffold(
      appBar: AppBar(
        automaticallyImplyLeading: widget.standalone,
        title: Text(l.menu),
        actions: [
          IconButton(
              tooltip: l.cart,
              onPressed: () => Navigator.of(context)
                  .push(MaterialPageRoute(builder: (_) => const CartScreen())),
              icon: Badge(
                  isLabelVisible: app.cart.count > 0,
                  label: Text('${app.cart.count}'),
                  child: const Icon(Icons.shopping_bag_outlined))),
        ],
      ),
      body: Column(children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
          child: const SalesPointBanner(),
        ),
        SearchFilterBar(
          hint: l.searchMenu,
          categories: _categories,
          category: _category,
          onSearch: (q) => setState(() => _query = q),
          onCategory: (c) => setState(() => _category = c),
        ),
        Expanded(
          child: PagedListView<MenuItem>(
            key: ValueKey('menu-${point?.id}-$_category-$_query'),
            fetch: (page, size) => point == null
                ? app.catalogue.products(
                    category: _category, q: _query, page: page, size: size)
                : app.catalogue.productsAt(point.id,
                    category: _category, q: _query, page: page, size: size),
            empty: EmptyState(
                icon: Icons.local_cafe_outlined,
                title: l.noProductsFound,
                message: l.tryAnotherSearch),
            itemBuilder: (context, item, _, __) => MenuRow(
                item: item,
                onTap: () => Navigator.of(context).push(MaterialPageRoute(
                    builder: (_) => ProductScreen(item: item)))),
          ),
        ),
      ]),
    );
  }
}
