import 'package:flutter_test/flutter_test.dart';
import 'package:lutiano_core/testing.dart';
import 'package:lutiano_customer/data/models.dart';
import 'package:lutiano_customer/state/app_state.dart';
import 'package:lutiano_customer/state/cart_controller.dart';

import '../support/fakes.dart';

CartLine _line({int quantity = 1, bool addon = false, String size = 'v1'}) {
  final item = MenuItem.fromJson(cappuccino);
  return CartLine.of(item,
      size: item.sizes.firstWhere((s) => s.id == size),
      addons: addon ? item.addons : [],
      quantity: quantity);
}

void main() {
  test(
      'the same product, size and add-ons is one line; another size is another line',
      () {
    final cart = CartController()..bindTo('op1');
    cart.add(_line());
    cart.add(_line(quantity: 2));
    cart.add(_line(size: 'v2'));
    cart.add(_line(addon: true));
    expect(cart.lines.length, 3);
    expect(cart.count, 5);
    expect(cart.totalXaf, 3 * 1200 + 2000 + 1500);
  });

  test('choosing another sales point starts an empty cart', () {
    final cart = CartController()..bindTo('op1');
    cart.add(_line());
    cart.bindTo('op1');
    expect(cart.isEmpty, isFalse);
    cart.bindTo('op2');
    expect(cart.isEmpty, isTrue);
  });

  test('the checkout key stays the same until the cart changes', () {
    final cart = CartController()..bindTo('op1');
    cart.add(_line());
    final key = cart.checkoutKey;
    expect(cart.checkoutKey, key);
    cart.setQuantity(cart.lines.single.key, 3);
    expect(cart.checkoutKey, isNot(key));
  });

  test('setting a quantity of zero removes the line', () {
    final cart = CartController()..bindTo('op1');
    cart.add(_line());
    cart.setQuantity(cart.lines.single.key, 0);
    expect(cart.isEmpty, isTrue);
  });

  test('the app hears every cart change, so the cart badges update', () {
    final app = AppState(api: FakeApi({}).client());
    var heard = 0;
    app.addListener(() => heard++);
    app.cart.bindTo('op1');
    app.cart.add(_line());
    expect(heard, greaterThanOrEqualTo(1));
    expect(app.cart.count, 1);
  });
}
