import 'package:material_ui/material_ui.dart';

import 'package:lutiano_core/lutiano_core.dart';
import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import 'payout_details_screen.dart';

/// The pill for a payout's status, the same on the list and the details.
StatusChip payoutStatusChip(BuildContext context, PayoutItem p) {
  final l = context.l10n;
  return switch (p.group) {
    PayoutGroup.completed => StatusChip(l.payoutCompleted),
    PayoutGroup.pending => StatusChip(l.payoutPending,
        color: AppColors.amber, background: AppColors.amberSoft),
    PayoutGroup.cancelled => StatusChip(l.payoutCancelled,
        color: AppColors.red, background: AppColors.redSoft),
  };
}

/// Batch 5 · Screen 5 — Payouts and payment history, filtered by what they are (completed, pending, cancelled).
class PayoutsScreen extends StatefulWidget {
  const PayoutsScreen({super.key});

  @override
  State<PayoutsScreen> createState() => _PayoutsScreenState();
}

class _PayoutsScreenState extends State<PayoutsScreen> {
  PayoutGroup? _group;

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final l = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l.payoutsTitle)),
      body: Column(children: [
        FilterChips<PayoutGroup>(
          options: [
            (PayoutGroup.completed, l.payoutCompleted),
            (PayoutGroup.pending, l.payoutPending),
            (PayoutGroup.cancelled, l.payoutCancelled),
          ],
          selected: _group,
          onSelected: (g) => setState(() => _group = g),
        ),
        Expanded(
          child: PagedListView<PayoutItem>(
            key: ValueKey('payouts-$_group-${app.dataVersion}'),
            fetch: (page, size) =>
                app.earnings.payoutsPage(group: _group, page: page, size: size),
            // the totals cover every payout, whatever the filter
            header: (context, c) => c.last == null
                ? const SkeletonScope(child: SkeletonMetrics())
                : _Totals(counts: PayoutCounts.fromJson(c.summary)),
            empty: EmptyState(
                icon: Icons.account_balance_outlined, title: l.noPayoutsYet),
            itemBuilder: (context, p, _, __) => Padding(
              padding: const EdgeInsets.only(bottom: 8),
              child: AppCard(
                onTap: () => Navigator.of(context).push(MaterialPageRoute(
                    builder: (_) => PayoutDetailsScreen(payoutId: p.id))),
                child: Row(children: [
                  const Icon(Icons.account_balance_outlined,
                      color: AppColors.navy),
                  const SizedBox(width: 12),
                  Expanded(
                      child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                        Text(p.reference,
                            style:
                                const TextStyle(fontWeight: FontWeight.w800)),
                        Text(dayMonthYear(p.paidAt ?? p.requestedAt),
                            style: const TextStyle(
                                color: AppColors.muted, fontSize: 12.5)),
                      ])),
                  Column(crossAxisAlignment: CrossAxisAlignment.end, children: [
                    Text(xaf(p.amountXaf),
                        style: const TextStyle(fontWeight: FontWeight.w800)),
                    payoutStatusChip(context, p),
                  ]),
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

class _Totals extends StatelessWidget {
  const _Totals({required this.counts});
  final PayoutCounts counts;

  @override
  Widget build(BuildContext context) {
    final l = context.l10n;
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Column(children: [
        AppCard(
          color: AppColors.navy,
          child: Row(children: [
            const Icon(Icons.account_balance_wallet_outlined,
                color: AppColors.gold, size: 30),
            const SizedBox(width: 12),
            Expanded(
                child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                  Text(l.totalPaidOut,
                      style: const TextStyle(color: AppColors.white70)),
                  Text(xaf(counts.paidOut),
                      style: const TextStyle(
                          color: Colors.white,
                          fontWeight: FontWeight.w800,
                          fontSize: 22)),
                ])),
          ]),
        ),
        const SizedBox(height: 10),
        AppCard(
          child: Row(children: [
            Expanded(
                child: MetricTile(
                    icon: Icons.check_circle_outline,
                    label: l.payoutCompleted,
                    value: '${counts.completed}')),
            Expanded(
                child: MetricTile(
                    icon: Icons.hourglass_empty,
                    label: l.payoutPending,
                    value: '${counts.pending}',
                    iconColor: AppColors.amber,
                    iconBg: AppColors.amberSoft)),
            Expanded(
                child: MetricTile(
                    icon: Icons.cancel_outlined,
                    label: l.payoutCancelled,
                    value: '${counts.cancelled}',
                    iconColor: AppColors.red,
                    iconBg: AppColors.redSoft)),
          ]),
        ),
      ]),
    );
  }
}
