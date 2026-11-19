import 'dart:math';

import 'package:flutter/foundation.dart';

import '../data/models.dart';

class CartLine {
  CartLine(this.product, this.quantity);
  final Product product;
  int quantity;
  int get totalXaf => product.priceXaf * quantity;
}

/// The in-progress sale. It is bound to one sales operation and kept locally (a draft) until the
/// server confirms the sale, so a lost connection never loses the cart or completes a sale offline.
class CartController extends ChangeNotifier {
  final Map<String, CartLine> _lines = {};
  String? _operationId;
  String? _checkoutKey;
  ScannedCustomer? _customer;
  String? _customerToken;

  String? get operationId => _operationId;

  /// The customer whose loyalty QR was scanned for this sale, and the token the API needs to confirm them.
  ScannedCustomer? get customer => _customer;
  String? get customerToken => _customerToken;
  List<CartLine> get lines => _lines.values.toList(growable: false);
  bool get isEmpty => _lines.isEmpty;
  int get itemCount => _lines.values.fold(0, (n, l) => n + l.quantity);
  int get totalXaf => _lines.values.fold(0, (n, l) => n + l.totalXaf);
  int quantityOf(String productId) => _lines[productId]?.quantity ?? 0;

  /// Product id → quantity, as sent to the API.
  Map<String, int> get quantities =>
      {for (final l in _lines.values) l.product.id: l.quantity};

  /// The same key is reused for retries of the same cart, so a sale is never recorded twice.
  /// Any change to the cart starts a new key.
  String get checkoutKey => _checkoutKey ??= _newKey();

  /// A cart belongs to one operation; switching operation empties it.
  void bindTo(String operationId) {
    if (_operationId == operationId) return;
    _operationId = operationId;
    _lines.clear();
    _customer = null;
    _customerToken = null;
    _changed();
  }

  void add(Product product, {required int available}) {
    final current = quantityOf(product.id);
    if (current >= available) return;
    _lines.update(product.id, (l) => l..quantity += 1,
        ifAbsent: () => CartLine(product, 1));
    _changed();
  }

  void remove(String productId) {
    final line = _lines[productId];
    if (line == null) return;
    if (line.quantity <= 1) {
      _lines.remove(productId);
    } else {
      line.quantity -= 1;
    }
    _changed();
  }

  void deleteLine(String productId) {
    if (_lines.remove(productId) != null) _changed();
  }

  void attachCustomer(ScannedCustomer customer, String token) {
    _customer = customer;
    _customerToken = token;
    _changed();
  }

  void detachCustomer() {
    if (_customer == null) return;
    _customer = null;
    _customerToken = null;
    _changed();
  }

  void clear() {
    if (_lines.isEmpty && _checkoutKey == null && _customer == null) return;
    _lines.clear();
    _customer = null;
    _customerToken = null;
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
