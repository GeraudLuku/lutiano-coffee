import 'package:material_ui/material_ui.dart';

import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import 'stock_widgets.dart';
import '../../core/l10n.dart';

enum _Filter { all, stockIn, stockOut }

/// Batch 2 · Screen 4 — Stock Movements (received allocations in, sales out).
class StockMovementsScreen extends StatefulWidget {
  const StockMovementsScreen({super.key});

  @override
  State<StockMovementsScreen> createState() => _StockMovementsScreenState();
}

class _StockMovementsScreenState extends State<StockMovementsScreen> {
  _Filter _filter = _Filter.all;
  PeriodChoice _period = const PeriodChoice(Period.month);

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final op = app.operation!;
    final (from, to) = _period.dates(DateTime.now());
    return Scaffold(
      appBar: AppBar(title: Text(context.l10n.stockMovements)),
      body: Column(children: [
        PeriodBar(
            value: _period, onChanged: (p) => setState(() => _period = p)),
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
          child: SegmentedButton<_Filter>(
            showSelectedIcon: false,
            segments: [
              ButtonSegment(value: _Filter.all, label: Text(context.l10n.all)),
              ButtonSegment(
                  value: _Filter.stockIn, label: Text(context.l10n.inReceived)),
              ButtonSegment(
                  value: _Filter.stockOut, label: Text(context.l10n.outSold)),
            ],
            selected: {_filter},
            onSelectionChanged: (s) => setState(() => _filter = s.first),
          ),
        ),
        Expanded(
          child: PagedListView<StockMovement>(
            key: ValueKey(
                'moves-${op.id}-${app.dataVersion}-$_filter-$from-$to'),
            fetch: (page, size) => app.inventory.movementsPage(op.id,
                direction: switch (_filter) {
                  _Filter.stockIn => 'IN',
                  _Filter.stockOut => 'OUT',
                  _Filter.all => null
                },
                from: from,
                to: to,
                page: page,
                size: size),
            padding: const EdgeInsets.symmetric(horizontal: 16),
            skeleton: const SkeletonTile(),
            empty: EmptyState(
                icon: Icons.swap_vert, title: context.l10n.noMovementsToShow),
            itemBuilder: (context, m, _, __) => MovementTile(movement: m),
          ),
        ),
      ]),
    );
  }
}
