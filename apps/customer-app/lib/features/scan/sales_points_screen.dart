import 'package:lutiano_core/lutiano_core.dart';
import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import 'sales_point_screen.dart';
import 'scan_flow.dart';

/// Every sales point, with search and a scan button. With [pick] it returns the chosen one to the screen that asked.
class SalesPointsScreen extends StatefulWidget {
  const SalesPointsScreen({super.key, this.pick = false});
  final bool pick;

  @override
  State<SalesPointsScreen> createState() => _SalesPointsScreenState();
}

class _SalesPointsScreenState extends State<SalesPointsScreen> {
  String _query = '';

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final l = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l.salesPoints), actions: [
        IconButton(
            tooltip: l.scanToOrder,
            icon: const Icon(Icons.qr_code_scanner),
            onPressed: () => scanSalesPoint(context)),
      ]),
      body: Column(children: [
        SearchFilterBar(
            hint: l.searchSalesPoints,
            categories: const [],
            category: null,
            onSearch: (q) => setState(() => _query = q),
            onCategory: (_) {}),
        Expanded(
          child: PagedListView<SalesPoint>(
            key: ValueKey('points-$_query'),
            fetch: (page, size) =>
                app.catalogue.salesPoints(q: _query, page: page, size: size),
            empty: EmptyState(
                icon: Icons.storefront_outlined,
                title: l.noSalesPoints,
                message: l.tryAnotherSearch),
            itemBuilder: (context, p, _, __) => Padding(
              padding: const EdgeInsets.only(bottom: 10),
              child: AppCard(
                onTap: () async {
                  final chosen = await Navigator.of(context).push<SalesPoint>(
                      MaterialPageRoute(
                          builder: (_) => SalesPointScreen(
                              salesPointId: p.id,
                              preloaded: null,
                              pick: widget.pick)));
                  if (widget.pick && chosen != null && context.mounted) {
                    Navigator.of(context).pop(chosen);
                  }
                },
                child: Row(children: [
                  ProductImage(url: p.imageUrl, size: 56),
                  const SizedBox(width: 12),
                  Expanded(
                      child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                        Text(p.name,
                            style:
                                const TextStyle(fontWeight: FontWeight.w800)),
                        Text(p.place,
                            style: const TextStyle(
                                color: AppColors.muted, fontSize: 12.5)),
                      ])),
                  p.openNow
                      ? StatusChip(l.openNow)
                      : StatusChip(l.closedNow,
                          color: AppColors.red, background: AppColors.redSoft),
                ]),
              ),
            ),
          ),
        ),
      ]),
    );
  }
}
