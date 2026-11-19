import 'package:lutiano_core/lutiano_core.dart';
import '../models.dart';

class CatalogRepository {
  CatalogRepository(this._api);
  final ApiClient _api;

  Future<List<Operation>> myOperations() async =>
      (await _api.getAll('/operations')).map(Operation.fromJson).toList();

  Future<OperationDetail> operation(String id) async =>
      OperationDetail.fromJson(
          (await _api.get('/operations/$id') as Map).cast<String, dynamic>());

  String operationQrUrl(String id) =>
      _api.uri('/operations/$id/qr.png').toString();
}
