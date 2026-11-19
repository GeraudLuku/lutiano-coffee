import 'package:material_ui/material_ui.dart';

import 'package:lutiano_core/lutiano_core.dart';
import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import 'earnings_widgets.dart';

/// Batch 5 · Screen 4 — Earnings details for one sale: the sale, and the commission each line earned under which rule.
class EarningDetailScreen extends StatelessWidget {
  const EarningDetailScreen({super.key, required this.saleId});
  final String saleId;

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final l = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l.earningDetails)),
      body: AsyncView<SaleEarningDetail>(
        key: ValueKey('earning-$saleId-${app.dataVersion}'),
        load: () => app.earnings.sale(saleId),
        builder: (context, d, _) {
          final s = d.sale;
          return ListView(padding: const EdgeInsets.all(16), children: [
            AppCard(
              child: Row(children: [
                const Icon(Icons.receipt_long_outlined, color: AppColors.gold),
                const SizedBox(width: 12),
                Expanded(
                    child: Text(s.reference,
                        style: const TextStyle(
                            fontWeight: FontWeight.w800, fontSize: 16))),
                StatusChip(
                    s.reversed > 0 ? l.refundedCommission : l.statusCompleted,
                    color: s.reversed > 0 ? AppColors.amber : AppColors.green,
                    background: s.reversed > 0
                        ? AppColors.amberSoft
                        : AppColors.greenSoft),
              ]),
            ),
            SectionLabel(l.saleInformation),
            AppCard(
              child: Column(children: [
                InfoRow(l.dateAndTime, dateTime(s.createdAt)),
                InfoRow(l.salesOperation, s.operationName),
                InfoRow(l.saleAmount, xaf(s.totalXaf)),
              ]),
            ),
            SectionLabel(l.myEarnings),
            AppCard(
              child: Column(children: [
                for (final line in d.lines)
                  InfoRow(
                      '${line.productName} × ${line.quantity}\n${ruleText(context, line.basis, line.rate)}',
                      xaf(line.commission)),
                if (s.reversed > 0)
                  InfoRow(l.refundedCommission, '-${xaf(s.reversed)}'),
                const Divider(),
                InfoRow(l.netCommission, xaf(s.net), bold: true),
              ]),
            ),
          ]);
        },
      ),
    );
  }
}
