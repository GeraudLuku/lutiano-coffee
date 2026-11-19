import 'package:material_ui/material_ui.dart';

import 'package:lutiano_core/lutiano_core.dart';
import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import 'request_details_screen.dart';
import 'request_status.dart';

/// Batch 3 · Screen 4 — My Requests: filter by status, search by reference.
class MyRequestsScreen extends StatefulWidget {
  const MyRequestsScreen({super.key, this.initialStatus});
  final RequestStatus? initialStatus;

  @override
  State<MyRequestsScreen> createState() => _MyRequestsScreenState();
}

class _MyRequestsScreenState extends State<MyRequestsScreen> {
  // cancelled requests only show under "All"
  static const _filters = [
    RequestStatus.pending,
    RequestStatus.approved,
    RequestStatus.inFulfillment,
    RequestStatus.completed,
    RequestStatus.rejected,
  ];
  late RequestStatus? _status = widget.initialStatus;
  String _reference = '';

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final op = app.operation!;
    final labels = {
      for (final s in _filters) requestStatusLabel(context, s): s
    };
    return Scaffold(
      appBar: AppBar(title: Text(context.l10n.myRequests)),
      body: Column(children: [
        SearchFilterBar(
          hint: context.l10n.searchByReference,
          categories: labels.keys.toList(),
          category:
              _status == null ? null : requestStatusLabel(context, _status!),
          onSearch: (q) => setState(() => _reference = q),
          onCategory: (label) => setState(() => _status = labels[label]),
        ),
        Expanded(
          child: PagedListView<StockRequest>(
            key: ValueKey(
                'my-requests-${op.id}-${app.dataVersion}-$_status-$_reference'),
            fetch: (page, size) => app.requests.minePage(op.id,
                status: _status, reference: _reference, page: page, size: size),
            empty: EmptyState(
                icon: Icons.inventory_2_outlined,
                title: _status == null && _reference.isEmpty
                    ? context.l10n.noRequestsYet
                    : context.l10n.noRequestsMatch,
                message: context.l10n.requestsWillAppearHere),
            itemBuilder: (context, r, _, __) => Padding(
              padding: const EdgeInsets.only(bottom: 8),
              child: AppCard(
                onTap: () => Navigator.of(context).push(MaterialPageRoute(
                    builder: (_) => RequestDetailsScreen(requestId: r.id))),
                child: Row(children: [
                  Expanded(
                      child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                        Text(r.reference,
                            style:
                                const TextStyle(fontWeight: FontWeight.w800)),
                        Text(
                            context.l10n.requestItemsLine(
                                dayMonthYear(r.createdAt), r.itemCount),
                            style: const TextStyle(
                                color: AppColors.muted, fontSize: 12.5)),
                      ])),
                  requestStatusChip(context, r.status),
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
