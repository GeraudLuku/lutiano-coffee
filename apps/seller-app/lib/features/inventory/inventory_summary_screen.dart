import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';

/// Batch 2 · Screen 9 — Inventory summary for a period: units received, consumed, net, and the top consumed products.
class InventorySummaryScreen extends StatefulWidget {
  const InventorySummaryScreen({super.key});

  @override
  State<InventorySummaryScreen> createState() => _InventorySummaryScreenState();
}

class _InventorySummaryScreenState extends State<InventorySummaryScreen> {
  PeriodChoice _period = const PeriodChoice(Period.month);

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final op = app.operation!;
    final (from, to) = _period.dates(DateTime.now());
    return Scaffold(
      appBar: AppBar(title: Text(context.l10n.inventorySummary)),
      body: Column(children: [
        PeriodBar(
            value: _period, onChanged: (p) => setState(() => _period = p)),
        Expanded(
          child: AsyncView<StockSummary>(
            key: ValueKey('summary-${op.id}-${app.dataVersion}-$from-$to'),
            load: () => app.inventory.summary(op.id, from: from, to: to),
            builder: (context, s, _) =>
                ListView(padding: const EdgeInsets.all(16), children: [
              _Figure(
                  icon: Icons.south,
                  color: AppColors.green,
                  background: AppColors.greenSoft,
                  value: '${s.received}',
                  label: context.l10n.stockInReceived),
              _Figure(
                  icon: Icons.north,
                  color: AppColors.red,
                  background: AppColors.redSoft,
                  value: '${s.consumed}',
                  label: context.l10n.stockOutConsumed),
              _Figure(
                  icon: Icons.functions,
                  color: AppColors.navy,
                  background: AppColors.goldSoft,
                  value: '${s.net > 0 ? '+' : ''}${s.net}',
                  label: context.l10n.netMovement),
              SectionLabel(context.l10n.topConsumedProducts),
              if (s.top.isEmpty)
                EmptyState(
                    icon: Icons.local_cafe_outlined,
                    title: context.l10n.nothingConsumedInPeriod)
              else
                AppCard(
                  child: Column(children: [
                    for (final t in s.top)
                      InfoRow(t.name, '${t.quantity}', bold: true),
                  ]),
                ),
            ]),
          ),
        ),
      ]),
    );
  }
}

class _Figure extends StatelessWidget {
  const _Figure(
      {required this.icon,
      required this.color,
      required this.background,
      required this.value,
      required this.label});
  final IconData icon;
  final Color color, background;
  final String value, label;

  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.only(bottom: 10),
        child: AppCard(
            child: MetricTile(
                icon: icon,
                label: label,
                value: value,
                iconColor: color,
                iconBg: background)),
      );
}
