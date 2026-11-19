import 'package:lutiano_core/lutiano_core.dart';

/// How the app groups notifications.
enum NotificationCategory {
  inventory('INVENTORY'),
  requests('REQUESTS'),
  sales('SALES'),
  earnings('EARNINGS'),
  targets('TARGETS'),
  operations('OPERATIONS'),
  system('SYSTEM');

  const NotificationCategory(this.api);
  final String api;

  static NotificationCategory fromApi(String v) =>
      values.firstWhere((c) => c.api == v, orElse: () => system);
}

/// A message for the seller in the app. Events are titled by the app from [eventKey]; an announcement carries its own [title].
class AppNotification {
  const AppNotification({
    required this.id,
    required this.eventKey,
    required this.category,
    required this.body,
    required this.important,
    required this.createdAt,
    this.title,
    this.relatedType,
    this.relatedId,
    this.readAt,
  });

  factory AppNotification.fromJson(Map<String, dynamic> j) => AppNotification(
        id: asString(j['id']),
        eventKey: asString(j['event_key']),
        category: NotificationCategory.fromApi(asString(j['category'])),
        body: asString(j['body']),
        important: asBool(j['important']),
        createdAt: asTime(j['created_at']),
        title: asStringOrNull(j['title']),
        relatedType: asStringOrNull(j['related_type']),
        relatedId: asStringOrNull(j['related_id']),
        readAt: asTimeOrNull(j['read_at']),
      );

  final String id, eventKey, body;
  final NotificationCategory category;
  final bool important;
  final DateTime createdAt;
  final String? title, relatedType, relatedId;
  final DateTime? readAt;

  bool get isRead => readAt != null;
}

/// Counts over everything not cleared, whatever the list is filtered to.
class NotificationCounts {
  const NotificationCounts(
      {this.total = 0,
      this.unread = 0,
      this.important = 0,
      this.categories = const {}});

  factory NotificationCounts.fromJson(Map<String, dynamic> j) =>
      NotificationCounts(
        total: asInt(j['total']),
        unread: asInt(j['unread']),
        important: asInt(j['important']),
        categories: {
          for (final e in ((j['categories'] as Map?) ?? const {}).entries)
            NotificationCategory.fromApi('${e.key}'): (
              total: asInt((e.value as Map)['total']),
              unread: asInt((e.value as Map)['unread'])
            )
        },
      );

  final int total, unread, important;
  final Map<NotificationCategory, ({int total, int unread})> categories;

  int unreadIn(NotificationCategory c) => categories[c]?.unread ?? 0;
}

/// Whether a category is on in the app, and whether it can be switched off.
class NotificationPreference {
  const NotificationPreference(
      {required this.category,
      required this.inApp,
      required this.push,
      required this.locked});

  factory NotificationPreference.fromJson(Map<String, dynamic> j) =>
      NotificationPreference(
        category: NotificationCategory.fromApi(asString(j['category'])),
        inApp: asBool(j['in_app']),
        push: asBool(j['push']),
        locked: asBool(j['locked']),
      );

  final NotificationCategory category;
  final bool inApp, push, locked;
}
