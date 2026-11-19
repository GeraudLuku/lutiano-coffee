import 'package:material_ui/material_ui.dart';

import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import '../inventory/stock_list_screen.dart';
import 'operation_picker.dart';
import 'operation_qr_screen.dart';
import '../../core/l10n.dart';

/// Batch 1 · Screen 3 — My Sales Operation (also Batch 8 · Sales Operation Details).
class MyOperationScreen extends StatelessWidget {
  const MyOperationScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final op = app.operation;
    return Scaffold(
      appBar: AppBar(title: Text(context.l10n.mySalesOperation)),
      body: op == null
          ? EmptyState(
              icon: Icons.storefront_outlined,
              title: context.l10n.noSalesOperationAssigned)
          : AsyncView<OperationDetail>(
              key: ValueKey('op-${op.id}-${app.dataVersion}'),
              load: () => app.catalog.operation(op.id),
              builder: (context, d, _) =>
                  ListView(padding: const EdgeInsets.all(16), children: [
                OperationHeaderCard(operation: d.operation),
                const SizedBox(height: 12),
                AppCard(
                  child: Column(children: [
                    Row(children: [
                      Expanded(
                          child: MetricTile(
                              icon: Icons.today_outlined,
                              label: context.l10n.today,
                              value: xaf(d.stats.todayXaf))),
                      Expanded(
                          child: MetricTile(
                              icon: Icons.date_range_outlined,
                              label: context.l10n.last7Days,
                              value: xaf(d.stats.weekXaf),
                              iconColor: AppColors.blue,
                              iconBg: AppColors.blueSoft)),
                    ]),
                    const SizedBox(height: 12),
                    Row(children: [
                      Expanded(
                          child: MetricTile(
                              icon: Icons.calendar_month_outlined,
                              label: context.l10n.thisMonth,
                              value: xaf(d.stats.monthXaf),
                              iconColor: AppColors.purple,
                              iconBg: AppColors.purpleSoft)),
                      Expanded(
                          child: MetricTile(
                              icon: Icons.receipt_long_outlined,
                              label: context.l10n.transactions,
                              value: '${d.stats.transactions}',
                              iconColor: AppColors.amber,
                              iconBg: AppColors.amberSoft)),
                    ]),
                  ]),
                ),
                const SizedBox(height: 12),
                AppCard(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                  child: Column(children: [
                    MenuTile(
                        icon: Icons.qr_code_2,
                        title: context.l10n.myQrCode,
                        subtitle: context.l10n.showMyPermanentQrCode,
                        onTap: () => Navigator.of(context).push(
                            MaterialPageRoute(
                                builder: (_) => const OperationQrScreen()))),
                    MenuTile(
                        icon: Icons.inventory_2_outlined,
                        title: context.l10n.assignedProducts,
                        subtitle: context.l10n.productsAndStockICan,
                        onTap: () => Navigator.of(context).push(
                            MaterialPageRoute(
                                builder: (_) => const StockListScreen()))),
                    if (app.operations.length > 1)
                      MenuTile(
                          icon: Icons.swap_horiz,
                          title: context.l10n.switchOperation,
                          subtitle: context.l10n
                              .operationsAssigned(app.operations.length),
                          onTap: () => showOperationPicker(context)),
                  ]),
                ),
                SectionLabel(
                    context.l10n.assignedSellers(d.sellerNames.length)),
                AppCard(
                  child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        for (final name in d.sellerNames)
                          Padding(
                              padding: const EdgeInsets.symmetric(vertical: 4),
                              child: Row(children: [
                                const Icon(Icons.person_outline,
                                    color: AppColors.muted, size: 20),
                                const SizedBox(width: 10),
                                Text(name),
                              ])),
                      ]),
                ),
              ]),
            ),
    );
  }
}

class OperationHeaderCard extends StatelessWidget {
  const OperationHeaderCard({super.key, required this.operation});
  final Operation operation;

  @override
  Widget build(BuildContext context) => AppCard(
        child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Container(
            width: 64,
            height: 64,
            decoration: BoxDecoration(
                color: AppColors.goldSoft,
                borderRadius: BorderRadius.circular(12)),
            child:
                const Icon(Icons.storefront, color: AppColors.navy, size: 32),
          ),
          const SizedBox(width: 12),
          Expanded(
            child:
                Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(operation.name,
                  style: const TextStyle(
                      fontWeight: FontWeight.w800, fontSize: 16)),
              const SizedBox(height: 4),
              Wrap(spacing: 6, runSpacing: 4, children: [
                operation.active
                    ? activeChip(context)
                    : StatusChip(context.l10n.inactive,
                        color: AppColors.red, background: AppColors.redSoft),
                StatusChip(operation.type.label(context.l10n),
                    color: AppColors.purple, background: AppColors.purpleSoft),
              ]),
              const SizedBox(height: 6),
              Text(operation.code,
                  style: const TextStyle(color: AppColors.muted)),
              if (operation.location.isNotEmpty)
                Row(children: [
                  const Icon(Icons.place_outlined,
                      size: 16, color: AppColors.muted),
                  const SizedBox(width: 4),
                  Expanded(
                      child: Text(operation.location,
                          style: const TextStyle(color: AppColors.muted))),
                ]),
            ]),
          ),
        ]),
      );
}
