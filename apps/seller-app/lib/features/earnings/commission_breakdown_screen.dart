import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import 'earnings_widgets.dart';

/// Batch 5 · Screen 2 — Commission breakdown: by source (product sales, add-ons), bonuses and adjustments, and top products.
class CommissionBreakdownScreen extends StatefulWidget {
  const CommissionBreakdownScreen({super.key, required this.month});
  final String month;

  @override
  State<CommissionBreakdownScreen> createState() =>
      _CommissionBreakdownScreenState();
}

class _CommissionBreakdownScreenState extends State<CommissionBreakdownScreen> {
  late String _month = widget.month;

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final l = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l.commissionBreakdown)),
      body: Column(children: [
        MonthPicker(
            value: _month, onChanged: (m) => setState(() => _month = m)),
        Expanded(
          child: AsyncView<EarningsBreakdown>(
            key: ValueKey('breakdown-$_month-${app.dataVersion}'),
            load: () => app.earnings.breakdown(month: _month),
            builder: (context, b, _) =>
                ListView(padding: const EdgeInsets.all(16), children: [
              AppCard(
                color: AppColors.goldSoft,
                child: MetricTile(
                    icon: Icons.savings_outlined,
                    label: l.totalCommissions,
                    value: xaf(b.commission),
                    iconColor: AppColors.amber,
                    iconBg: Colors.white),
              ),
              SectionLabel(l.bySource),
              AppCard(
                child: Column(children: [
                  _Share(l.productSales, b.productCommission, b.commission),
                  _Share(l.addOnSales, b.addonCommission, b.commission),
                ]),
              ),
              SectionLabel(l.bonusesAndAdjustments),
              AppCard(
                child: Column(children: [
                  InfoRow(l.bonusesLabel, xaf(b.bonus)),
                  InfoRow(l.adjustmentsLabel, signedXaf(b.adjustments)),
                ]),
              ),
              SectionLabel(l.byProductTop),
              if (b.top.isEmpty)
                EmptyState(
                    icon: Icons.local_cafe_outlined,
                    title: l.noEarningsThisMonth)
              else
                AppCard(
                  child: Column(children: [
                    for (final t in b.top)
                      _Share(t.name, t.commission, b.commission),
                  ]),
                ),
            ]),
          ),
        ),
      ]),
    );
  }
}

class _Share extends StatelessWidget {
  const _Share(this.label, this.amount, this.whole);
  final String label;
  final int amount, whole;

  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 6),
        child: Row(children: [
          Expanded(
              child: Text(label,
                  style: const TextStyle(fontWeight: FontWeight.w600))),
          Text(xaf(amount),
              style: const TextStyle(fontWeight: FontWeight.w800)),
          SizedBox(
              width: 56,
              child: Text(sharePercent(amount, whole),
                  textAlign: TextAlign.right,
                  style: const TextStyle(color: AppColors.muted))),
        ]),
      );
}
