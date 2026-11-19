import 'dart:math';

import 'package:flutter/foundation.dart';

import '../data/models.dart';

/// One line of the cart: a product (and size) with add-ons and a quantity. Lines are told apart by product, size and add-ons.
class CartLine {
  CartLine(
      {required this.productId,
      required this.name,
      required this.baseXaf,
      required this.addons,
      required this.quantity,
      this.sizeName});

  /// A line for a product on the menu, in a size if it has sizes.
  factory CartLine.of(MenuItem item,
          {ProductSize? size,
          List<Addon> addons = const [],
          int quantity = 1}) =>
      CartLine(
          productId: size?.id ?? item.id,
          name: item.name,
          sizeName: size?.name,
          baseXaf: size?.priceXaf ?? item.priceXaf,
          addons: addons,
          quantity: quantity);

  /// The id the API sells: the size when there is one, otherwise the product.
  final String productId;
  final String name;
  final String? sizeName;

  /// The price of the product (or size) without add-ons.
  final int baseXaf;
  final List<Addon> addons;
  int quantity;

  String get key =>
      '$productId|${(addons.map((a) => a.id).toList()..sort()).join(',')}';
  int get unitXaf => baseXaf + addons.fold(0, (s, a) => s + a.priceXaf);
  int get totalXaf => unitXaf * quantity;
}

/// What the customer is about to order. A cart belongs to one sales point: choosing another empties it.
class CartController extends ChangeNotifier {
  final Map<String, CartLine> _lines = {};
  String? _salesPointId;
  String? _checkoutKey;

  String? get salesPointId => _salesPointId;
  List<CartLine> get lines => _lines.values.toList();
  bool get isEmpty => _lines.isEmpty;
  int get count => _lines.values.fold(0, (s, l) => s + l.quantity);
  int get totalXaf => _lines.values.fold(0, (s, l) => s + l.totalXaf);

  /// Sent with the order so a retry after a bad network never places it twice. Changes when the cart changes.
  String get checkoutKey => _checkoutKey ??= _newKey();

  /// Ties the cart to a sales point; a different one starts an empty cart.
  void bindTo(String salesPointId) {
    if (_salesPointId == salesPointId) return;
    _salesPointId = salesPointId;
    _lines.clear();
    _changed();
  }

  void add(CartLine line) {
    final existing = _lines[line.key];
    if (existing == null) {
      _lines[line.key] = line;
    } else {
      existing.quantity += line.quantity;
    }
    _changed();
  }

  void setQuantity(String key, int quantity) {
    if (quantity <= 0) {
      _lines.remove(key);
    } else {
      _lines[key]?.quantity = quantity;
    }
    _changed();
  }

  void clear() {
    _lines.clear();
    _changed();
  }

  void _changed() {
    _checkoutKey = null;
    notifyListeners();
  }

  static String _newKey() {
    final r = Random.secure();
    return List.generate(
        16, (_) => r.nextInt(256).toRadixString(16).padLeft(2, '0')).join();
  }
}
