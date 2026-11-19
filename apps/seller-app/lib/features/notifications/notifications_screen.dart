import 'package:material_ui/material_ui.dart';

import 'package:lutiano_core/lutiano_core.dart';
import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import 'notification_categories_screen.dart';
import 'notification_details_screen.dart';
import 'notification_history_screen.dart';
import 'notification_preferences_screen.dart';
import 'notification_widgets.dart';

/// Batch 7 · Screens 1 and 6 — the notification center: All, Unread and Important, mark as read, clear, and select several.
class NotificationsScreen extends StatefulWidget {
  const NotificationsScreen({super.key, this.category});

  /// Shows only this category (opened from Notification Categories).
  final NotificationCategory? category;

  @override
  State<NotificationsScreen> createState() => _NotificationsScreenState();
}

class _NotificationsScreenState extends State<NotificationsScreen> {
  String? _filter; // null = all, UNREAD, IMPORTANT
  int _version = 0;
  final Set<String> _selected = {};

  bool get _selecting => _selected.isNotEmpty;

  void _toggle(AppNotification n) => setState(() {
        if (!_selected.add(n.id)) _selected.remove(n.id);
      });

  Future<void> _open(AppNotification n) async {
    if (_selecting) return _toggle(n);
    await Navigator.of(context).push(MaterialPageRoute(
        builder: (_) => NotificationDetailsScreen(notificationId: n.id)));
    if (mounted) _reload();
  }

  void _reload() {
    AppScope.read(context).refreshUnread();
    setState(() => _version++);
  }

  Future<void> _run(Future<void> Function() action) async {
    try {
      await action();
    } catch (e) {
      if (mounted) showMessage(context, errorMessage(e), error: true);
      return;
    }
    if (!mounted) return;
    setState(_selected.clear);
    _reload();
  }

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final l = context.l10n;
    final c = widget.category;
    return Scaffold(
      appBar: AppBar(
        title: Text(_selecting
            ? l.selectedCount(_selected.length)
            : c == null
                ? l.notifications
                : categoryName(context, c)),
        leading: _selecting
            ? IconButton(
                tooltip: MaterialLocalizations.of(context).cancelButtonLabel,
                icon: const Icon(Icons.close),
                onPressed: () => setState(_selected.clear))
            : null,
        actions: [
          if (!_selecting)
            PopupMenuButton<String>(
              onSelected: (v) async {
                switch (v) {
                  case 'read':
                    await _run(() => app.notifications.markRead());
                  case 'clear':
                    await _run(() => app.notifications.clear());
                  case 'categories':
                    await _push(const NotificationCategoriesScreen());
                  case 'history':
                    await _push(const NotificationHistoryScreen());
                  case 'preferences':
                    await _push(const NotificationPreferencesScreen());
                }
              },
              itemBuilder: (context) => [
                PopupMenuItem(value: 'read', child: Text(l.markAllAsRead)),
                PopupMenuItem(value: 'clear', child: Text(l.clearRead)),
                if (c == null)
                  PopupMenuItem(
                      value: 'categories',
                      child: Text(l.notificationCategories)),
                if (c == null)
                  PopupMenuItem(
                      value: 'history', child: Text(l.notificationHistory)),
                PopupMenuItem(
                    value: 'preferences', child: Text(l.managePreferences)),
              ],
            ),
        ],
      ),
      bottomNavigationBar: _selecting
          ? SafeArea(
              child: Padding(
                padding: const EdgeInsets.all(12),
                child: Row(children: [
                  Expanded(
                      child: OutlinedButton.icon(
                          onPressed: () => _run(() =>
                              app.notifications.markRead(_selected.toList())),
                          icon: const Icon(Icons.done_all),
                          label: Text(l.markAsRead))),
                  const SizedBox(width: 10),
                  Expanded(
                      child: FilledButton.icon(
                          onPressed: () => _run(() =>
                              app.notifications.clear(_selected.toList())),
                          icon: const Icon(Icons.delete_outline),
                          label: Text(l.clearSelected))),
                ]),
              ),
            )
          : null,
      body: PagedListView<AppNotification>(
        key: ValueKey('notifications-$c-$_filter-$_version-${app.dataVersion}'),
        fetch: (page, size) => app.notifications
            .page(category: c, filter: _filter, page: page, size: size),
        header: (context, controller) {
          final counts = NotificationCounts.fromJson(controller.summary);
          Widget chip(String? value, String label, int count) => Padding(
                padding: const EdgeInsets.only(right: 8),
                child: ChoiceChip(
                  label: Text(controller.last == null
                      ? label
                      : l.notifFilterLabel(label, count)),
                  selected: _filter == value,
                  showCheckmark: false,
                  selectedColor: AppColors.gold,
                  onSelected: (_) => setState(() => _filter = value),
                ),
              );
          return Padding(
            padding: const EdgeInsets.only(bottom: 12),
            child: SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              child: Row(children: [
                chip(null, l.notifAll, counts.total),
                chip('UNREAD', l.notifUnread, counts.unread),
                chip('IMPORTANT', l.notifImportant, counts.important),
              ]),
            ),
          );
        },
        empty: EmptyState(
            icon: Icons.notifications_none,
            title: l.noNotifications,
            message: l.noNotificationsMessage),
        itemBuilder: (context, n, _, __) => NotificationTile(
          notification: n,
          selected: _selecting ? _selected.contains(n.id) : null,
          onTap: () => _open(n),
          onLongPress: () => _toggle(n),
        ),
      ),
    );
  }

  Future<void> _push(Widget screen) async {
    await Navigator.of(context).push(MaterialPageRoute(builder: (_) => screen));
    if (mounted) _reload();
  }
}
