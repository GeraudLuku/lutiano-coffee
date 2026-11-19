import 'package:material_ui/material_ui.dart';

import 'package:lutiano_core/lutiano_core.dart';
import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import 'cash_shift_screen.dart';

/// Every shift the seller has worked, newest first, loaded a page at a time.
/// Outflow requests from every shift, waiting ones first, filtered by status.
class OutflowRequestsScreen extends StatefulWidget {
  const OutflowRequestsScreen({super.key});

  @override
  State<OutflowRequestsScreen> createState() => _OutflowRequestsScreenState();
}

class _OutflowRequestsScreenState extends State<OutflowRequestsScreen> {
  String? _status;

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final l = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l.allOutflowRequests)),
      body: Column(children: [
        FilterChips<String>(
          options: [
            ('REQUESTED', l.awaitingApproval),
            ('APPROVED', l.approved),
            ('REJECTED', l.rejected),
            ('CANCELLED', l.cancelled),
          ],
          selected: _status,
          onSelected: (s) => setState(() => _status = s),
        ),
        Expanded(
          child: PagedListView<CashOutflow>(
            key: ValueKey('outflows-$_status-${app.dataVersion}'),
            fetch: (page, size) =>
                app.cash.outflowsPage(status: _status, page: page, size: size),
            empty:
                EmptyState(icon: Icons.outbox_outlined, title: l.noOutflowsYet),
            itemBuilder: (context, o, _, __) => OutflowTile(outflow: o),
          ),
        ),
      ]),
    );
  }
}

/// Remittances from every shift with their acknowledgement status, filtered by status.
class RemittancesScreen extends StatefulWidget {
  const RemittancesScreen({super.key, this.initialStatus});
  final String? initialStatus;

  @override
  State<RemittancesScreen> createState() => _RemittancesScreenState();
}

class _RemittancesScreenState extends State<RemittancesScreen> {
  late String? _status = widget.initialStatus;

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final l = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l.allRemittances)),
      body: Column(children: [
        FilterChips<String>(
          options: [
            ('OUTSTANDING', l.awaitingConfirmation),
            ('ACKNOWLEDGED', l.confirmed),
            ('DISPUTED', l.disputed),
          ],
          selected: _status,
          onSelected: (s) => setState(() => _status = s),
        ),
        Expanded(
          child: PagedListView<CashRemittance>(
            key: ValueKey('remittances-$_status-${app.dataVersion}'),
            fetch: (page, size) => app.cash
                .remittancesPage(status: _status, page: page, size: size),
            empty: EmptyState(
                icon: Icons.payments_outlined, title: l.noRemittancesYet),
            itemBuilder: (context, r, _, __) => RemittanceTile(remittance: r),
          ),
        ),
      ]),
    );
  }
}

class ShiftHistoryScreen extends StatelessWidget {
  const ShiftHistoryScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    return Scaffold(
      appBar: AppBar(title: Text(context.l10n.viewAllShifts)),
      body: PagedListView<CashShiftDetail>(
        key: ValueKey('shifts-${app.dataVersion}'),
        fetch: (page, size) => app.cash.shiftsPage(page: page, size: size),
        empty: EmptyState(
            icon: Icons.lock_clock_outlined, title: context.l10n.noShiftsYet),
        itemBuilder: (context, shift, _, __) => ShiftHistoryTile(shift: shift),
      ),
    );
  }
}

/// A shift's cash ledger, newest entry first, loaded a page at a time.
class CashLedgerScreen extends StatelessWidget {
  const CashLedgerScreen({super.key, required this.shiftId});
  final String shiftId;

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    return Scaffold(
      appBar: AppBar(title: Text(context.l10n.cashLedger)),
      body: PagedListView<CashLedgerEntry>(
        key: ValueKey('ledger-$shiftId-${app.dataVersion}'),
        fetch: (page, size) =>
            app.cash.ledgerPage(shiftId, page: page, size: size),
        skeleton: const SkeletonTile(),
        empty: EmptyState(
            icon: Icons.receipt_long_outlined,
            title: context.l10n.noLedgerEntries),
        itemBuilder: (context, e, _, __) => InfoRow(
            '${e.label(context.l10n)}${e.sourceReference == null ? '' : ' · ${e.sourceReference}'}\n${hhmm(e.createdAt)}',
            '${e.amountXaf > 0 ? '+' : ''}${xaf(e.amountXaf)}'),
      ),
    );
  }
}
