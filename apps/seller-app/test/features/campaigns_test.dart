import 'package:flutter_test/flutter_test.dart';
import 'package:lutiano_seller/features/targets/campaigns_screen.dart';
import 'package:lutiano_seller/state/app_scope.dart';
import 'package:lutiano_seller/state/app_state.dart';

import '../support/fake_api.dart';
import '../support/harness.dart';

Map<String, Object?> _campaign(
        {int sales = 320000, String state = 'CURRENT'}) =>
    {
      'id': 'c1',
      'name': 'Spring Boost',
      'description': 'Sell more coffee',
      'operation_name': null,
      'starts_on': '2026-11-01',
      'ends_on': '2026-11-30',
      'state': state,
      'sales_xaf': sales,
      'bonus_xaf': 5000,
      'next_threshold_xaf': 500000,
      'next_bonus_xaf': 25000,
      'paid_bonus': null,
      'tiers': [
        {'threshold_xaf': 200000, 'bonus_xaf': 5000},
        {'threshold_xaf': 500000, 'bonus_xaf': 25000},
      ],
    };

void main() {
  testWidgets('campaigns show the seller\'s progress toward the next tier',
      (tester) async {
    final api = FakeApi({
      'GET /me/campaigns': (_) => (200, FakeApi.page([_campaign()])),
      'GET /me/campaigns/c1': (_) => (200, _campaign()),
    });
    final state = AppState(api: api.client());
    await tester.pumpWidget(
        AppScope(state: state, child: testApp(home: const CampaignsScreen())));
    await tester.pumpAndSettle();

    expect(find.text('Spring Boost'), findsOneWidget);
    expect(find.textContaining('XAF'), findsWidgets);
    expect(api.uris.first.queryParameters['state'], 'CURRENT');

    await tester.tap(find.text('Spring Boost'));
    await tester.pumpAndSettle();
    expect(find.text('Tiers'), findsOneWidget);
    expect(find.textContaining('Reach'), findsOneWidget);
  });

  testWidgets('no campaign shows an empty message', (tester) async {
    final api = FakeApi({'GET /me/campaigns': (_) => (200, FakeApi.page([]))});
    await tester.pumpWidget(AppScope(
        state: AppState(api: api.client()),
        child: testApp(home: const CampaignsScreen())));
    await tester.pumpAndSettle();
    expect(find.text('No campaigns'), findsOneWidget);
  });
}
