import 'package:lutiano_core/lutiano_core.dart';

class MenuCategory {
  const MenuCategory({required this.id, required this.name, this.imageUrl});
  factory MenuCategory.fromJson(Map<String, dynamic> j) => MenuCategory(
      id: asString(j['id']),
      name: asString(j['name']),
      imageUrl: asStringOrNull(j['image_url']));
  final String id, name;
  final String? imageUrl;
}

/// A size of a product (e.g. Small, Regular, Large) with its own price.
class ProductSize {
  const ProductSize(
      {required this.id, required this.name, required this.priceXaf});
  factory ProductSize.fromJson(Map<String, dynamic> j) => ProductSize(
      id: asString(j['id']),
      name: asString(j['variant_name']),
      priceXaf: asInt(j['price_xaf']));
  final String id, name;
  final int priceXaf;
}

class Addon {
  const Addon({required this.id, required this.name, required this.priceXaf});
  factory Addon.fromJson(Map<String, dynamic> j) => Addon(
      id: asString(j['id']),
      name: asString(j['name']),
      priceXaf: asInt(j['price_xaf']));
  final String id, name;
  final int priceXaf;
}

/// A product on the menu. A product with [sizes] is sold by size (its price is the lowest one).
class MenuItem {
  const MenuItem({
    required this.id,
    required this.name,
    required this.category,
    required this.priceXaf,
    this.description,
    this.imageUrl,
    this.sizes = const [],
    this.addons = const [],
    this.available = true,
  });

  factory MenuItem.fromJson(Map<String, dynamic> j) => MenuItem(
        id: asString(j['id']),
        name: asString(j['name']),
        category: asString(j['category']),
        priceXaf: asInt(j['price_xaf']),
        description: asStringOrNull(j['description']),
        imageUrl: asStringOrNull(j['image_url']),
        sizes: asList(j['variants']).map(ProductSize.fromJson).toList(),
        addons: asList(j['addons']).map(Addon.fromJson).toList(),
        available: j['available'] == null ? true : asBool(j['available']),
      );

  final String id, name, category;
  final int priceXaf;
  final String? description, imageUrl;
  final List<ProductSize> sizes;
  final List<Addon> addons;

  /// At the sales point being browsed: whether it can be had there now.
  final bool available;

  bool get hasSizes => sizes.isNotEmpty;
}

class OpeningHours {
  const OpeningHours(this.weekday, this.opens, this.closes);
  factory OpeningHours.fromJson(Map<String, dynamic> j) => OpeningHours(
      asInt(j['weekday']),
      asString(j['opens']).substring(0, 5),
      asString(j['closes']).substring(0, 5));
  final int weekday;
  final String opens, closes;
}

/// A LUTIANO sales point (a shop, a mobile barista or a market stand).
class SalesPoint {
  const SalesPoint({
    required this.id,
    required this.name,
    required this.type,
    required this.openNow,
    this.address,
    this.city,
    this.description,
    this.imageUrl,
    this.hours = const [],
    this.paymentMethods = const [],
  });

  factory SalesPoint.fromJson(Map<String, dynamic> j) => SalesPoint(
        id: asString(j['id']),
        name: asString(j['name']),
        type: asString(j['type']),
        openNow: asBool(j['open_now']),
        address: asStringOrNull(j['address']),
        city: asStringOrNull(j['city']),
        description: asStringOrNull(j['description']),
        imageUrl: asStringOrNull(j['image_url']),
        hours: asList(j['hours']).map(OpeningHours.fromJson).toList(),
        paymentMethods: ((j['payment_methods'] as List?) ?? const [])
            .map((e) => '$e')
            .toList(),
      );

  final String id, name, type;
  final bool openNow;
  final String? address, city, description, imageUrl;
  final List<OpeningHours> hours;
  final List<String> paymentMethods;

  String get place =>
      [address, city].whereType<String>().where((s) => s.isNotEmpty).join(', ');
}
