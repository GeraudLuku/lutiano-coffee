import 'package:lutiano_core/lutiano_core.dart';

import '../models.dart';

/// The customer's notification inbox and choices, and their help requests.
class InboxRepository {
  InboxRepository(this._api);
  final ApiClient _api;

  Map<String, dynamic> _map(Object? d) => (d as Map).cast<String, dynamic>();

  /// [category] is ORDERS, LOYALTY, PROMOTIONS or SYSTEM.
  Future<Paged<AppNotification>> notifications(
          {String? category, int page = 1, int size = 20}) async =>
      Paged.fromJson(
          await _api.page('/customer/notifications',
              query: {if (category != null) 'category': category},
              page: page,
              size: size),
          AppNotification.fromJson);

  Future<AppNotification> notification(String id) async =>
      AppNotification.fromJson(
          _map(await _api.get('/customer/notifications/$id')));

  /// Marks the given messages as read; with no ids, all of them.
  Future<void> markRead([List<String>? ids]) =>
      _api.post('/customer/notifications/read', body: {'ids': ids});

  Future<void> clear([List<String>? ids]) =>
      _api.post('/customer/notifications/clear', body: {'ids': ids});

  Future<List<NotificationPreference>> preferences() async =>
      asList(await _api.get('/customer/notifications/preferences'))
          .map(NotificationPreference.fromJson)
          .toList();

  Future<List<NotificationPreference>> setPreference(String category,
          {bool? inApp, bool? push}) async =>
      asList(await _api.put('/customer/notifications/preferences/$category',
              body: {
            if (inApp != null) 'inApp': inApp,
            if (push != null) 'push': push
          }))
          .map(NotificationPreference.fromJson)
          .toList();

  // ----- support -----

  Future<Paged<SupportRequest>> requests({int page = 1, int size = 20}) async =>
      Paged.fromJson(
          await _api.page('/customer/support', page: page, size: size),
          SupportRequest.fromJson);

  Future<SupportRequest> request(String id) async =>
      SupportRequest.fromJson(_map(await _api.get('/customer/support/$id')));

  Future<SupportRequest> sendRequest(
          {required String type,
          required String subject,
          required String description,
          List<int>? imageBytes,
          String? imageName,
          String? imageType}) async =>
      SupportRequest.fromJson(_map(await _api.upload('/customer/support',
          fields: {
            'type': type,
            'subject': subject,
            'description': description
          },
          bytes: imageBytes,
          fileName: imageName,
          contentType: imageType)));
}
