import 'package:lutiano_seller/data/models.dart';

/// Sample records shared by the widget tests (JSON as the API sends it, and the matching models).
const operationJson = {
  'id': 'op1',
  'name': 'Bonapriso Cafe',
  'code': 'LTN-001',
  'type': 'FIXED_POS',
  'active': true,
  'city': 'Douala'
};

final operation = Operation.fromJson(operationJson);

const latte = Product(
    id: 'p1',
    sku: 'LAT',
    name: 'Latte',
    category: 'Drinks',
    priceXaf: 2000,
    lowStockThreshold: 5);

Map<String, Object?> saleJson(
        {String reference = 'SL-1',
        int total = 4000,
        int cash = 5000,
        String? customer,
        int? points}) =>
    {
      'id': 's1',
      'reference': reference,
      'total_xaf': total,
      'created_at': '2026-11-17T10:00:00Z',
      'operation_name': 'Bonapriso Cafe',
      'item_count': 2,
      'payment_method': 'CASH',
      'cash_received_xaf': cash,
      'change_xaf': cash - total,
      if (customer != null) 'customer_name': customer,
      if (points != null) 'loyalty': {'total_points': points},
    };

const cashMethods = [
  {'code': 'CASH', 'label': 'Cash', 'available': true, 'enabled': true},
  {
    'code': 'MTN_MOMO',
    'label': 'MTN Mobile Money',
    'available': false,
    'enabled': false
  },
];

const openShiftJson = {
  'id': 'sh1',
  'opening_float': 10000,
  'expected_cash': 14000,
  'opened_at': '2026-11-17T07:00:00Z',
  'sale_count': 2
};
