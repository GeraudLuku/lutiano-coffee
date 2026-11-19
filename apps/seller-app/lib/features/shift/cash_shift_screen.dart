import 'package:material_ui/material_ui.dart';

import 'package:lutiano_core/lutiano_core.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import 'cash_dialogs.dart';
import 'shift_dialogs.dart';
import 'shift_detail_screen.dart';
import 'shift_lists.dart';
import '../../core/l10n.dart';

/// S12 Cash shift: the open shift's reconciliation and ledger, outflow requests, remittances and their
/// acknowledgement status, and past shifts (with recounts when a manager asks for one).
class CashShiftScreen extends StatefulWidget {
  const CashShiftScreen({super.key});

  @override
  State<CashShiftScreen> createState() => _CashShiftScreenState();
}

class _CashShiftScreenState extends State<CashShiftScreen> {
  int _version = 0;

  /// The open shift, and the latest closed ones (the first page: a recount asked for is always among the recent shifts).
  Future<(CashShiftDetail?, List<CashShiftDetail>, CashOverview)>
      _load() async {
    final app = AppScope.read(context);
    await app.refreshShift();
    final current =
        app.shift == null ? null : await app.cash.shift(app.shift!.id);
    final history =
        (await app.cash.shiftsPage()).items.where((s) => !s.isOpen).toList();
    return (current, history, await app.cash.overview());
  }

  Future<void> _after(Future<bool> action, String message) async {
    if (await action && mounted) {
      showMessage(context, message);
      setState(() => _version++);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(title: Text(context.l10n.cashShift)),
        body:
            AsyncView<(CashShiftDetail?, List<CashShiftDetail>, CashOverview)>(
          key: ValueKey(_version),
          load: _load,
          builder: (context, data, reload) {
            final (current, history, overview) = data;
            final recounts = history.where((s) => s.recountRequested).toList();
            return ListView(padding: const EdgeInsets.all(16), children: [
              for (final s in recounts)
                Padding(
                  padding: const EdgeInsets.only(bottom: 12),
                  child: AppCard(
                    color: AppColors.amberSoft,
                    onTap: () => _after(showRecountDialog(context, s),
                        context.l10n.recountSubmitted),
                    child: Row(children: [
                      const Icon(Icons.calculate_outlined,
                          color: AppColors.amber),
                      const SizedBox(width: 12),
                      Expanded(
                          child: Text(
                              context.l10n.recountAsked(dayMonth(s.openedAt)),
                              style: const TextStyle(
                                  fontWeight: FontWeight.w700,
                                  color: AppColors.amber))),
                      const Icon(Icons.chevron_right, color: AppColors.amber),
                    ]),
                  ),
                ),
              if (!overview.isEmpty) ...[
                SectionLabel(context.l10n.needsAttention),
                _Attention(overview: overview),
              ],
              if (current != null && isBeforeToday(current.openedAt))
                Padding(
                  padding: const EdgeInsets.only(bottom: 12),
                  child: InfoNote(
                      context.l10n
                          .shiftOpenedEarlier(dayMonth(current.openedAt)),
                      icon: Icons.schedule,
                      color: AppColors.amber,
                      background: AppColors.amberSoft),
                ),
              if (current == null)
                EmptyState(
                  icon: Icons.lock_clock_outlined,
                  title: context.l10n.noOpenCashShift,
                  message: context.l10n.openAShiftWithYour,
                  action: FilledButton(
                      onPressed: () => _after(showOpenShiftDialog(context),
                          context.l10n.shiftOpened),
                      child: Text(context.l10n.openShift)),
                )
              else
                ..._openShift(current),
              SectionLabel(context.l10n.cash),
              AppCard(
                padding:
                    const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                child: Column(children: [
                  MenuTile(
                      icon: Icons.outbox_outlined,
                      title: context.l10n.allOutflowRequests,
                      onTap: () => Navigator.of(context).push(MaterialPageRoute(
                          builder: (_) => const OutflowRequestsScreen()))),
                  MenuTile(
                      icon: Icons.payments_outlined,
                      title: context.l10n.allRemittances,
                      onTap: () => Navigator.of(context).push(MaterialPageRoute(
                          builder: (_) => const RemittancesScreen()))),
                ]),
              ),
              if (history.isNotEmpty) ...[
                SectionLabel(context.l10n.previousShifts),
                for (final s in history.take(5)) ShiftHistoryTile(shift: s),
                MenuTile(
                    icon: Icons.history,
                    title: context.l10n.viewAllShifts,
                    onTap: () => Navigator.of(context).push(MaterialPageRoute(
                        builder: (_) => const ShiftHistoryScreen()))),
              ],
            ]);
          },
        ),
      );

  List<Widget> _openShift(CashShiftDetail s) => [
        HighlightBox(
            label: context.l10n.expectedInYourDrawer,
            value: xaf(s.expectedCash)),
        const SizedBox(height: 12),
        AppCard(
          child: Column(children: [
            InfoRow(context.l10n.opened, dateTime(s.openedAt)),
            InfoRow(context.l10n.openingFloat, xaf(s.openingFloat)),
            InfoRow(
                context.l10n.cashSalesCount(s.saleCount), xaf(s.cashSalesXaf)),
            if (s.refundsPaidXaf > 0)
              InfoRow(context.l10n.refundsPaid, xaf(s.refundsPaidXaf)),
            if (s.outflowsXaf > 0)
              InfoRow(context.l10n.approvedOutflows, xaf(s.outflowsXaf)),
            if (s.remittedXaf > 0)
              InfoRow(context.l10n.remitted, xaf(s.remittedXaf)),
            InfoRow(context.l10n.expectedCash2, xaf(s.expectedCash),
                bold: true),
          ]),
        ),
        const SizedBox(height: 12),
        Wrap(spacing: 8, runSpacing: 8, children: [
          OutlinedButton.icon(
              onPressed: () => _after(
                  showOutflowDialog(context), context.l10n.outflowRequested),
              icon: const Icon(Icons.outbox_outlined),
              label: Text(context.l10n.requestOutflow)),
          OutlinedButton.icon(
              onPressed: () => _after(
                  showRemittanceDialog(context, s.expectedCash),
                  context.l10n.remittanceRecorded),
              icon: const Icon(Icons.payments_outlined),
              label: Text(context.l10n.recordRemittance)),
          FilledButton.icon(
              onPressed: () async {
                await showCloseShiftDialog(context);
                if (mounted) setState(() => _version++);
              },
              icon: const Icon(Icons.lock_outline),
              label: Text(context.l10n.closeShift)),
        ]),
        if (s.outflows.isNotEmpty) ...[
          SectionLabel(context.l10n.outflowRequests),
          for (final o in s.outflows)
            OutflowTile(
              outflow: o,
              trailing: o.status == 'REQUESTED'
                  ? IconButton(
                      tooltip: context.l10n.cancelRequest,
                      icon: const Icon(Icons.close),
                      onPressed: () async {
                        await AppScope.read(context).cash.cancelOutflow(o.id);
                        if (mounted) setState(() => _version++);
                      })
                  : null,
            ),
        ],
        if (s.remittances.isNotEmpty) ...[
          SectionLabel(context.l10n.remittances),
          for (final r in s.remittances) RemittanceTile(remittance: r),
        ],
        SectionLabel(context.l10n.cashLedger),
        MenuTile(
            icon: Icons.receipt_long_outlined,
            title: context.l10n.viewCashLedger,
            onTap: () => Navigator.of(context).push(MaterialPageRoute(
                builder: (_) => CashLedgerScreen(shiftId: s.id)))),
      ];
}

/// What needs the seller's attention across all shifts; each line opens the list it is about.
class _Attention extends StatelessWidget {
  const _Attention({required this.overview});
  final CashOverview overview;

