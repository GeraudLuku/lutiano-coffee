import 'package:lutiano_core/lutiano_core.dart';
import '../models.dart';

class CustomerRepository {
  CustomerRepository(this._api);
  final ApiClient _api;

  /// Who is behind a scanned loyalty QR. Throws an [ApiException] with a message for an invalid or suspended account.
  Future<ScannedCustomer> scan(String token) async => ScannedCustomer.fromJson(
      ((await _api.get('/customers/scan', query: {'token': token})) as Map)
          .cast<String, dynamic>());
}
