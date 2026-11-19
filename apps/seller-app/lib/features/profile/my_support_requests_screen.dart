import 'package:material_ui/material_ui.dart';

import 'package:lutiano_core/lutiano_core.dart';
import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import 'support_details_screen.dart';
import 'support_widgets.dart';

/// The seller's help requests, with where each stands.
class MySupportRequestsScreen extends StatelessWidget {
  const MySupportRequestsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final l = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l.mySupportRequests)),
      body: PagedListView<SupportRequest>(
        key: ValueKey('support-${app.dataVersion}'),
        fetch: (page, size) => app.support.page(page: page, size: size),
        empty:
            EmptyState(icon: Icons.inbox_outlined, title: l.noSupportRequests),
        itemBuilder: (context, r, _, __) => Padding(
          padding: const EdgeInsets.only(bottom: 8),
          child: AppCard(
            onTap: () => Navigator.of(context).push(MaterialPageRoute(
                builder: (_) => SupportDetailsScreen(requestId: r.id))),
            child: Row(children: [
              Expanded(
                  child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                    Text(r.subject,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(fontWeight: FontWeight.w800)),
                    Text('${r.reference} · ${dayMonthYear(r.createdAt)}',
                        style: const TextStyle(
                            color: AppColors.muted, fontSize: 12.5)),
                  ])),
              supportStatusChip(context, r.status),
              const Icon(Icons.chevron_right, color: AppColors.muted),
            ]),
          ),
        ),
      ),
    );
  }
}
