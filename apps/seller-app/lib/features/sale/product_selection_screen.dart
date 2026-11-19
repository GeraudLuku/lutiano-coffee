import 'package:material_ui/material_ui.dart';

import 'package:lutiano_core/lutiano_core.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import 'cart_bar.dart';
import 'cart_screen.dart';
import '../../core/l10n.dart';

/// Batch 1 · Screen 6 — Product Selection (central prices; only stock held at this operation can be sold).
class ProductSelectionScreen extends StatefulWidget {
  const ProductSelectionScreen({super.key});
  static const routeName = '/sale/products';

  @override
  State<ProductSelectionScreen> createState() => _ProductSelectionScreenState();
}

class _ProductSelectionScreenState extends State<ProductSelectionScreen> {
  String? _category;
  String _query = '';
  List<String> _categories = const [];

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final op = app.operation!;
    return Scaffold(
      appBar: AppBar(title: Text(context.l10n.selectProducts)),
      body: Column(children: [
        SearchFilterBar(
          hint: context.l10n.searchProduct,
          categories: _categories,
          category: _category,
          onSearch: (q) => setState(() => _query = q),
          onCategory: (c) => setState(() => _category = c),
        ),
        Expanded(
          child: PagedListView<StockItem>(
            key: ValueKey(
                'stock-${op.id}-${app.dataVersion}-$_category-$_query'),
            columns: 2,
            childAspectRatio: 0.72,
            padding: const EdgeInsets.fromLTRB(16, 4, 16, 80),
            fetch: (page, size) => app.inventory.stockPage(op.id,
                q: _query, category: _category, page: page, size: size),
            onPage: (p) {
              final cats = [
                for (final c in (p.summary['categories'] as List? ?? const []))
                  '$c'
              ];
              if (cats.length != _categories.length) {
                WidgetsBinding.instance.addPostFrameCallback((_) {
                  if (mounted) setState(() => _categories = cats);
                });
              }
            },
            skeleton: const SkeletonProductCard(),
            empty: EmptyState(
                icon: Icons.local_cafe_outlined,
                title: context.l10n.noProducts,
                message: context.l10n.nothingMatchesOrNoStock),
            itemBuilder: (context, s, _, __) => _ProductCard(item: s),
          ),
        ),
      ]),
      bottomNavigationBar: CartBar(
        label: context.l10n.viewCart,
        onPressed: () => Navigator.of(context)
            .push(MaterialPageRoute(builder: (_) => const CartScreen())),
      ),
    );
  }
}

class _ProductCard extends StatelessWidget {
  const _ProductCard({required this.item});
  final StockItem item;

  @override
  Widget build(BuildContext context) {
    final cart = AppScope.of(context).cart;
    return ListenableBuilder(
      listenable: cart,
      builder: (context, _) {
        final inCart = cart.quantityOf(item.product.id);
        final remaining = item.quantity - inCart;
        return AppCard(
          padding: EdgeInsets.zero,
          child:
              Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Expanded(
                child: SizedBox(
                    width: double.infinity,
                    child:
                        ProductImage(url: item.product.imageUrl, radius: 0))),
            Padding(
              padding: const EdgeInsets.fromLTRB(10, 8, 8, 10),
              child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(item.product.name,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(fontWeight: FontWeight.w800)),
                    Text(
                        remaining <= 0
                            ? context.l10n.outOfStock
                            : context.l10n.availableCount(remaining),
                        style: TextStyle(
                            fontSize: 12,
                            color: remaining <= 0
                                ? AppColors.red
                                : AppColors.muted)),
                    const SizedBox(height: 4),
                    Row(children: [
                      Expanded(
                          child: Text(xaf(item.product.priceXaf),
                              style: const TextStyle(
                                  fontWeight: FontWeight.w800))),
                      if (inCart > 0)
                        Padding(
                            padding: const EdgeInsets.only(right: 6),
                            child: StatusChip('×$inCart',
                                color: AppColors.navy,
                                background: AppColors.goldSoft)),
                      AddButton(
                          onPressed: remaining <= 0
                              ? null
                              : () => cart.add(item.product,
                                  available: item.quantity)),
                    ]),
                  ]),
            ),
          ]),
        );
      },
    );
  }
}