  @override
  Widget build(BuildContext context) {
    final o = overview;
    final l = context.l10n;
    Widget line(IconData icon, Color color, Color background, String text,
            Widget screen) =>
        Padding(
          padding: const EdgeInsets.only(bottom: 8),
          child: AppCard(
            color: background,
            onTap: () => Navigator.of(context)
                .push(MaterialPageRoute(builder: (_) => screen)),
            child: Row(children: [
              Icon(icon, color: color),
              const SizedBox(width: 12),
              Expanded(
                  child: Text(text,
                      style: TextStyle(
                          fontWeight: FontWeight.w700, color: color))),
              Icon(Icons.chevron_right, color: color),
            ]),
          ),
        );
    return Column(children: [
      if (o.disputedRemittances > 0)
        line(
            Icons.report_problem_outlined,
            AppColors.red,
            AppColors.redSoft,
            l.remittancesDisputed(o.disputedRemittances),
            const RemittancesScreen(initialStatus: 'DISPUTED')),
      if (o.outstandingRemittances > 0)
        line(
            Icons.hourglass_empty,
            AppColors.amber,
            AppColors.amberSoft,
            l.remittancesAwaiting(
                o.outstandingRemittances, xaf(o.outstandingRemittancesXaf)),
            const RemittancesScreen(initialStatus: 'OUTSTANDING')),
      if (o.pendingOutflows > 0)
        line(
            Icons.outbox_outlined,
            AppColors.amber,
            AppColors.amberSoft,
            l.outflowsAwaiting(o.pendingOutflows),
            const OutflowRequestsScreen()),
    ]);
  }
}

/// The pill for an outflow or remittance status, the same wherever one is listed.
StatusChip cashStatusChip(BuildContext context, String status) =>
    switch (status) {
      'REQUESTED' => StatusChip(context.l10n.awaitingApproval,
          color: AppColors.amber, background: AppColors.amberSoft),
      'APPROVED' => StatusChip(context.l10n.approved),
      'REJECTED' => StatusChip(context.l10n.rejected,
          color: AppColors.red, background: AppColors.redSoft),
      'CANCELLED' => StatusChip(context.l10n.cancelled,
          color: AppColors.muted, background: AppColors.background),
      'OUTSTANDING' => StatusChip(context.l10n.awaitingConfirmation,
          color: AppColors.amber, background: AppColors.amberSoft),
      'ACKNOWLEDGED' => StatusChip(context.l10n.confirmed),
      'DISPUTED' => StatusChip(context.l10n.disputed,
          color: AppColors.red, background: AppColors.redSoft),
      _ => StatusChip(status),
    };

/// One outflow request with its status (and, once decided, who decided and why).
class OutflowTile extends StatelessWidget {
  const OutflowTile({super.key, required this.outflow, this.trailing});
  final CashOutflow outflow;
  final Widget? trailing;

