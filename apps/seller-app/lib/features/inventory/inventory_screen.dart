import 'package:material_ui/material_ui.dart';

import 'package:lutiano_core/lutiano_core.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import 'incoming_stock_screen.dart';
import 'inventory_summary_screen.dart';
import 'low_stock_screen.dart';
import 'stock_list_screen.dart';
import 'stock_movements_screen.dart';
import '../requests/stock_requests_screen.dart';
import '../../core/l10n.dart';

/// Batch 2 · Screen 1 — Inventory Overview for the seller's current operation.
class InventoryScreen extends StatelessWidget {
  const InventoryScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final op = app.operation;
    return Scaffold(
      appBar: AppBar(
          title: Text(context.l10n.inventory),
          automaticallyImplyLeading: false),
      body: op == null
          ? EmptyState(
              icon: Icons.inventory_2_outlined,
              title: context.l10n.noSalesOperationAssigned)
          : AsyncView<(Paged<StockItem>, Paged<IncomingAllocation>)>(
              key: ValueKey('inventory-${op.id}-${app.dataVersion}'),
              skeleton: const _InventorySkeleton(),
              // one row of each is enough: the totals come with the page
              load: () async => (
                await app.inventory.stockPage(op.id, size: 1),
                await app.inventory.incomingPage(op.id, size: 1)
              ),
              builder: (context, data, _) {
                final (stock, deliveries) = data;
                final units = asInt(stock.summary['units']);
                final value = asInt(stock.summary['value_xaf']);
                final low = asInt(stock.summary['attention']);
                final incoming = deliveries.total;
                return ListView(padding: const EdgeInsets.all(16), children: [
                  AppCard(
                    color: AppColors.navy,
                    child: Row(children: [
                      const Icon(Icons.inventory_2,
                          color: AppColors.gold, size: 32),
                      const SizedBox(width: 12),
                      Expanded(
                          child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                            Text(context.l10n.myInventory,
                                style: TextStyle(
                                    color: Colors.white,
                                    fontWeight: FontWeight.w800,
                                    fontSize: 16)),
                            Text(op.name,
                                style:
                                    const TextStyle(color: AppColors.white70)),
                          ])),
                    ]),
                  ),
                  const SizedBox(height: 12),
                  Row(children: [
                    Expanded(
                        child: AppCard(
                            child: MetricTile(
                                icon: Icons.inventory_2_outlined,
                                label: context.l10n.unitsInStock,
                                value: '$units',
                                iconColor: AppColors.blue,
                                iconBg: AppColors.blueSoft))),
                    const SizedBox(width: 10),
                    Expanded(
                        child: AppCard(
                      onTap: () => _push(context, const LowStockScreen()),
                      child: MetricTile(
                          icon: Icons.warning_amber_rounded,
                          label: context.l10n.lowStock,
                          value: '$low',
                          iconColor: AppColors.red,
                          iconBg: AppColors.redSoft),
                    )),
                  ]),
                  const SizedBox(height: 10),
                  AppCard(
                      child: MetricTile(
                          icon: Icons.payments_outlined,
                          label: context.l10n.stockValueSellingPrice,
                          value: xaf(value),
                          iconColor: AppColors.amber,
                          iconBg: AppColors.amberSoft)),
                  if (incoming > 0) ...[
                    const SizedBox(height: 12),
                    AppCard(
                      color: AppColors.goldSoft,
                      onTap: () => _push(context, const IncomingStockScreen()),
                      child: Row(children: [
                        const Icon(Icons.local_shipping_outlined,
                            color: AppColors.navy),
                        const SizedBox(width: 12),
                        Expanded(
                            child: Text(
                                context.l10n.deliveriesToConfirm(incoming),
                                style: const TextStyle(
                                    fontWeight: FontWeight.w800))),
                        const Icon(Icons.chevron_right),
                      ]),
                    ),
                  ],
                  const SizedBox(height: 12),
                  AppCard(
                    padding:
                        const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    child: Column(children: [
                      MenuTile(
                          icon: Icons.list_alt,
                          title: context.l10n.viewInventory,
                          subtitle: context.l10n.allProductsAtThisOperation,
                          onTap: () => _push(context, const StockListScreen())),
                      MenuTile(
                          icon: Icons.swap_vert,
                          title: context.l10n.stockMovements,
                          subtitle: context.l10n.receivedStockAndSales,
                          onTap: () =>
                              _push(context, const StockMovementsScreen())),
                      MenuTile(
                          icon: Icons.inventory_outlined,
                          title: context.l10n.stockRequests,
                          subtitle: context.l10n.stockRequestsSubtitle,
                          onTap: () =>
                              _push(context, const StockRequestsScreen())),
                      MenuTile(
                          icon: Icons.bar_chart,
                          title: context.l10n.inventorySummary,
                          subtitle: context.l10n.receivedConsumedAndTop,
                          onTap: () =>
                              _push(context, const InventorySummaryScreen())),
                      MenuTile(
                          icon: Icons.warning_amber_rounded,
                          title: context.l10n.lowStockAlerts,
                          subtitle: context.l10n.productsNeedAttention(low),
                          onTap: () => _push(context, const LowStockScreen())),
                      MenuTile(
                          icon: Icons.move_to_inbox_outlined,
                          title: context.l10n.receiveStock,
                          subtitle:
                              context.l10n.confirmDeliveriesFromTheCentral,
                          onTap: () =>
                              _push(context, const IncomingStockScreen())),
                    ]),
                  ),
                ]);
              },
            ),
    );
  }

  static void _push(BuildContext context, Widget screen) =>
      Navigator.of(context).push(MaterialPageRoute(builder: (_) => screen));
}

/// The overview while it loads: header card, two figures, one more figure.
class _InventorySkeleton extends StatelessWidget {
  const _InventorySkeleton();

  @override
  Widget build(BuildContext context) => const SkeletonScope(
        child: Padding(
          padding: EdgeInsets.all(16),
          child: Column(children: [
            AppCard(child: SkeletonTile(trailing: false)),
            SizedBox(height: 12),
            Row(children: [
              Expanded(child: AppCard(child: SkeletonMetrics(count: 1))),
              SizedBox(width: 10),
              Expanded(child: AppCard(child: SkeletonMetrics(count: 1))),
            ]),
            SizedBox(height: 10),
            AppCard(child: SkeletonMetrics(count: 1)),
            SizedBox(height: 12),
            AppCard(
                child: Column(children: [
              SkeletonTile(),
              SkeletonTile(),
              SkeletonTile()
            ])),
          ]),
        ),
      );
}
