import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import 'commission_breakdown_screen.dart';
import 'commission_rules_screen.dart';
import 'earnings_analytics_screen.dart';
import 'earnings_by_operation_screen.dart';
import 'earnings_by_period_screen.dart';
import 'earnings_widgets.dart';
import 'payouts_screen.dart';
import 'sale_earnings_screen.dart';
import '../targets/targets_screen.dart';
import 'statement_export_screen.dart';

/// Batch 5 · Screen 1 — Earnings overview for a month, with the way into every other earnings screen.
class EarningsScreen extends StatefulWidget {
  const EarningsScreen({super.key});

  @override
  State<EarningsScreen> createState() => _EarningsScreenState();
}

class _EarningsScreenState extends State<EarningsScreen> {
  String _month = currentMonth();

  void _open(Widget screen) =>
      Navigator.of(context).push(MaterialPageRoute(builder: (_) => screen));

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final l = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l.earnings), automaticallyImplyLeading: false),
      body: Column(children: [
        MonthPicker(
            value: _month, onChanged: (m) => setState(() => _month = m)),
        Expanded(
          child: AsyncView<EarningsMonth>(
            key: ValueKey('earnings-$_month-${app.dataVersion}'),
            load: () => app.earnings.overview(month: _month),
            builder: (context, e, _) =>
                ListView(padding: const EdgeInsets.all(16), children: [
              AppCard(
                color: AppColors.navy,
                child: Row(children: [
                  const Icon(Icons.savings_outlined,
                      color: AppColors.gold, size: 32),
                  const SizedBox(width: 12),
                  Expanded(
                      child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                        Text(l.myEarnings,
                            style: const TextStyle(
                                color: Colors.white,
                                fontWeight: FontWeight.w800,
                                fontSize: 16)),
                        Text(l.earningsSubtitle,
                            style: const TextStyle(color: AppColors.white70)),
                      ])),
                ]),
              ),
              const SizedBox(height: 12),
              AppCard(
                color: AppColors.greenSoft,
                child: Row(children: [
                  const Icon(Icons.check_circle,
                      color: AppColors.green, size: 34),
                  const SizedBox(width: 12),
                  Expanded(
                      child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                        Text(l.totalEarnings,
                            style: const TextStyle(
                                color: AppColors.muted, fontSize: 12.5)),
                        Text(xaf(e.total),
                            style: const TextStyle(
                                fontWeight: FontWeight.w800, fontSize: 24)),
                        ChangeLine(percent: e.changePercent),
                      ])),
                  earningsStatusChip(context, e.isFinal),
                ]),
              ),
              const SizedBox(height: 12),
              EarningsFigures(
                  commission: e.commission,
                  bonus: e.bonus,
                  adjustments: e.adjustments),
              if (!e.isFinal) ...[
                const SizedBox(height: 10),
                InfoNote(l.provisionalNote,
                    icon: Icons.schedule,
                    color: AppColors.amber,
                    background: AppColors.amberSoft),
              ],
              if (e.balance != null && e.balance!.owed > 0) ...[
                const SizedBox(height: 12),
                AppCard(
                  onTap: () => _open(const PayoutsScreen()),
                  child: MetricTile(
                      icon: Icons.account_balance_wallet_outlined,
                      label: l.stillToBePaid,
                      value: xaf(e.balance!.outstanding),
                      iconColor: AppColors.purple,
                      iconBg: AppColors.purpleSoft),
                ),
              ],
              const SizedBox(height: 16),
              FilledButton.icon(
                onPressed: () =>
                    _open(CommissionBreakdownScreen(month: _month)),
                icon: const Icon(Icons.chevron_right),
                label: Text(l.viewBreakdown),
              ),
              const SizedBox(height: 16),
              AppCard(
                padding:
                    const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                child: Column(children: [
                  MenuTile(
                      icon: Icons.emoji_events_outlined,
                      title: l.targetsAndBonuses,
                      subtitle: l.targetsSubtitle,
                      onTap: () => _open(const TargetsScreen())),
                  MenuTile(
                      icon: Icons.bar_chart,
                      title: l.earningsByPeriod,
                      onTap: () => _open(const EarningsByPeriodScreen())),
                  MenuTile(
                      icon: Icons.receipt_long_outlined,
                      title: l.salesThatEarned,
                      onTap: () => _open(SaleEarningsScreen(month: _month))),
                  MenuTile(
                      icon: Icons.account_balance_outlined,
                      title: l.payoutsTitle,
                      onTap: () => _open(const PayoutsScreen())),
                  MenuTile(
                      icon: Icons.storefront_outlined,
                      title: l.earningsByOperation,
                      onTap: () =>
                          _open(EarningsByOperationScreen(month: _month))),
                  MenuTile(
                      icon: Icons.show_chart,
                      title: l.trendsAndAnalytics,
                      onTap: () => _open(const EarningsAnalyticsScreen())),
                  MenuTile(
                      icon: Icons.rule,
                      title: l.commissionRulesTitle,
                      onTap: () => _open(const CommissionRulesScreen())),
                  MenuTile(
                      icon: Icons.picture_as_pdf_outlined,
                      title: l.earningsStatement,
                      onTap: () => _open(const StatementExportScreen())),
                ]),
              ),
            ]),
          ),
        ),
      ]),
    );
  }
}
