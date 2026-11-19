import 'package:material_ui/material_ui.dart';

import 'package:lutiano_core/lutiano_core.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import '../requests/stock_requests_screen.dart';
import 'stock_detail_screen.dart';
import 'stock_widgets.dart';
import '../../core/l10n.dart';

/// Batch 2 · Screen 5 — Low Stock & Alerts.
class LowStockScreen extends StatelessWidget {
  const LowStockScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final op = app.operation!;
    return Scaffold(
      appBar: AppBar(title: Text(context.l10n.lowStockAlerts)),
      bottomNavigationBar: app.user?.canRequestStock == true
          ? SafeArea(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: FilledButton.icon(
                  onPressed: () => StockRequestsScreen.startRequest(context),
                  icon: const Icon(Icons.add),
                  label: Text(context.l10n.requestStock),
                ),
              ),
            )
          : null,
      body: PagedListView<StockItem>(
        key: ValueKey('low-${op.id}-${app.dataVersion}'),
        fetch: (page, size) => app.inventory.stockPage(op.id,
            status: 'attention', lowestFirst: true, page: page, size: size),
        header: (context, c) => c.total == 0
            ? const SizedBox.shrink()
            : Column(children: [
                const Icon(Icons.warning_amber_rounded,
                    color: AppColors.amber, size: 48),
                const SizedBox(height: 8),
                Text(context.l10n.productsRunningLow(c.total),
                    textAlign: TextAlign.center,
                    style: const TextStyle(
                        fontWeight: FontWeight.w800, fontSize: 18)),
                Text(context.l10n.askYourInventoryManagerFor,
                    textAlign: TextAlign.center,
                    style: const TextStyle(color: AppColors.muted)),
                const SizedBox(height: 16),
              ]),
        empty: EmptyState(
            icon: Icons.check_circle_outline,
            title: context.l10n.allStockLevelsAreHealthy),
        itemBuilder: (context, s, _, __) => StockTile(
            item: s,
            onTap: () => Navigator.of(context).push(
                MaterialPageRoute(builder: (_) => StockDetailScreen(item: s)))),
      ),
    );
  }
}
