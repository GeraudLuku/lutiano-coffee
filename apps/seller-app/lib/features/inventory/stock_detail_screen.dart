import 'package:material_ui/material_ui.dart';

import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import 'stock_widgets.dart';
import '../../core/l10n.dart';

/// Batch 2 · Screens 3 & 7 — Product stock details, threshold and recent movements.
class StockDetailScreen extends StatelessWidget {
  const StockDetailScreen({super.key, required this.item});
  final StockItem item;

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final p = item.product;
    return Scaffold(
      appBar: AppBar(title: Text(context.l10n.productDetails)),
      body: PagedListView<StockMovement>(
        key: ValueKey('detail-${p.id}-${app.dataVersion}'),
        fetch: (page, size) => app.inventory.movementsPage(app.operation!.id,
            productId: p.id, page: page, size: size),
        skeleton: const SkeletonTile(),
        empty: EmptyState(
            icon: Icons.swap_vert, title: context.l10n.noMovementsYet),
        itemBuilder: (context, m, _, __) => MovementTile(movement: m),
        header: (context, c) =>
            Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          AppCard(
            child: Row(children: [
              ProductImage(url: p.imageUrl, size: 84),
              const SizedBox(width: 14),
              Expanded(
                  child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                    Text(p.name,
                        style: const TextStyle(
                            fontWeight: FontWeight.w800, fontSize: 18)),
                    Text(p.category,
                        style: const TextStyle(color: AppColors.muted)),
                    const SizedBox(height: 6),
                    stockChip(context, item.level),
                  ])),
            ]),
          ),
          const SizedBox(height: 12),
          _Figures(
              key: ValueKey('figures-${p.id}-${app.dataVersion}'), item: item),
          const SizedBox(height: 12),
          AppCard(
            child:
                Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              InfoRow(context.l10n.minimumLevel, '${p.lowStockThreshold}'),
              InfoRow(context.l10n.salesPrice, xaf(p.priceXaf)),
              InfoRow(context.l10n.sku, p.sku),
            ]),
          ),
          if (item.level != StockLevel.healthy) ...[
            const SizedBox(height: 12),
            InfoNote(context.l10n.stockAtOrBelow(p.lowStockThreshold),
                icon: Icons.warning_amber_rounded,
                color: AppColors.amber,
                background: AppColors.amberSoft),
          ],
          SectionLabel(context.l10n.recentMovements),
        ]),
      ),
    );
  }
}

/// Current stock, everything ever received here, and what was consumed today (from the movement summary).
class _Figures extends StatelessWidget {
  const _Figures({super.key, required this.item});
  final StockItem item;

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final op = app.operation!.id;
    final today = isoDateOf(DateTime.now());
    return AsyncView<(StockSummary, StockSummary)>(
      skeleton: const SkeletonScope(child: SkeletonMetrics()),
      load: () async => (
        await app.inventory.summary(op, productId: item.product.id),
        await app.inventory
            .summary(op, productId: item.product.id, from: today, to: today)
      ),
      builder: (context, data, _) {
        final (ever, day) = data;
        return Row(children: [
          Expanded(
              child: AppCard(
                  child: MetricTile(
                      icon: Icons.inventory_2_outlined,
                      label: context.l10n.currentStock,
                      value: '${item.quantity}',
                      iconColor: AppColors.amber,
                      iconBg: AppColors.amberSoft))),
          const SizedBox(width: 8),
          Expanded(
              child: AppCard(
                  child: MetricTile(
                      icon: Icons.south,
                      label: context.l10n.totalReceived,
                      value: '${ever.received}',
                      iconColor: AppColors.green,
                      iconBg: AppColors.greenSoft))),
          const SizedBox(width: 8),
          Expanded(
              child: AppCard(
                  child: MetricTile(
                      icon: Icons.north,
                      label: context.l10n.consumedToday,
                      value: '${day.consumed}',
                      iconColor: AppColors.red,
                      iconBg: AppColors.redSoft))),
        ]);
      },
    );
  }
}
