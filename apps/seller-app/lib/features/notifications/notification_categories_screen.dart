import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import 'package:lutiano_core/lutiano_core.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import 'notifications_screen.dart';
import 'notification_widgets.dart';

/// Batch 7 · Screen 3 — Notification categories with how many are new in each.
class NotificationCategoriesScreen extends StatelessWidget {
  const NotificationCategoriesScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final l = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l.notificationCategories)),
      body: AsyncView<Paged<AppNotification>>(
        key: ValueKey('categories-${app.dataVersion}'),
        // one row is enough: the counts per category come with the page
        load: () => app.notifications.page(size: 1),
        builder: (context, page, _) {
          final counts = NotificationCounts.fromJson(page.summary);
          return ListView(padding: const EdgeInsets.all(16), children: [
            InfoNote(l.notificationsSubtitle,
                icon: Icons.filter_list,
                color: AppColors.amber,
                background: AppColors.amberSoft),
            const SizedBox(height: 12),
            AppCard(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
              child: Column(children: [
                for (final c in NotificationCategory.values)
                  ListTile(
                    contentPadding: EdgeInsets.zero,
                    leading: Icon(categoryIcon(c), color: categoryColor(c)),
                    title: Text(categoryName(context, c)),
                    trailing: Row(mainAxisSize: MainAxisSize.min, children: [
                      if (counts.unreadIn(c) > 0)
                        StatusChip(l.newCount(counts.unreadIn(c))),
                      const Icon(Icons.chevron_right, color: AppColors.muted),
                    ]),
                    onTap: () => Navigator.of(context).push(MaterialPageRoute(
                        builder: (_) => NotificationsScreen(category: c))),
                  ),
              ]),
            ),
          ]);
        },
      ),
    );
  }
}
