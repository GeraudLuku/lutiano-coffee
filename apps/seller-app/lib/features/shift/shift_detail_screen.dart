import 'package:material_ui/material_ui.dart';

import 'package:lutiano_core/lutiano_core.dart';
import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import 'cash_dialogs.dart';
import 'cash_shift_screen.dart';
import 'shift_lists.dart';

/// One shift's reconciliation: how the expected cash was reached, what was counted, the variance and its reason,
/// the manager's review, every count (closing and recounts), and the outflows and remittances of the shift.
class ShiftDetailScreen extends StatefulWidget {
  const ShiftDetailScreen({super.key, required this.shiftId});
  final String shiftId;

  @override
  State<ShiftDetailScreen> createState() => _ShiftDetailScreenState();
}

class _ShiftDetailScreenState extends State<ShiftDetailScreen> {
  int _version = 0;

  Future<void> _recount(CashShiftDetail shift) async {
    if (!await showRecountDialog(context, shift) || !mounted) return;
    showMessage(context, context.l10n.recountSubmitted);
    AppScope.read(context).dataChanged();
    setState(() => _version++);
  }

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    return Scaffold(
      appBar: AppBar(title: Text(context.l10n.shiftDetails)),
      body: AsyncView<CashShiftDetail>(
        key: ValueKey('shift-${widget.shiftId}-$_version-${app.dataVersion}'),
        load: () => app.cash.shift(widget.shiftId),
        builder: (context, s, _) {
          final l = context.l10n;
          final v = s.variance;
          return ListView(padding: const EdgeInsets.all(16), children: [
            AppCard(
              child: Row(children: [
                const Icon(Icons.lock_clock_outlined, color: AppColors.gold),
                const SizedBox(width: 12),
                Expanded(
                    child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                      Text(dayMonthYear(s.openedAt),
                          style: const TextStyle(
                              fontWeight: FontWeight.w800, fontSize: 16)),
                      Text(
                          s.closedAt == null
                              ? l.shiftOpenSince(hhmm(s.openedAt))
                              : '${hhmm(s.openedAt)} – ${hhmm(s.closedAt!)}',
                          style: const TextStyle(
                              color: AppColors.muted, fontSize: 12.5)),
                    ])),
                if (!s.isOpen) shiftReviewChip(context, s.reviewStatus),
              ]),
            ),
            SectionLabel(l.reconciliation),
            AppCard(
              child: Column(children: [
                InfoRow(l.openingFloat, xaf(s.openingFloat)),
                InfoRow(l.cashSalesCount(s.saleCount), xaf(s.cashSalesXaf)),
                if (s.refundsPaidXaf > 0)
                  InfoRow(l.refundsPaid, xaf(s.refundsPaidXaf)),
                if (s.outflowsXaf > 0)
                  InfoRow(l.approvedOutflows, xaf(s.outflowsXaf)),
                if (s.remittedXaf > 0) InfoRow(l.remitted, xaf(s.remittedXaf)),
                const Divider(),
                InfoRow(l.expectedCash2, xaf(s.expectedCash), bold: true),
                if (s.countedCash != null) ...[
                  InfoRow(l.countedCash, xaf(s.countedCash!), bold: true),
                  InfoRow(l.variance,
                      v == 0 ? xaf(0) : '${v! > 0 ? '+' : ''}${xaf(v)}',
                      bold: true),
                ],
              ]),
            ),
            if (s.varianceReason != null) ...[
              const SizedBox(height: 10),
              InfoNote(l.requestReasonValue(s.varianceReason!),
                  icon: Icons.notes,
                  color: AppColors.amber,
                  background: AppColors.amberSoft),
            ],
            if (!s.isOpen) _Review(shift: s),
            if (s.recountRequested) ...[
              const SizedBox(height: 12),
              FilledButton.icon(
                onPressed: () => _recount(s),
                icon: const Icon(Icons.calculate_outlined),
                label: Text(l.startRecount),
              ),
            ],
            if (s.counts.isNotEmpty) ...[
              SectionLabel(l.countHistory),
              AppCard(
                child: Column(children: [
                  for (final c in s.counts) _CountRow(count: c),
                ]),
              ),
            ],
            if (s.outflows.isNotEmpty || s.remittances.isNotEmpty) ...[
              SectionLabel(l.cashMovements),
              for (final o in s.outflows) OutflowTile(outflow: o),
              for (final r in s.remittances) RemittanceTile(remittance: r),
            ],
            const SizedBox(height: 4),
            MenuTile(
                icon: Icons.receipt_long_outlined,
                title: l.viewCashLedger,
                onTap: () => Navigator.of(context).push(MaterialPageRoute(
                    builder: (_) => CashLedgerScreen(shiftId: s.id)))),
          ]);
        },
      ),
    );
  }
}

/// The manager's decision on a variance, when there was one.
class _Review extends StatelessWidget {
  const _Review({required this.shift});
  final CashShiftDetail shift;

  @override
  Widget build(BuildContext context) {
    final s = shift;
    final l = context.l10n;
    if (s.reviewStatus == null || s.reviewStatus == 'NOT_REQUIRED') {
      return Padding(
          padding: const EdgeInsets.only(top: 10),
          child: InfoNote(l.shiftNotReviewed,
              icon: Icons.check_circle_outline,
              color: AppColors.green,
              background: AppColors.greenSoft));
    }
    final lines = [
      if (s.reviewedByName != null && s.reviewedAt != null)
        l.reviewedByWhen(s.reviewedByName!, dateTime(s.reviewedAt!)),
      if (s.reviewNote != null) s.reviewNote!,
    ];
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      SectionLabel(l.managerReviewTitle),
      AppCard(
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          shiftReviewChip(context, s.reviewStatus),
          if (lines.isNotEmpty) ...[
            const SizedBox(height: 8),
            Text(lines.join('\n')),
          ],
        ]),
      ),
    ]);
  }
}

class _CountRow extends StatelessWidget {
  const _CountRow({required this.count});
  final CashCount count;

  @override
  Widget build(BuildContext context) {
    final c = count;
    final l = context.l10n;
    final v = c.variance;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Expanded(
            child:
                Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text(c.isRecount ? l.recountLabel : l.closingCount,
              style: const TextStyle(fontWeight: FontWeight.w800)),
          Text(l.expectedCountedValue(xaf(c.expectedXaf), xaf(c.countedXaf)),
              style: const TextStyle(color: AppColors.muted, fontSize: 12.5)),
          Text(l.countedByWhen(c.countedByName ?? '', dateTime(c.createdAt)),
              style: const TextStyle(color: AppColors.muted, fontSize: 12.5)),
          if (c.reason != null)
            Text(c.reason!, style: const TextStyle(fontSize: 12.5)),
        ])),
        Text(v == 0 ? xaf(0) : '${v > 0 ? '+' : ''}${xaf(v)}',
            style: TextStyle(
                fontWeight: FontWeight.w800,
                color: v == 0 ? AppColors.green : AppColors.red)),
      ]),
    );
  }
}
