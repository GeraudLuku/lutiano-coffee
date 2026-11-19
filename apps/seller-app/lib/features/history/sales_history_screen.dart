import 'package:material_ui/material_ui.dart';

import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import 'sale_detail_screen.dart';
import '../../core/l10n.dart';

/// Batch 1 · Screen 10 — Sales History (the seller's own completed sales).
class SalesHistoryScreen extends StatefulWidget {
  const SalesHistoryScreen({super.key});

  @override
  State<SalesHistoryScreen> createState() => _SalesHistoryScreenState();
}

class _SalesHistoryScreenState extends State<SalesHistoryScreen> {
  PeriodChoice _period = const PeriodChoice(Period.week);

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final now = DateTime.now();
    final (from, to) = _period.dates(now);
    return Scaffold(
      appBar: AppBar(
          title: Text(context.l10n.salesHistory),
          automaticallyImplyLeading: false),
      body: Column(children: [
        PeriodBar(
            value: _period, onChanged: (p) => setState(() => _period = p)),
        Expanded(
          child: PagedListView<Sale>(
            key: ValueKey('history-$from-$to-${app.dataVersion}'),
            fetch: (page, size) =>
                app.sales.salesPage(from: from, to: to, page: page, size: size),
            // the totals cover the whole period, not only the pages loaded so far
            header: (context, c) => _Totals(controller: c),
            empty: EmptyState(
                icon: Icons.receipt_long_outlined,
                title: context.l10n.noSalesInThisPeriod,
                message: context.l10n.completedSalesWillAppearHere),
            itemBuilder: (context, sale, index, sales) {
              final day = dayMonthYear(sale.createdAt);
              final newDay =
                  index == 0 || dayMonthYear(sales[index - 1].createdAt) != day;
              return Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    if (newDay) SectionLabel(day),
                    Padding(
                        padding: const EdgeInsets.only(bottom: 8),
                        child: SaleTile(sale: sale)),
                  ]);
            },
          ),
        ),
      ]),
    );
  }
}

class _Totals extends StatelessWidget {
  const _Totals({required this.controller});
  final PagedController<Sale> controller;

  @override
  Widget build(BuildContext context) {
    final s = controller.summary;
    final loaded = controller.last != null;
    return AppCard(
      child: loaded
          ? Row(children: [
              Expanded(
                  child: MetricTile(
                      icon: Icons.receipt_long_outlined,
                      label: context.l10n.sales,
                      value: '${asInt(s['sales'])}')),
              Expanded(
                  child: MetricTile(
                      icon: Icons.payments_outlined,
                      label: context.l10n.xaf,
                      value: groupDigits(asInt(s['revenue_xaf'])),
                      iconColor: AppColors.purple,
                      iconBg: AppColors.purpleSoft)),
              Expanded(
                  child: MetricTile(
                      icon: Icons.local_cafe_outlined,
                      label: context.l10n.items,
                      value: '${asInt(s['units'])}',
                      iconColor: AppColors.amber,
                      iconBg: AppColors.amberSoft)),
            ])
          : const SkeletonScope(child: SkeletonMetrics()),
    );
  }
}

class SaleTile extends StatelessWidget {
  const SaleTile({super.key, required this.sale});
  final Sale sale;

  @override
  Widget build(BuildContext context) => AppCard(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
        onTap: () => Navigator.of(context).push(MaterialPageRoute(
            builder: (_) => SaleDetailScreen(saleId: sale.id))),
        child: Row(children: [
          Container(
            padding: const EdgeInsets.all(9),
            decoration: BoxDecoration(
                color: AppColors.goldSoft,
                borderRadius: BorderRadius.circular(10)),
            child: const Icon(Icons.receipt_outlined, color: AppColors.navy),
          ),
          const SizedBox(width: 12),
          Expanded(
            child:
                Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(sale.reference,
                  style: const TextStyle(fontWeight: FontWeight.w800)),
              Text(
                  context.l10n
                      .saleSubtitle(hhmm(sale.createdAt), sale.operationName),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style:
                      const TextStyle(color: AppColors.muted, fontSize: 12.5)),
            ]),
          ),
          Column(crossAxisAlignment: CrossAxisAlignment.end, children: [
            Text(xaf(sale.totalXaf),
                style: const TextStyle(fontWeight: FontWeight.w800)),
            const SizedBox(height: 4),
            completedChip(context),
          ]),
          const Icon(Icons.chevron_right, color: AppColors.muted),
        ]),
      );
}
