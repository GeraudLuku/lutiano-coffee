import 'package:material_ui/material_ui.dart';

import '../../data/models.dart';
import 'package:lutiano_core/lutiano_core.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import '../inventory/incoming_stock_screen.dart';
import '../orders/customer_orders_screen.dart';
import '../orders/scan_reward.dart';
import '../operation/my_operation_screen.dart';
import '../shift/shift_dialogs.dart';
import '../operation/operation_qr_screen.dart';
import '../sale/product_selection_screen.dart';
import '../shell/main_shell.dart';
import 'home_widgets.dart';
import '../../core/l10n.dart';

/// Batch 1 · Screen 2 — Role-based Home Dashboard.
class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final op = app.operation;
    return Scaffold(
      body: SafeArea(
        child: op == null
            ? const _NoOperation()
            : SummaryHost(
                key: ValueKey('summary-${op.id}-${app.dataVersion}'),
                child: AsyncView<_HomeData>(
                  key: ValueKey('home-${op.id}-${app.dataVersion}'),
                  skeleton: const _HomeSkeleton(),
                  load: () => _HomeData.load(context, op),
                  builder: (context, data, reload) =>
                      ListView(padding: const EdgeInsets.all(16), children: [
                    const HomeHeader(),
                    const SizedBox(height: 16),
                    const TodayPerformanceCard(),
                    const SizedBox(height: 12),
                    const ShiftCard(),
                    const SizedBox(height: 14),
                    FilledButton.icon(
                      onPressed: () => startNewSale(context),
                      icon: const Icon(Icons.add),
                      label: Text(context.l10n.newSale),
                    ),
                    SectionLabel(context.l10n.quickActions),
                    QuickActionGrid(actions: [
                      QuickAction(Icons.local_cafe_outlined,
                          context.l10n.products, () => startNewSale(context)),
                      QuickAction(
                          Icons.receipt_long_outlined,
                          context.l10n.salesHistory,
                          () => MainShell.goTo(context, MainShell.sales)),
                      QuickAction(
                          Icons.storefront_outlined,
                          context.l10n.myOperation,
                          () => _push(context, const MyOperationScreen())),
                      QuickAction(Icons.qr_code_2, context.l10n.myQrCode,
                          () => _push(context, const OperationQrScreen())),
                      QuickAction(
                          Icons.receipt_outlined,
                          context.l10n.customerOrders,
                          () => _push(context, const CustomerOrdersScreen()),
                          badge: data.orders),
                      QuickAction(Icons.card_giftcard, context.l10n.scanReward,
                          () => scanReward(context)),
                      QuickAction(
                          Icons.move_to_inbox_outlined,
                          context.l10n.receiveStock,
                          () => _openIncoming(context),
                          badge: data.incoming),
                      QuickAction(
                          Icons.inventory_2_outlined,
                          context.l10n.inventory,
                          () => MainShell.goTo(context, MainShell.inventory)),
                    ]),
                    const SizedBox(height: 4),
                    const SalesTrendCard(),
                    const SizedBox(height: 8),
                  ]),
                ),
              ),
      ),
    );
  }

  static void _push(BuildContext context, Widget screen) =>
      Navigator.of(context).push(MaterialPageRoute(builder: (_) => screen));
  static void _openIncoming(BuildContext context) =>
      _push(context, const IncomingStockScreen());
}

/// Requires an open shift, then opens product selection.
Future<void> startNewSale(BuildContext context) async {
  final app = AppScope.read(context);
  if (app.shift == null) {
    final opened = await showOpenShiftPrompt(context);
    if (!opened || !context.mounted) return;
  }
  await Navigator.of(context)
      .push(MaterialPageRoute(builder: (_) => const ProductSelectionScreen()));
}

/// What Home needs besides the seller's figures: how many deliveries wait, and a fresh look at the cash shift.
class _HomeData {
  _HomeData(this.incoming, this.orders);
  final int incoming;

  /// Customer orders waiting for the seller.
  final int orders;

  static Future<_HomeData> load(BuildContext context, Operation op) async {
    final app = AppScope.read(context);
    app.refreshUnread(); // the bell's badge; not awaited
    final results = await Future.wait([
      app.inventory.incomingPage(op.id, size: 1),
      app.refreshShift(),
      app.orders.counts().catchError((_) => const OrderCounts(0, 0, 0))
    ]);
    return _HomeData((results[0] as Paged<IncomingAllocation>).total,
        (results[2] as OrderCounts).open);
  }
}

/// Home while it loads: greeting, the day card, the shift card, the sale button and the quick actions.
class _HomeSkeleton extends StatelessWidget {
  const _HomeSkeleton();

  @override
  Widget build(BuildContext context) => SkeletonScope(
        child: ListView(
          physics: const NeverScrollableScrollPhysics(),
          padding: const EdgeInsets.all(16),
          children: [
            const SkeletonTile(trailing: false),
            const SizedBox(height: 16),
            const AppCard(
                child: Column(children: [
              SkeletonMetrics(count: 2),
              SizedBox(height: 14),
              SkeletonMetrics(count: 2)
            ])),
            const SizedBox(height: 12),
            const AppCard(child: SkeletonTile(trailing: false)),
            const SizedBox(height: 14),
            const SkeletonBox(width: double.infinity, height: 48, radius: 24),
            const SizedBox(height: 18),
            GridView.count(
              crossAxisCount: 3,
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              mainAxisSpacing: 10,
              crossAxisSpacing: 10,
              childAspectRatio: 1.15,
              children: [
                for (var i = 0; i < 6; i++)
                  const AppCard(
                      child: Center(
                          child:
                              SkeletonBox(width: 40, height: 40, radius: 12)))
              ],
            ),
          ],
        ),
      );
}

class _NoOperation extends StatelessWidget {
  const _NoOperation();

  @override
  Widget build(BuildContext context) =>
      ListView(padding: const EdgeInsets.all(16), children: [
        const HomeHeader(),
        const SizedBox(height: 24),
        EmptyState(
          icon: Icons.storefront_outlined,
          title: context.l10n.noSalesOperationAssigned,
          message: context.l10n.askYourManagerToAssign,
          action: OutlinedButton(
              onPressed: () => AppScope.read(context).refreshContext(),
              child: Text(context.l10n.refresh)),
        ),
      ]);
}

/// Shown when starting a sale without an open shift. Returns true once a shift is open.
Future<bool> showOpenShiftPrompt(BuildContext context) async {
  final go = await showDialog<bool>(
    context: context,
    builder: (ctx) => AlertDialog(
      title: Text(context.l10n.openYourCashShift),
      content: Text(context.l10n.cashSalesAreRecordedAgainst),
      actions: [
        TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: Text(context.l10n.notNow)),
        TextButton(
            onPressed: () => Navigator.pop(ctx, true),
            child: Text(context.l10n.openShift)),
      ],
    ),
  );
  if (go != true || !context.mounted) return false;
  return showOpenShiftDialog(context);
}
