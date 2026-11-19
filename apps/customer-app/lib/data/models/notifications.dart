import 'package:lutiano_core/lutiano_core.dart';

/// A message in the customer's inbox.
class AppNotification {
  const AppNotification(
      {required this.id,
      required this.eventKey,
      required this.category,
      required this.body,
      required this.important,
      required this.createdAt,
      this.title,
      this.relatedType,
      this.relatedId,
      this.readAt});
  factory AppNotification.fromJson(Map<String, dynamic> j) => AppNotification(
      id: asString(j['id']),
      eventKey: asString(j['event_key']),
      category: asString(j['category']),
      title: asStringOrNull(j['title']),
      body: asString(j['body']),
      important: asBool(j['important']),
      relatedType: asStringOrNull(j['related_type']),
      relatedId: asStringOrNull(j['related_id']),
      createdAt: asTime(j['created_at']),
      readAt: asTimeOrNull(j['read_at']));
  final String id, eventKey, category, body;
  final String? title, relatedType, relatedId;
  final bool important;
  final DateTime createdAt;
  final DateTime? readAt;
  bool get isRead => readAt != null;
}

/// Whether messages of one category show in the app and as push on this phone.
class NotificationPreference {
  const NotificationPreference(
      {required this.category,
      required this.inApp,
      required this.push,
      required this.locked});
  factory NotificationPreference.fromJson(Map<String, dynamic> j) =>
      NotificationPreference(
          category: asString(j['category']),
          inApp: asBool(j['in_app']),
          push: asBool(j['push']),
          locked: asBool(j['locked']));
  final String category;
  final bool inApp, push, locked;
}
