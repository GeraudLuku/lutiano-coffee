import 'package:flutter_test/flutter_test.dart';
import 'package:lutiano_core/testing.dart';
import 'package:lutiano_customer/features/rewards/reward_qr_screen.dart';
import 'package:lutiano_customer/features/rewards/rewards_catalogue_screen.dart';
import 'package:lutiano_customer/features/rewards/rewards_screen.dart';
import 'package:lutiano_customer/state/app_state.dart';
import 'package:material_ui/material_ui.dart';

import '../support/harness.dart';

const _overview = {
  'programme_active': true,
  'points_balance': 1060,
  'tier': {
    'code': 'BRONZE',
    'name': 'Bronze',
    'color': '#B87333',
    'min_annual_spend_xaf': 0,
    'multiplier_pct': 100
  },
  'next_tier': {
    'code': 'SILVER',
    'name': 'Silver',
    'color': '#9AA4B2',
    'min_annual_spend_xaf': 50000,
    'multiplier_pct': 125
  },
  'annual_spend_xaf': 20000,
  'remaining_to_next_xaf': 30000,
  'expiring_points': 0,
  'expiry_notice_days': 30,
  'rewards_available': 1,
  'min_redeem_points': 100,
};

Map<String, Object?> _wallet(String state) => {
      'id': 'w1',
      'reference': 'RWD-1',
      'reward_name': 'Free Espresso',
      'points': 400,
      'state': state,
      'valid_until': '2026-12-18T10:00:00Z',
      'qr': state == 'AVAILABLE' ? 'LUTIANO:RW:tok' : null,
    };

void main() {
  testWidgets('the Rewards tab shows points, tier progress and the way in',
      (tester) async {
    final api =
        FakeApi({'GET /customer/loyalty/overview': (_) => (200, _overview)});
    final state = AppState(api: api.client())..customer = null;
    await tester.pumpWidget(testApp(home: const RewardsScreen(), state: state));
    await tester.pumpAndSettle();
    expect(find.text('1060 pts'), findsOneWidget);
    expect(find.text('Bronze Member'), findsOneWidget);
    expect(find.textContaining('Silver'), findsWidgets);
    expect(find.text('Rewards Catalogue'), findsOneWidget);
    expect(find.text('1 ready to use'), findsOneWidget);
  });

  testWidgets('redeeming a reward spends points and shows its QR',
      (tester) async {
    tester.view.physicalSize = const Size(800, 1800);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    var redeemed = false;
    final api = FakeApi({
      'GET /customer/loyalty/rewards': (_) => (
            200,
            FakeApi.page([
              {
                'id': 'r1',
                'name': 'Free Espresso',
                'reward_type': 'FREE_PRODUCT',
                'point_cost': 400,
                'can_redeem': true,
                'blocked_by': null
              },
              {
                'id': 'r2',
                'name': '20% Discount Voucher',
                'reward_type': 'DISCOUNT_VOUCHER',
                'point_cost': 2000,
                'can_redeem': false,
                'blocked_by': 'NOT_ENOUGH_POINTS'
              },
            ])
          ),
      'POST /customer/loyalty/rewards/r1/redeem': (_) {
        redeemed = true;
        return (200, _wallet('AVAILABLE'));
      },
      'GET /customer/loyalty/redemptions/w1': (_) =>
          (200, _wallet('AVAILABLE')),
    });
    await tester.pumpWidget(testApp(
        home: const RewardsCatalogueScreen(),
        state: AppState(api: api.client())));
    await tester.pumpAndSettle();
    expect(find.text('Not enough points yet.'), findsOneWidget);

    await tester.tap(find.text('Free Espresso'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Redeem'));
    await tester.pumpAndSettle();
    expect(redeemed, isTrue);
    expect(find.text('Reward Redeemed!'), findsOneWidget);
    expect(find.text('Show this code to the seller.'), findsOneWidget);
  });

  testWidgets('a used reward shows no QR', (tester) async {
    final api = FakeApi({
      'GET /customer/loyalty/redemptions/w1': (_) =>
          (200, {..._wallet('USED'), 'used_at': '2026-11-18T11:00:00Z'})
    });
    await tester.pumpWidget(testApp(
        home: const RewardQrScreen(itemId: 'w1'),
        state: AppState(api: api.client())));
    await tester.pumpAndSettle();
    expect(find.text('Show this code to the seller.'), findsNothing);
    expect(find.textContaining('Used'), findsWidgets);
  });
}
