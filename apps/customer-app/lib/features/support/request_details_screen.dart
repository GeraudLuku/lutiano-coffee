import 'package:lutiano_core/lutiano_core.dart';
import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import 'my_requests_screen.dart';

/// A request, its status and the team's reply.
class RequestDetailsScreen extends StatelessWidget {
  const RequestDetailsScreen({super.key, required this.requestId});
  final String requestId;

  @override
  Widget build(BuildContext context) {
    final app = AppScope.read(context);
    final l = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l.supportRequest)),
      body: AsyncView<SupportRequest>(
        load: () => app.inbox.request(requestId),
        builder: (context, r, _) =>
            ListView(padding: const EdgeInsets.all(16), children: [
          Row(children: [
            Expanded(
                child: Text(r.reference,
                    style: const TextStyle(
                        fontSize: 18, fontWeight: FontWeight.w800))),
            requestStatusChip(context, r.status)
          ]),
          const SizedBox(height: 4),
          Text(dateTime(r.createdAt),
              style: const TextStyle(color: AppColors.muted)),
          SectionLabel(r.subject),
          AppCard(child: Text(r.description)),
          SectionLabel(l.teamReply),
          if (r.reply == null)
            InfoNote(l.noReplyYet)
          else
            AppCard(
                color: AppColors.greenSoft,
                child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(r.reply!),
                      if (r.repliedAt != null)
                        Padding(
                            padding: const EdgeInsets.only(top: 6),
                            child: Text(dateTime(r.repliedAt!),
                                style: const TextStyle(
                                    color: AppColors.muted, fontSize: 12)))
                    ])),
        ]),
      ),
    );
  }
}
