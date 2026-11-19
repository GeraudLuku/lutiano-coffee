Map<String, Object?> customerJson(
        {String name = 'Ada Lovelace', bool twoFactor = false}) =>
    {
      'id': 'c1',
      'code': 'CU-000001',
      'full_name': name,
      'phone': '+237677112233',
      'email': null,
      'language': 'en',
      'two_factor_enabled': twoFactor,
      'has_password': true,
      'marketing_consent': false,
      'tier_code': 'BRONZE',
      'loyalty_qr': 'LUTIANO:CU:abc',
      'linked_accounts': <Object?>[],
    };

Map<String, Object?> sessionJson() => {
      'token': 'tok',
      'expiresAt': '2026-12-01T00:00:00Z',
      'customer': customerJson()
    };

const cappuccino = {
  'id': 'p1',
  'name': 'Cappuccino',
  'category': 'Beverage',
  'description': 'Espresso with steamed milk',
  'image_url': null,
  'kind': 'PARENT',
  'price_xaf': 1200,
  'variants': [
    {'id': 'v1', 'parent_id': 'p1', 'variant_name': 'Small', 'price_xaf': 1200},
    {'id': 'v2', 'parent_id': 'p1', 'variant_name': 'Large', 'price_xaf': 2000},
  ],
  'addons': [
    {'id': 'a1', 'name': 'Caramel Syrup', 'price_xaf': 300},
  ],
};

const croissant = {
  'id': 'p2',
  'name': 'Butter Croissant',
  'category': 'Food',
  'description': null,
  'image_url': null,
  'kind': 'SIMPLE',
  'price_xaf': 1500,
  'variants': <Object?>[],
  'addons': <Object?>[],
};

const salesPointJson = {
  'id': 'op1',
  'name': 'Bonanjo Cafe',
  'code': 'LTN-001',
  'type': 'FIXED_POS',
  'address': 'Bonanjo',
  'city': 'Douala',
  'description': null,
  'image_url': null,
  'open_now': true,
  'hours': [
    {'weekday': 1, 'opens': '07:00:00', 'closes': '19:00:00'}
  ],
  'payment_methods': ['CASH'],
};
