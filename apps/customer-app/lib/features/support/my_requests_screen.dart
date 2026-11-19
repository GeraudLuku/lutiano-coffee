import 'package:lutiano_core/lutiano_core.dart';
import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import 'request_details_screen.dart';

StatusChip requestStatusChip(BuildContext context, String status) {
  final l = context.l10n;
  return switch (status) {
    'RESOLVED' => StatusChip(l.reqResolved),
    'IN_PROGRESS' => StatusChip(l.reqInProgress,
        color: AppColors.blue, background: AppColors.blueSoft),
    _ => StatusChip(l.reqOpen,
        color: AppColors.amber, background: AppColors.amberSoft),
  };
}

/// The customer's requests to the team and where each stands.
class MyRequestsScreen extends StatelessWidget {
  const MyRequestsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final l = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l.myRequests)),
      body: PagedListView<SupportRequest>(
        key: ValueKey('requests-${app.ordersVersion}'),
        fetch: (page, size) => app.inbox.requests(page: page, size: size),
        empty: EmptyState(
            icon: Icons.inbox_outlined,
            title: l.noRequests,
            message: l.noRequestsMessage),
        itemBuilder: (context, r, _, __) => Padding(
          padding: const EdgeInsets.only(bottom: 10),
          child: AppCard(
            onTap: () => Navigator.of(context).push(MaterialPageRoute(
                builder: (_) => RequestDetailsScreen(requestId: r.id))),
            child: Row(children: [
              Expanded(
                  child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                    Text(r.subject,
                        style: const TextStyle(fontWeight: FontWeight.w800)),
                    Text('${r.reference} · ${dateTime(r.createdAt)}',
                        style: const TextStyle(
                            color: AppColors.muted, fontSize: 12.5)),
                  ])),
              requestStatusChip(context, r.status),
            ]),
          ),
        ),
      ),
    );
  }
}
