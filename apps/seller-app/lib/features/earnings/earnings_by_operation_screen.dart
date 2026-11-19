import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';

/// Batch 5 · Screen 9 — Earnings by operation: sales and commission at each operation the seller sold at in the month.
class EarningsByOperationScreen extends StatefulWidget {
  const EarningsByOperationScreen({super.key, required this.month});
  final String month;

  @override
  State<EarningsByOperationScreen> createState() =>
      _EarningsByOperationScreenState();
}

class _EarningsByOperationScreenState extends State<EarningsByOperationScreen> {
  late String _month = widget.month;

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final l = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l.earningsByOperation)),
      body: Column(children: [
        MonthPicker(
            value: _month, onChanged: (m) => setState(() => _month = m)),
        Expanded(
          child: AsyncView<List<OperationEarnings>>(
            key: ValueKey('by-operation-$_month-${app.dataVersion}'),
            load: () => app.earnings.operations(month: _month),
            builder: (context, ops, _) => ops.isEmpty
                ? ListView(children: [
                    EmptyState(
                        icon: Icons.storefront_outlined,
                        title: l.noOperationEarnings)
                  ])
                : ListView(padding: const EdgeInsets.all(16), children: [
                    for (final o in ops)
                      Padding(
                        padding: const EdgeInsets.only(bottom: 12),
                        child: AppCard(
                          child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Row(children: [
                                  const Icon(Icons.storefront_outlined,
                                      color: AppColors.blue),
                                  const SizedBox(width: 10),
                                  Expanded(
                                      child: Text(o.name,
                                          style: const TextStyle(
                                              fontWeight: FontWeight.w800,
                                              fontSize: 16))),
                                ]),
                                const SizedBox(height: 8),
                                InfoRow(l.commissionsLabel, xaf(o.commission),
                                    bold: true),
                                InfoRow(l.sales, '${o.salesCount}'),
                                InfoRow(l.saleAmount, xaf(o.salesXaf)),
                                InfoRow(l.commissionRate,
                                    '${o.ratePercent.toStringAsFixed(1)}%'),
                              ]),
                        ),
                      ),
                  ]),
          ),
        ),
      ]),
    );
  }
}
