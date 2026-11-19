import 'package:lutiano_core/lutiano_core.dart';
import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import 'notification_preferences_screen.dart';
import 'notification_target.dart';

/// The inbox: orders, rewards, offers and news, newest first. Opening a message marks it read and goes to what it is about.
class NotificationsScreen extends StatefulWidget {
  const NotificationsScreen({super.key});

  @override
  State<NotificationsScreen> createState() => _NotificationsScreenState();
}

class _NotificationsScreenState extends State<NotificationsScreen> {
  String? _category;
  int _version = 0;

  Future<void> _open(AppNotification n) async {
    final app = AppScope.read(context);
    if (!n.isRead) {
      app.inbox
          .markRead([n.id])
          .then((_) => app.refreshUnread())
          .catchError((_) {});
    }
    final screen = notificationScreen(n);
    if (screen != null) {
      await Navigator.of(context)
          .push(MaterialPageRoute(builder: (_) => screen));
    } else if (mounted) {
      await showDialog<void>(
        context: context,
        builder: (ctx) => AlertDialog(
            title: Text(notificationTitle(context, n)),
            content: Text(n.body),
            actions: [
              TextButton(
                  onPressed: () => Navigator.of(ctx).pop(),
                  child: Text(context.l10n.close))
            ]),
      );
    }
    if (mounted) setState(() => _version++);
  }

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final l = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l.notifications), actions: [
        IconButton(
            tooltip: l.markAllRead,
            icon: const Icon(Icons.done_all),
            onPressed: () async {
              try {
                await app.inbox.markRead();
                await app.refreshUnread();
                if (mounted) setState(() => _version++);
              } catch (e) {
                if (context.mounted) {
                  showMessage(context, errorMessage(e), error: true);
                }
              }
            }),
        IconButton(
            tooltip: l.notificationSettings,
            icon: const Icon(Icons.tune),
            onPressed: () => Navigator.of(context).push(MaterialPageRoute(
                builder: (_) => const NotificationPreferencesScreen()))),
      ]),
      body: Column(children: [
        FilterChips<String>(
            options: [
              ('ORDERS', l.catOrders),
              ('LOYALTY', l.catLoyalty),
              ('PROMOTIONS', l.catPromotions),
              ('SYSTEM', l.catSystem)
            ],
            selected: _category,
            onSelected: (c) => setState(() => _category = c)),
        Expanded(
          child: PagedListView<AppNotification>(
            key: ValueKey('inbox-$_category-$_version-${app.ordersVersion}'),
            fetch: (page, size) => app.inbox
                .notifications(category: _category, page: page, size: size),
            empty: EmptyState(
                icon: Icons.notifications_none,
                title: l.noNotifications,
                message: l.noNotificationsMessage),
            itemBuilder: (context, n, _, __) => Padding(
              padding: const EdgeInsets.only(bottom: 10),
              child: AppCard(
                color: n.isRead ? Colors.white : AppColors.goldSoft,
                onTap: () => _open(n),
                child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Icon(categoryIcon(n.category), color: AppColors.amber),
                      const SizedBox(width: 12),
                      Expanded(
                          child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                            Text(notificationTitle(context, n),
                                style: TextStyle(
                                    fontWeight: n.isRead
                                        ? FontWeight.w600
                                        : FontWeight.w800)),
                            const SizedBox(height: 2),
                            Text(n.body,
                                maxLines: 3, overflow: TextOverflow.ellipsis),
                            const SizedBox(height: 4),
                            Text(dateTime(n.createdAt),
                                style: const TextStyle(
                                    color: AppColors.muted, fontSize: 12)),
                          ])),
                      if (!n.isRead)
                        const Padding(
                            padding: EdgeInsets.only(left: 8, top: 4),
                            child: CircleAvatar(
                                radius: 5, backgroundColor: AppColors.red)),
                    ]),
              ),
            ),
          ),
        ),
      ]),
    );
  }
}
