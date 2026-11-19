import 'package:lutiano_core/lutiano_core.dart';

import '../models.dart';

/// The public menu and sales points (readable without signing in).
class CatalogueRepository {
  CatalogueRepository(this._api);
  final ApiClient _api;

  Future<List<MenuCategory>> categories() async =>
      (await _api.getAll('/public/categories'))
          .map(MenuCategory.fromJson)
          .toList();

  Future<Paged<MenuItem>> products(
          {String? category, String? q, int page = 1, int size = 20}) async =>
      Paged.fromJson(
          await _api.page('/public/products',
              query: {
                if (category != null) 'category': category,
                if (q != null && q.isNotEmpty) 'q': q
              },
              page: page,
              size: size),
          MenuItem.fromJson);

  Future<MenuItem> product(String id) async => MenuItem.fromJson(
      (await _api.get('/public/products/$id') as Map).cast<String, dynamic>());

  /// The menu at one sales point: each product says whether it is available there now.
  Future<Paged<MenuItem>> productsAt(String salesPointId,
          {String? category, String? q, int page = 1, int size = 20}) async =>
      Paged.fromJson(
          await _api.page('/public/operations/$salesPointId/products',
              query: {
                if (category != null) 'category': category,
                if (q != null && q.isNotEmpty) 'q': q
              },
              page: page,
              size: size),
          MenuItem.fromJson);

  Future<Paged<SalesPoint>> salesPoints(
          {String? q, int page = 1, int size = 20}) async =>
      Paged.fromJson(
          await _api.page('/public/operations',
              query: {if (q != null && q.isNotEmpty) 'q': q},
              page: page,
              size: size),
          SalesPoint.fromJson);

  Future<SalesPoint> salesPoint(String id) async =>
      SalesPoint.fromJson((await _api.get('/public/operations/$id') as Map)
          .cast<String, dynamic>());

  /// The sales point behind a scanned or typed code ("LUTIANO:OP:…").
  Future<SalesPoint> salesPointByCode(String code) async => SalesPoint.fromJson(
      (await _api.get('/public/operations/by-qr', query: {'code': code.trim()})
              as Map)
          .cast<String, dynamic>());
}
