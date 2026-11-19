import 'package:lutiano_core/lutiano_core.dart';
import '../models.dart';

/// The seller's in-app notifications and what they chose to receive.
class NotificationsRepository {
  NotificationsRepository(this._api);
  final ApiClient _api;

  /// Newest first. [filter] is UNREAD or IMPORTANT, [days] limits to the last N days. The page summary counts everything not cleared.
  Future<Paged<AppNotification>> page(
          {NotificationCategory? category,
          String? filter,
          int? days,
          int page = 1,
          int size = 20}) async =>
      Paged.fromJson(
          await _api.page('/me/notifications',
              query: {
                if (category != null) 'category': category.api,
                if (filter != null) 'filter': filter,
                if (days != null) 'days': '$days',
              },
              page: page,
              size: size),
          AppNotification.fromJson);

  Future<AppNotification> get(String id) async => AppNotification.fromJson(
      (await _api.get('/me/notifications/$id') as Map).cast<String, dynamic>());

  Future<int> unreadCount() async => asInt(
      (await _api.get('/me/notifications/unread-count') as Map)['unread']);

  /// Marks the given notifications as read; with no ids, all of them.
  Future<void> markRead([List<String>? ids]) =>
      _api.post('/me/notifications/read', body: {'ids': ids});

  /// Hides the given notifications; with no ids, every one already read.
  Future<void> clear([List<String>? ids]) =>
      _api.post('/me/notifications/clear', body: {'ids': ids});

  Future<List<NotificationPreference>> preferences() async =>
      asList(await _api.get('/me/notifications/preferences'))
          .map(NotificationPreference.fromJson)
          .toList();

  /// Tells the server this phone's Firebase token, so it can send notifications here.
  Future<void> registerDevice(String token) =>
      _api.post('/me/notifications/devices',
          body: {'token': token, 'platform': 'ANDROID'});

  Future<void> unregisterDevice(String token) =>
      _api.delete('/me/notifications/devices', query: {'token': token});

  Future<void> setPush(NotificationCategory category, bool on) => _api
      .put('/me/notifications/preferences/${category.api}', body: {'push': on});

  Future<void> setInApp(NotificationCategory category, bool on) =>
      _api.put('/me/notifications/preferences/${category.api}',
          body: {'inApp': on});
}