  @override
  Widget build(BuildContext context) {
    final o = outflow;
    final l = context.l10n;
    return CashRecordRow(
      title: '${outflowCategoryLabel(l, o.category)} · ${xaf(o.amountXaf)}',
      subtitle: '${o.reference} · ${o.description}',
      detail: o.decisionNote == null
          ? null
          : l.decidedByNote(o.decidedByName ?? '', o.decisionNote!),
      chip: cashStatusChip(context, o.status),
      trailing: trailing,
    );
  }
}

/// One remittance with its acknowledgement status (a disputed one shows what the recipient says they received).
class RemittanceTile extends StatelessWidget {
  const RemittanceTile({super.key, required this.remittance});
  final CashRemittance remittance;

  @override
  Widget build(BuildContext context) {
    final r = remittance;
    final l = context.l10n;
    return CashRecordRow(
      title: xaf(r.amountXaf),
      subtitle: '${r.reference} · ${r.destination(l)}',
      detail: r.status == 'DISPUTED' && r.receivedXaf != null
          ? [
              l.receivedAmount(xaf(r.receivedXaf!)),
              if (r.acknowledgementNote != null) r.acknowledgementNote!,
            ].join(' · ')
          : null,
      chip: cashStatusChip(context, r.status),
    );
  }
}

class CashRecordRow extends StatelessWidget {
  const CashRecordRow(
      {super.key,
      required this.title,
      required this.subtitle,
      required this.chip,
      this.detail,
      this.trailing,
      this.onTap});
  final String title, subtitle;
  final VoidCallback? onTap;
  final String? detail;
  final Widget chip;
  final Widget? trailing;

  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.only(bottom: 8),
        child: AppCard(
          padding: const EdgeInsets.fromLTRB(14, 10, 6, 10),
          onTap: onTap,
          child: Row(children: [
            Expanded(
              child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(title,
                        style: const TextStyle(fontWeight: FontWeight.w800)),
                    Text(subtitle,
                        style: const TextStyle(
                            color: AppColors.muted, fontSize: 12.5)),
                    if (detail != null)
                      Text(detail!,
                          style: const TextStyle(
                              color: AppColors.red,
                              fontSize: 12.5,
                              fontWeight: FontWeight.w700)),
                    const SizedBox(height: 6),
                    chip,
                  ]),
            ),
            if (trailing != null) trailing!,
            if (onTap != null)
              const Icon(Icons.chevron_right, color: AppColors.muted),
          ]),
        ),
      );
}

/// One past shift: its expected and counted cash, and where its review stands.
class ShiftHistoryTile extends StatelessWidget {
  const ShiftHistoryTile({super.key, required this.shift});
  final CashShiftDetail shift;

  @override
  Widget build(BuildContext context) {
    final v = shift.variance;
    return CashRecordRow(
      title: context.l10n
          .shiftExpected(dayMonthYear(shift.openedAt), xaf(shift.expectedCash)),
      subtitle: v == null
          ? context.l10n.notCounted
          : context.l10n.countedVariance(
              xaf(shift.countedCash!), '${v > 0 ? '+' : ''}${xaf(v)}'),
      chip: shiftReviewChip(context, shift.reviewStatus),
      onTap: () => Navigator.of(context).push(MaterialPageRoute(
          builder: (_) => ShiftDetailScreen(shiftId: shift.id))),
    );
  }
}

/// Where a closed shift's count stands with the manager.
StatusChip shiftReviewChip(BuildContext context, String? reviewStatus) =>
    switch (reviewStatus) {
      'PENDING_REVIEW' => StatusChip(context.l10n.managerReview,
          color: AppColors.amber, background: AppColors.amberSoft),
      'RECOUNT_REQUESTED' => StatusChip(context.l10n.recountRequested,
          color: AppColors.blue, background: AppColors.blueSoft),
      'RESOLVED' => StatusChip(context.l10n.reviewed),
      _ => StatusChip(context.l10n.balanced),
    };
