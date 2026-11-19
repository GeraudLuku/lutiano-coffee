import 'package:lutiano_core/lutiano_core.dart';
import '../models.dart';

/// The signed-in seller's own account: profile, phone, photo, sign-in history and sessions.
class AccountRepository {
  AccountRepository(this._api);
  final ApiClient _api;

  /// Changes the name and/or the language (fr or en); returns the updated profile.
  Future<UserProfile> update({String? fullName, String? language}) async =>
      UserProfile.fromJson((await _api.put('/me/profile', body: {
        if (fullName != null) 'fullName': fullName,
        if (language != null) 'language': language,
      }) as Map)
          .cast<String, dynamic>());

  /// Sends a code to [phone]; the number changes only once [confirmPhone] succeeds.
  Future<void> startPhoneChange(String phone) =>
      _api.post('/me/phone', body: {'phone': phone});

  Future<UserProfile> confirmPhone(String code) async => UserProfile.fromJson(
      (await _api.post('/me/phone/confirm', body: {'code': code}) as Map)
          .cast<String, dynamic>());

  /// Uploads a new profile photo and returns its address.
  Future<String> uploadPhoto(
          List<int> bytes, String fileName, String contentType) async =>
      asString((await _api.upload('/me/photo',
          bytes: bytes,
          fileName: fileName,
          contentType: contentType) as Map)['imageUrl']);

  Future<Paged<AccountActivity>> activityPage(
          {int page = 1, int size = 20}) async =>
      Paged.fromJson(await _api.page('/me/activity', page: page, size: size),
          AccountActivity.fromJson);

  /// Signs out every other device and returns a fresh token for this one.
  Future<String> signOutEverywhere() async =>
      asString((await _api.post('/me/sessions/revoke') as Map)['token']);
}

/// Help requests to the LUTIANO team.
class SupportRepository {
  SupportRepository(this._api);
  final ApiClient _api;

  Future<Paged<SupportRequest>> page(
          {SupportStatus? status, int page = 1, int size = 20}) async =>
      Paged.fromJson(
          await _api.page('/me/support',
              query: {if (status != null) 'status': status.api},
              page: page,
              size: size),
          SupportRequest.fromJson);

  Future<SupportRequest> get(String id) async => SupportRequest.fromJson(
      (await _api.get('/me/support/$id') as Map).cast<String, dynamic>());

  /// Sends a request, with an optional image (up to 10 MB).
  Future<SupportRequest> create(
          {required SupportType type,
          required String subject,
          required String description,
          List<int>? fileBytes,
          String? fileName,
          String? contentType}) async =>
      SupportRequest.fromJson((await _api.upload('/me/support',
              fields: {
                'type': type.api,
                'subject': subject,
                'description': description
              },
              bytes: fileBytes,
              fileName: fileName,
              contentType: contentType) as Map)
          .cast<String, dynamic>());
}
