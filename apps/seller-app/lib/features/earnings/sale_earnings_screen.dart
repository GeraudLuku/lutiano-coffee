import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import 'earning_detail_screen.dart';
import 'earnings_widgets.dart';

/// The sales that earned commission in a month, newest first, each with what it earned.
class SaleEarningsScreen extends StatefulWidget {
  const SaleEarningsScreen({super.key, required this.month});
  final String month;

  @override
  State<SaleEarningsScreen> createState() => _SaleEarningsScreenState();
}

class _SaleEarningsScreenState extends State<SaleEarningsScreen> {
  late String _month = widget.month;

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final l = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l.salesThatEarned)),
      body: Column(children: [
        MonthPicker(
            value: _month, onChanged: (m) => setState(() => _month = m)),
        Expanded(
          child: PagedListView<SaleEarning>(
            key: ValueKey('sale-earnings-$_month-${app.dataVersion}'),
            fetch: (page, size) =>
                app.earnings.salesPage(month: _month, page: page, size: size),
            empty: EmptyState(
                icon: Icons.receipt_long_outlined, title: l.noSalesEarnedYet),
            itemBuilder: (context, s, _, __) => Padding(
              padding: const EdgeInsets.only(bottom: 8),
              child: AppCard(
                onTap: () => Navigator.of(context).push(MaterialPageRoute(
                    builder: (_) => EarningDetailScreen(saleId: s.saleId))),
                child: Row(children: [
                  Expanded(
                      child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                        Text(s.reference,
                            style:
                                const TextStyle(fontWeight: FontWeight.w800)),
                        Text('${dateTime(s.createdAt)} · ${s.operationName}',
                            style: const TextStyle(
                                color: AppColors.muted, fontSize: 12.5)),
                      ])),
                  Text(signedXaf(s.net),
                      style: const TextStyle(
                          fontWeight: FontWeight.w800, color: AppColors.green)),
                  const Icon(Icons.chevron_right, color: AppColors.muted),
                ]),
              ),
            ),
          ),
        ),
      ]),
    );
  }
}
