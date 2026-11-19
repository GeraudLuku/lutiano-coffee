import 'package:flutter_test/flutter_test.dart';
import 'package:lutiano_seller/data/models.dart';
import 'package:lutiano_seller/state/cart_controller.dart';

Product product(String id, int price) => Product(
    id: id,
    sku: id,
    name: id,
    category: 'Drinks',
    priceXaf: price,
    lowStockThreshold: 5);

void main() {
  late CartController cart;
  setUp(() => cart = CartController()..bindTo('op1'));

  test('adds, totals and removes lines', () {
    cart.add(product('a', 1500), available: 10);
    cart.add(product('a', 1500), available: 10);
    cart.add(product('b', 2000), available: 10);
    expect(cart.itemCount, 3);
    expect(cart.totalXaf, 5000);
    cart.remove('a');
    expect(cart.quantityOf('a'), 1);
    cart.remove('a');
    expect(cart.quantityOf('a'), 0);
  });

  test('never sells more than the stock available', () {
    for (var i = 0; i < 5; i++) {
      cart.add(product('a', 1000), available: 2);
    }
    expect(cart.quantityOf('a'), 2);
  });

  test(
      'retries of the same cart reuse one idempotency key; any change makes a new one',
      () {
    cart.add(product('a', 1000), available: 5);
    final first = cart.checkoutKey;
    expect(cart.checkoutKey, first);
    cart.add(product('a', 1000), available: 5);
    expect(cart.checkoutKey, isNot(first));
  });

  test('switching operation empties the cart', () {
    cart.add(product('a', 1000), available: 5);
    cart.bindTo('op2');
    expect(cart.isEmpty, true);
  });

  test(
      'a customer is kept with the cart, changes the idempotency key, and goes with the cart',
      () {
    cart.add(product('a', 1000), available: 5);
    final before = cart.checkoutKey;
    cart.attachCustomer(
        const ScannedCustomer(fullName: 'Ann', code: 'CU-1', pointsBalance: 10),
        'tok');
    expect(cart.customerToken, 'tok');
    expect(cart.checkoutKey, isNot(before));
    cart.detachCustomer();
    expect(cart.customer, isNull);
    cart.attachCustomer(
        const ScannedCustomer(fullName: 'Ann', code: 'CU-1', pointsBalance: 10),
        'tok');
    cart.clear();
    expect(cart.customer, isNull);
    expect(cart.customerToken, isNull);
  });

  test('switching operation drops the customer too', () {
    cart.attachCustomer(
        const ScannedCustomer(fullName: 'Ann', code: 'CU-1', pointsBalance: 10),
        'tok');
    cart.bindTo('op2');
    expect(cart.customer, isNull);
  });
}
