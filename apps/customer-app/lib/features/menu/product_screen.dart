import 'package:lutiano_core/lutiano_core.dart';
import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../state/cart_controller.dart';
import '../cart/cart_screen.dart';
import '../scan/sales_points_screen.dart';

/// One product: photo, description, size, add-ons and quantity, then "Add to cart".
class ProductScreen extends StatefulWidget {
  const ProductScreen({super.key, required this.item});
  final MenuItem item;

  @override
  State<ProductScreen> createState() => _ProductScreenState();
}

class _ProductScreenState extends State<ProductScreen> {
  late ProductSize? _size =
      widget.item.sizes.isEmpty ? null : widget.item.sizes.first;
  final Set<String> _addons = {};
  int _quantity = 1;

  MenuItem get item => widget.item;

  CartLine get _line => CartLine.of(item,
      size: _size,
      addons: item.addons.where((a) => _addons.contains(a.id)).toList(),
      quantity: _quantity);

  Future<void> _add() async {
    final app = AppScope.read(context);
    if (app.salesPoint == null) {
      final chosen = await Navigator.of(context).push<SalesPoint>(
          MaterialPageRoute(
              builder: (_) => const SalesPointsScreen(pick: true)));
      if (chosen == null || !mounted) return;
      app.selectSalesPoint(chosen);
    }
    final line = _line;
    app.cart.add(line);
    if (!mounted) return;
    final l = context.l10n;
    await showDialog<void>(
      context: context,
      builder: (ctx) => AlertDialog(
        icon: const Icon(Icons.check_circle, color: AppColors.green, size: 48),
        title: Text(l.addedToCart),
        content: Text(l.addedToCartMessage(item.name)),
        actions: [
          TextButton(
              onPressed: () => Navigator.of(ctx).pop(),
              child: Text(l.continueShopping)),
          FilledButton(
              onPressed: () {
                Navigator.of(ctx).pop();
                Navigator.of(context).push(
                    MaterialPageRoute(builder: (_) => const CartScreen()));
              },
              child: Text(l.viewCart(app.cart.count))),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final l = context.l10n;
    final app = AppScope.of(context);
    final fav = app.isFavourite(item.id);
    return Scaffold(
      appBar: AppBar(title: Text(item.name), actions: [
        IconButton(
          tooltip: fav ? l.removeFromFavourites : l.addToFavourites,
          icon: Icon(fav ? Icons.favorite : Icons.favorite_border,
              color: fav ? AppColors.red : null),
          onPressed: () => app.toggleFavourite(item.id).catchError((e) {
            if (context.mounted) {
              showMessage(context, errorMessage(e), error: true);
            }
          }),
        ),
      ]),
      body: ListView(padding: const EdgeInsets.all(16), children: [
        SizedBox(
            height: 220, child: ProductImage(url: item.imageUrl, radius: 16)),
        const SizedBox(height: 14),
        Row(children: [
          Expanded(
              child: Text(item.name,
                  style: const TextStyle(
                      fontSize: 22, fontWeight: FontWeight.w800))),
          Text(xaf(_size?.priceXaf ?? item.priceXaf),
              style: const TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.w800,
                  color: AppColors.amber)),
        ]),
        if (item.description != null && item.description!.isNotEmpty) ...[
          const SizedBox(height: 6),
          Text(item.description!)
        ],
        if (!item.available) ...[
          const SizedBox(height: 10),
          InfoNote(l.unavailableHere,
              icon: Icons.info_outline,
              color: AppColors.red,
              background: AppColors.redSoft)
        ],
        if (item.hasSizes) ...[
          SectionLabel(l.size),
          Wrap(spacing: 8, runSpacing: 8, children: [
            for (final s in item.sizes)
              ChoiceChip(
                  label: Text('${s.name} · ${xaf(s.priceXaf)}'),
                  selected: _size?.id == s.id,
                  selectedColor: AppColors.gold,
                  showCheckmark: false,
                  onSelected: (_) => setState(() => _size = s)),
          ]),
        ],
        if (item.addons.isNotEmpty) ...[
          SectionLabel(l.addOnsOptional),
          for (final a in item.addons)
            CheckboxListTile(
              contentPadding: EdgeInsets.zero,
              controlAffinity: ListTileControlAffinity.leading,
              value: _addons.contains(a.id),
              onChanged: (v) => setState(
                  () => v == true ? _addons.add(a.id) : _addons.remove(a.id)),
              title: Text(a.name),
              secondary: Text('+${xaf(a.priceXaf)}',
                  style: const TextStyle(fontWeight: FontWeight.w700)),
            ),
        ],
        const SizedBox(height: 80),
      ]),
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Row(children: [
            QuantityStepper(
                value: _quantity,
                onMinus:
                    _quantity > 1 ? () => setState(() => _quantity--) : null,
                onPlus: () => setState(() => _quantity++)),
            const SizedBox(width: 12),
            Expanded(
                child: FilledButton(
                    onPressed: item.available ? _add : null,
                    child: Text(l.addToCartPrice(xaf(_line.totalXaf))))),
          ]),
        ),
      ),
    );
  }
}
