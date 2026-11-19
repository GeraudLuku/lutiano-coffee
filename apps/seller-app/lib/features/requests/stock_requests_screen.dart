import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import 'package:lutiano_core/lutiano_core.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import 'my_requests_screen.dart';
import 'new_request_screen.dart';
import 'request_details_screen.dart';

/// Batch 3 · Screen 1 — Stock Requests overview: counts per status, the Request Stock button, and the list entries.
class StockRequestsScreen extends StatelessWidget {
  const StockRequestsScreen({super.key});

  static Future<void> _push(BuildContext context, Widget screen) =>
      Navigator.of(context).push(MaterialPageRoute(builder: (_) => screen));

  /// Opens the new-request flow and, when a request was sent, its details.
  static Future<void> startRequest(BuildContext context) async {
    final navigator = Navigator.of(context);
    final app = AppScope.read(context);
    final created = await navigator.push<StockRequest>(
        MaterialPageRoute(builder: (_) => const NewRequestScreen()));
    if (created == null) return;
    app.dataChanged();
    await navigator.push(MaterialPageRoute(
        builder: (_) => RequestDetailsScreen(requestId: created.id)));
  }

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final op = app.operation;
    return Scaffold(
      appBar: AppBar(title: Text(context.l10n.stockRequests)),
      body: op == null
          ? EmptyState(
              icon: Icons.inventory_2_outlined,
              title: context.l10n.noSalesOperationAssigned)
          : AsyncView<Paged<StockRequest>>(
              key: ValueKey('requests-${op.id}-${app.dataVersion}'),
              // one row is enough: the counts come with the page
              load: () => app.requests.minePage(op.id, size: 1),
              builder: (context, page, _) {
                final c = RequestCounts.fromJson(page.summary);
                return ListView(padding: const EdgeInsets.all(16), children: [
                  AppCard(
                    color: AppColors.navy,
                    child: Row(children: [
                      const Icon(Icons.inventory_2,
                          color: AppColors.gold, size: 32),
                      const SizedBox(width: 12),
                      Expanded(
                          child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                            Text(context.l10n.stockRequests,
                                style: const TextStyle(
                                    color: Colors.white,
                                    fontWeight: FontWeight.w800,
                                    fontSize: 16)),
                            Text(context.l10n.stockRequestsSubtitle,
                                style:
                                    const TextStyle(color: AppColors.white70)),
                          ])),
                    ]),
                  ),
                  const SizedBox(height: 12),
                  AppCard(
                      child: MetricTile(
                          icon: Icons.inventory_2_outlined,
                          label: context.l10n.totalRequests,
                          value: '${c.total}',
                          iconColor: AppColors.blue,
                          iconBg: AppColors.blueSoft)),
                  const SizedBox(height: 10),
                  Row(children: [
                    _Count(
                        icon: Icons.check_circle_outline,
                        color: AppColors.green,
                        label: context.l10n.requestsApproved,
                        value: c.approved,
                        status: RequestStatus.approved),
                    _Count(
                        icon: Icons.hourglass_empty,
                        color: AppColors.amber,
                        label: context.l10n.requestsPending,
                        value: c.pending,
                        status: RequestStatus.pending),
                    _Count(
                        icon: Icons.cancel_outlined,
                        color: AppColors.red,
                        label: context.l10n.requestsRejected,
                        value: c.rejected,
                        status: RequestStatus.rejected),
                    _Count(
                        icon: Icons.local_shipping_outlined,
                        color: AppColors.blue,
                        label: context.l10n.requestsInFulfillment,
                        value: c.inFulfillment,
                        status: RequestStatus.inFulfillment),
                  ]),
                  if (app.user?.canRequestStock == true) ...[
                    const SizedBox(height: 16),
                    FilledButton.icon(
                      onPressed: () => startRequest(context),
                      icon: const Icon(Icons.add),
                      label: Text(context.l10n.requestStock),
                    ),
                  ],
                  const SizedBox(height: 16),
                  AppCard(
                    padding:
                        const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    child: Column(children: [
                      MenuTile(
                          icon: Icons.list_alt,
                          title: context.l10n.myRequests,
                          onTap: () =>
                              _push(context, const MyRequestsScreen())),
                      MenuTile(
                          icon: Icons.hourglass_empty,
                          title: context.l10n.pendingApprovals,
                          onTap: () => _push(
                              context,
                              const MyRequestsScreen(
                                  initialStatus: RequestStatus.pending))),
                    ]),
                  ),
                ]);
              },
            ),
    );
  }
}

class _Count extends StatelessWidget {
  const _Count(
      {required this.icon,
      required this.color,
      required this.label,
      required this.value,
      required this.status});
  final IconData icon;
  final Color color;
  final String label;
  final int value;
  final RequestStatus status;

  @override
  Widget build(BuildContext context) => Expanded(
        child: InkWell(
          borderRadius: BorderRadius.circular(12),
          onTap: () => Navigator.of(context).push(MaterialPageRoute(
              builder: (_) => MyRequestsScreen(initialStatus: status))),
          child: Padding(
            padding: const EdgeInsets.symmetric(vertical: 8),
            child: Column(children: [
              Icon(icon, color: color),
              const SizedBox(height: 4),
              Text('$value',
                  style: const TextStyle(
                      fontWeight: FontWeight.w800, fontSize: 18)),
              Text(label,
                  textAlign: TextAlign.center,
                  style:
                      const TextStyle(color: AppColors.muted, fontSize: 11.5)),
            ]),
          ),
        ),
      );
}
