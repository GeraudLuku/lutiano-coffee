import 'package:material_ui/material_ui.dart';

import 'package:lutiano_core/lutiano_core.dart';
import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import 'support_widgets.dart';

/// One help request and the team's reply.
class SupportDetailsScreen extends StatelessWidget {
  const SupportDetailsScreen({super.key, required this.requestId});
  final String requestId;

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final l = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l.supportRequest)),
      body: AsyncView<SupportRequest>(
        key: ValueKey('support-$requestId-${app.dataVersion}'),
        load: () => app.support.get(requestId),
        builder: (context, r, _) =>
            ListView(padding: const EdgeInsets.all(16), children: [
          AppCard(
            child: Row(children: [
              Expanded(
                  child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                    Text(r.reference,
                        style: const TextStyle(
                            fontWeight: FontWeight.w800, fontSize: 16)),
                    Text(
                        '${supportTypeLabel(context, r.type)} · ${dateTime(r.createdAt)}',
                        style: const TextStyle(
                            color: AppColors.muted, fontSize: 12.5)),
                  ])),
              supportStatusChip(context, r.status),
            ]),
          ),
          const SizedBox(height: 12),
          AppCard(
            child:
                Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(r.subject,
                  style: const TextStyle(fontWeight: FontWeight.w800)),
              const SizedBox(height: 6),
              Text(r.description),
              if (r.attachmentName != null) ...[
                const SizedBox(height: 8),
                Row(children: [
                  const Icon(Icons.attach_file,
                      size: 16, color: AppColors.muted),
                  Expanded(
                      child: Text(r.attachmentName!,
                          style: const TextStyle(color: AppColors.muted))),
                ]),
              ],
            ]),
          ),
          SectionLabel(l.teamReply),
          if (r.reply == null)
            InfoNote(l.noReplyYet)
          else
            InfoNote(r.reply!,
                icon: Icons.mark_chat_read_outlined,
                color: AppColors.green,
                background: AppColors.greenSoft),
        ]),
      ),
    );
  }
}
