import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import 'package:lutiano_core/lutiano_core.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import 'request_summary_screen.dart';

/// Batch 3 · Screens 2 and 10 — choose the products and quantities, optionally starting from a previous request.
/// Pops the request that was created (or null).
class NewRequestScreen extends StatefulWidget {
  const NewRequestScreen({super.key, this.initial = const []});

  /// Products to start with (a duplicated request).
  final List<RequestLine> initial;

  @override
  State<NewRequestScreen> createState() => _NewRequestScreenState();
}

class _NewRequestScreenState extends State<NewRequestScreen> {
  static const _maxQuantity = 10000;
  late final Map<String, RequestLine> _selected = {
    for (final l in widget.initial) l.productId: l
  };
  String? _category;
  String _query = '';
  List<String> _categories = const [];

  int get _total => _selected.values.fold(0, (s, l) => s + l.quantity);

  void _set(StockItem item, int quantity) => setState(() {
        if (quantity <= 0) {
          _selected.remove(item.product.id);
        } else {
          _selected[item.product.id] = RequestLine(
              productId: item.product.id,
              name: item.product.name,
              imageUrl: item.product.imageUrl,
              quantity: quantity.clamp(1, _maxQuantity));
        }
      });

  Future<void> _edit(StockItem item, int current) async {
    final value = await showDialog<int>(
        context: context,
        builder: (context) => DialogFields(
            initial: [current > 0 ? '$current' : ''],
            builder: (context, f) => AlertDialog(
                  title: Text(item.product.name),
                  content: TextField(
                      controller: f[0],
                      autofocus: true,
                      keyboardType: TextInputType.number,
                      decoration: InputDecoration(
                          labelText: context.l10n.totalQuantity)),
                  actions: [
                    TextButton(
                        onPressed: () => Navigator.pop(context),
                        child: Text(context.l10n.cancel)),
                    TextButton(
                        onPressed: () => Navigator.pop(
                            context, int.tryParse(f[0].text) ?? 0),
                        child: Text(context.l10n.confirm)),
                  ],
                )));
    if (value != null) _set(item, value);
  }

  Future<void> _fromPrevious() async {
    final app = AppScope.read(context);
    final picked = await showModalBottomSheet<StockRequest>(
        context: context,
        isScrollControlled: true,
        builder: (context) =>
            _PreviousRequests(operationId: app.operation!.id));
    if (picked == null || !mounted) return;
    setState(() {
      _selected
        ..clear()
        ..addEntries([
          for (final i in picked.items)
            MapEntry(
                i.productId,
                RequestLine(
                    productId: i.productId,
                    name: i.name,
                    imageUrl: i.imageUrl,
                    quantity: i.quantity))
        ]);
    });
  }

  Future<void> _next() async {
    final created = await Navigator.of(context).push<StockRequest>(
        MaterialPageRoute(
            builder: (_) =>
                RequestSummaryScreen(lines: _selected.values.toList())));
    if (created != null && mounted) Navigator.pop(context, created);
  }

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final op = app.operation!;
    return Scaffold(
      appBar: AppBar(title: Text(context.l10n.newStockRequest)),
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
          child: TextButton.icon(
              onPressed: _fromPrevious,
              icon: const Icon(Icons.copy_all_outlined, size: 18),
              label: Text(context.l10n.startFromPrevious)),
        ),
        Expanded(
          child: PagedListView<StockItem>(
            key: ValueKey('request-products-${op.id}-$_category-$_query'),
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
            padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
            empty: EmptyState(
                icon: Icons.inventory_2_outlined,
                title: context.l10n.noProductsToRequest),
            itemBuilder: (context, item, _, __) {
              final quantity = _selected[item.product.id]?.quantity ?? 0;
              return Padding(
                padding: const EdgeInsets.only(bottom: 8),
                child: AppCard(
                  padding: const EdgeInsets.fromLTRB(6, 8, 10, 8),
                  child: Row(children: [
                    Checkbox(
                        value: quantity > 0,
                        onChanged: (v) => _set(item, v == true ? 1 : 0)),
                    ProductImage(url: item.product.imageUrl, size: 44),
                    const SizedBox(width: 10),
                    Expanded(
                        child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                          Text(item.product.name,
                              style:
                                  const TextStyle(fontWeight: FontWeight.w800)),
                          Text(context.l10n.stockNow(item.quantity),
                              style: const TextStyle(
                                  color: AppColors.muted, fontSize: 12.5)),
                        ])),
                    QuantityStepper(
                      value: quantity,
                      onMinus:
                          quantity > 0 ? () => _set(item, quantity - 1) : null,
                      onPlus: () => _set(item, quantity + 1),
                      onEdit: () => _edit(item, quantity),
                    ),
                  ]),
                ),
              );
            },
          ),
        ),
        _SelectionBar(
            products: _selected.length,
            quantity: _total,
            onNext: _selected.isEmpty ? null : _next),
      ]),
    );
  }
}

class _SelectionBar extends StatelessWidget {
  const _SelectionBar(
      {required this.products, required this.quantity, required this.onNext});
  final int products, quantity;
  final VoidCallback? onNext;

  @override
  Widget build(BuildContext context) => Material(
        elevation: 8,
        color: Colors.white,
        child: SafeArea(
          top: false,
          child: Padding(
            padding: const EdgeInsets.fromLTRB(16, 10, 16, 10),
            child: Row(children: [
              Expanded(
                  child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                    Text(context.l10n.productsSelected(products),
                        style: const TextStyle(fontWeight: FontWeight.w700)),
                    Text(context.l10n.totalQuantityValue(quantity),
                        style: const TextStyle(color: AppColors.muted)),
                  ])),
              // the theme's buttons fill the width; in a row this one keeps its own size
              FilledButton(
                  onPressed: onNext,
                  style:
                      FilledButton.styleFrom(minimumSize: const Size(110, 48)),
                  child: Text(context.l10n.next)),
            ]),
          ),
        ),
      );
}

/// The seller's recent requests, to start a new one from.
class _PreviousRequests extends StatelessWidget {
  const _PreviousRequests({required this.operationId});
  final String operationId;

  @override
  Widget build(BuildContext context) {
    final app = AppScope.read(context);
    return SizedBox(
      height: MediaQuery.of(context).size.height * 0.6,
      child: Column(children: [
        Padding(
            padding: const EdgeInsets.all(16),
            child: Text(context.l10n.previousRequests,
                style: const TextStyle(
                    fontWeight: FontWeight.w800, fontSize: 16))),
        Expanded(
          child: PagedListView<StockRequest>(
            fetch: (page, size) =>
                app.requests.minePage(operationId, page: page, size: size),
            empty: EmptyState(
                icon: Icons.inventory_2_outlined,
                title: context.l10n.noRequestsYet),
            itemBuilder: (context, r, _, __) => Padding(
              padding: const EdgeInsets.only(bottom: 8),
              child: AppCard(
                onTap: () => Navigator.pop(context, r),
                child: Row(children: [
                  Expanded(
                      child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                        Text(r.reference,
                            style:
                                const TextStyle(fontWeight: FontWeight.w800)),
                        Text(
                            r.items
                                .map((i) => '${i.quantity} × ${i.name}')
                                .join(', '),
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(
                                color: AppColors.muted, fontSize: 12.5)),
                      ])),
                  const Icon(Icons.chevron_right, color: AppColors.muted),
                ]),
              ),
            ),
          ),
        ),
      ]),
    );
  }
}
