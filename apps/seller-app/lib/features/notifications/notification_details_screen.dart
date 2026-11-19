import 'package:material_ui/material_ui.dart';

import 'package:lutiano_core/lutiano_core.dart';
import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import 'notification_widgets.dart';

/// Batch 7 · Screens 2, 9 and 10 — one notification in full. Opening it marks it as read.
class NotificationDetailsScreen extends StatelessWidget {
  const NotificationDetailsScreen({super.key, required this.notificationId});
  final String notificationId;

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final l = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l.notificationDetails)),
      body: AsyncView<AppNotification>(
        key: ValueKey('notification-$notificationId'),
        load: () async {
          final n = await app.notifications.get(notificationId);
          if (!n.isRead) {
            // reading it is what opening it means; the badge catches up
            await app.notifications.markRead([n.id]);
            app.refreshUnread();
          }
          return n;
        },
        builder: (context, n, _) {
          final color = categoryColor(n.category);
          final link = notificationLink(context, n);
          return ListView(padding: const EdgeInsets.all(16), children: [
            Center(
              child: Container(
                width: 72,
                height: 72,
                decoration: BoxDecoration(
                    color: color.withValues(alpha: 0.12),
                    borderRadius: BorderRadius.circular(20)),
                child: Icon(categoryIcon(n.category), color: color, size: 38),
              ),
            ),
            const SizedBox(height: 14),
            Text(notificationTitle(context, n),
                textAlign: TextAlign.center,
                style:
                    const TextStyle(fontWeight: FontWeight.w800, fontSize: 20)),
            const SizedBox(height: 10),
            Text(n.body,
                textAlign: TextAlign.center,
                style: const TextStyle(fontSize: 15)),
            const SizedBox(height: 16),
            AppCard(
              child: Column(children: [
                InfoRow(l.dateAndTime, dateTime(n.createdAt)),
                InfoRow(
                    l.statusLabel,
                    n.important
                        ? l.notifImportant
                        : categoryName(context, n.category)),
              ]),
            ),
            if (link != null) ...[
              const SizedBox(height: 16),
              FilledButton.icon(
                onPressed: () => Navigator.of(context)
                    .push(MaterialPageRoute(builder: (_) => link.screen())),
                icon: const Icon(Icons.open_in_new),
                label: Text('${l.notifView}: ${link.label}'),
              ),
            ],
          ]);
        },
      ),
    );
  }
}
