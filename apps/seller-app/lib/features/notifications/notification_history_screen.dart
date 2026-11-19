import 'package:material_ui/material_ui.dart';

import 'package:lutiano_core/lutiano_core.dart';
import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import 'notification_details_screen.dart';
import 'notification_widgets.dart';

/// Batch 7 · Screen 7 — Notification history: the last 7 or 30 days, or everything, grouped by day.
class NotificationHistoryScreen extends StatefulWidget {
  const NotificationHistoryScreen({super.key});

  @override
  State<NotificationHistoryScreen> createState() =>
      _NotificationHistoryScreenState();
}

class _NotificationHistoryScreenState extends State<NotificationHistoryScreen> {
  int? _days = 7;

  String _dayLabel(BuildContext context, DateTime t) {
    final today = isoDateOf(DateTime.now());
    final yesterday = doualaIsoDate(DateTime.now(), -1);
    final d = isoDateOf(t);
    if (d == today) return context.l10n.periodToday;
    if (d == yesterday) return context.l10n.yesterdayLabel;
    return dayMonthYear(t);
  }

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final l = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l.notificationHistory)),
      body: Column(children: [
        FilterChips<int>(
          options: [(7, l.lastNDays(7)), (30, l.lastNDays(30))],
          selected: _days,
          onSelected: (d) => setState(() => _days = d),
        ),
        Expanded(
          child: PagedListView<AppNotification>(
            key: ValueKey('history-$_days-${app.dataVersion}'),
            fetch: (page, size) =>
                app.notifications.page(days: _days, page: page, size: size),
            empty: EmptyState(icon: Icons.history, title: l.noNotifications),
            itemBuilder: (context, n, index, items) {
              final day = isoDateOf(n.createdAt);
              final newDay =
                  index == 0 || isoDateOf(items[index - 1].createdAt) != day;
              return Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    if (newDay) SectionLabel(_dayLabel(context, n.createdAt)),
                    NotificationTile(
                        notification: n,
                        onTap: () => Navigator.of(context).push(
                            MaterialPageRoute(
                                builder: (_) => NotificationDetailsScreen(
                                    notificationId: n.id)))),
                  ]);
            },
          ),
        ),
      ]),
    );
  }
}
