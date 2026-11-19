import 'package:flutter_test/flutter_test.dart';
import 'package:lutiano_seller/features/targets/bonus_details_screen.dart';
import 'package:lutiano_seller/features/targets/bonus_eligibility_screen.dart';
import 'package:lutiano_seller/features/targets/bonus_history_screen.dart';
import 'package:lutiano_seller/features/targets/my_targets_screen.dart';
import 'package:lutiano_seller/features/targets/performance_screen.dart';
import 'package:lutiano_seller/features/targets/target_details_screen.dart';
import 'package:lutiano_seller/features/targets/target_rules_screen.dart';
import 'package:lutiano_seller/features/targets/target_trend_screen.dart';
import 'package:lutiano_seller/features/targets/targets_screen.dart';
import 'package:lutiano_seller/state/app_scope.dart';
import 'package:lutiano_seller/state/app_state.dart';
import 'package:material_ui/material_ui.dart';

import '../support/fake_api.dart';
import '../support/harness.dart';

Map<String, Object?> _target({
  String id = 't1',
  String name = 'Monthly sales March',
  int target = 2500000,
  int achieved = 1950000,
  double pct = 78,
  bool reached = false,
  String state = 'CURRENT',
  String bonusState = 'IN_PROGRESS',
  int bonus = 50000,
  int? paid,
  String start = '2026-03-01',
  String end = '2026-03-31',
}) =>
    {
      'id': id,
      'name': name,
      'metric': 'VALUE',
      'period_type': 'MONTHLY',
      'starts_on': start,
      'ends_on': end,
      'target_value': target,
      'bonus_type': 'FIXED',
      'bonus_xaf': bonus,
      'bonus_paid_xaf': paid,
      'achieved': achieved,
      'progress_pct': pct,
      'reached': reached,
      'bonus_earned': reached ? bonus : 0,
      'state': state,
      'bonus_state': bonusState,
      'bonus_month': end.substring(0, 7),
    };

Map<String, Object?> _page(List<Object?> items) => {
      'items': items,
      'page': 1,
      'size': 20,
      'total': items.length,
      'pages': 1,
      'summary': {}
    };

AppState _state(FakeApi api) => AppState(api: api.client());
Widget _host(AppState state, Widget home) =>
    AppScope(state: state, child: testApp(home: home));

void main() {
  testWidgets(
      'the overview shows the main target as a ring with its figures and the counts',
      (tester) async {
    final api = FakeApi({
      'GET /me/targets/summary': (_) => (
            200,
            {
              'month': '2026-03',
              'targets': 2,
              'current': 2,
              'reached': 1,
              'bonus_pending': 1,
              'bonus_pending_xaf': 50000,
              'bonus_included': 0,
              'bonus_included_xaf': 0,
              'main': _target(),
            }
          ),
    });
    await tester.pumpWidget(_host(_state(api), const TargetsScreen()));
    await tester.pumpAndSettle();
    expect(find.text('78%'), findsOneWidget);
    expect(find.text('2,500,000 XAF'), findsOneWidget);
    expect(find.text('1,950,000 XAF'), findsOneWidget);
    expect(find.text('550,000 XAF'), findsOneWidget,
        reason: 'what is left to reach');
    expect(find.text('Active targets'), findsOneWidget);
    expect(find.text('Bonus earned'), findsOneWidget);
    expect(api.uris.single.queryParameters['month'], isNotNull);
  });

  testWidgets('a month without a target says so', (tester) async {
    final api = FakeApi({
      'GET /me/targets/summary': (_) => (
            200,
            {
              'month': '2026-03',
              'targets': 0,
              'current': 0,
              'reached': 0,
              'bonus_pending': 0,
              'bonus_pending_xaf': 0,
              'bonus_included': 0,
              'bonus_included_xaf': 0,
              'main': null
            }
          ),
    });
    await tester.pumpWidget(_host(_state(api), const TargetsScreen()));
    await tester.pumpAndSettle();
    expect(find.text('No target this month'), findsOneWidget);
  });

  testWidgets('My Targets lists them with progress and filters by state',
      (tester) async {
    final api = FakeApi({
      'GET /me/targets': (_) => (
            200,
            _page([
              _target(),
              _target(
                  id: 't2',
                  name: 'February',
                  state: 'ENDED',
                  pct: 105,
                  reached: true)
            ])
          ),
    });
    await tester.pumpWidget(_host(_state(api), const MyTargetsScreen()));
    await tester.pumpAndSettle();
    expect(find.text('Monthly sales March'), findsOneWidget);
    expect(find.text('105%'), findsOneWidget,
        reason: 'past the target shows the real percentage');
    await tester.tap(find.text('Completed').first);
    await tester.pumpAndSettle();
    expect(api.uris.last.queryParameters['state'], 'ENDED');
  });

  testWidgets(
      'target details show the goal, progress and reward, and the way to the bonus screens',
      (tester) async {
    final api = FakeApi({'GET /me/targets/t1': (_) => (200, _target())});
    await tester.pumpWidget(
        _host(_state(api), const TargetDetailsScreen(targetId: 't1')));
    await tester.pumpAndSettle();
    expect(find.text('Monthly sales March'), findsOneWidget);
    expect(find.text('78%'), findsOneWidget);
    expect(find.text('50,000 XAF (fixed)'), findsOneWidget);
    expect(find.textContaining('Reach 100%'), findsOneWidget);
    expect(find.text('Bonus Eligibility'), findsOneWidget);
  });

  group('bonus eligibility', () {
    Future<void> open(WidgetTester tester, Map<String, Object?> target) async {
      final api = FakeApi({'GET /me/targets/t1': (_) => (200, target)});
      await tester.pumpWidget(
          _host(_state(api), const BonusEligibilityScreen(targetId: 't1')));
      await tester.pumpAndSettle();
    }

    testWidgets('a reached target is eligible', (tester) async {
      await open(
          tester,
          _target(
              reached: true,
              pct: 110,
              achieved: 2750000,
              bonusState: 'PENDING'));
      expect(find.text('You are eligible for this bonus!'), findsOneWidget);
      expect(find.text('Target reached'), findsOneWidget);
    });

    testWidgets('a target not reached yet says how far away it is',
        (tester) async {
      // a whole month long, started long ago: the seller at 10% is behind
      await open(
          tester,
          _target(
              pct: 10,
              achieved: 250000,
              start: '2000-01-01',
              end: '2030-12-31'));
      expect(find.text('Not eligible yet'), findsOneWidget);
      expect(find.text('Behind'), findsOneWidget);
      expect(
          find.textContaining('away from reaching the target'), findsOneWidget);
      expect(find.text('Remaining: 2,250,000 XAF'), findsOneWidget);
    });

    testWidgets('a target that ended short is not reached', (tester) async {
      await open(
          tester, _target(pct: 60, state: 'ENDED', bonusState: 'NOT_REACHED'));
      expect(find.text('Not reached'), findsOneWidget);
    });
  });

  group('bonus details', () {
    Future<void> open(WidgetTester tester, Map<String, Object?> target) async {
      final api = FakeApi({'GET /me/targets/t1': (_) => (200, target)});
      await tester.pumpWidget(
          _host(_state(api), const BonusDetailsScreen(targetId: 't1')));
      await tester.pumpAndSettle();
    }

    testWidgets('a reached bonus waits for the month to close', (tester) async {
      await open(
          tester, _target(reached: true, pct: 120, bonusState: 'PENDING'));
      expect(find.text('Pending'), findsOneWidget);
      expect(
          find.textContaining('joins your earnings when the month is closed'),
          findsOneWidget);
      expect(find.text('After March 2026 is closed'), findsOneWidget);
    });

    testWidgets('an included bonus names the month of the earnings',
        (tester) async {
      await open(
          tester,
          _target(
              reached: true,
              pct: 120,
              bonusState: 'INCLUDED',
              paid: 50000,
              state: 'ENDED'));
      expect(find.text('In earnings'), findsOneWidget);
      expect(
          find.text('Included in the earnings of March 2026'), findsOneWidget);
    });

    testWidgets('a reversed bonus says why', (tester) async {
      await open(
          tester,
          _target(
              pct: 90, bonusState: 'REVERSED', paid: 50000, state: 'ENDED'));
      expect(find.text('Reversed'), findsOneWidget);
      expect(find.textContaining('taken back'), findsOneWidget);
    });
  });

  testWidgets('bonus history lists earned bonuses and filters by status',
      (tester) async {
    final api = FakeApi({
      'GET /me/targets/bonuses': (_) => (
            200,
            _page([_target(reached: true, pct: 110, bonusState: 'PENDING')])
          ),
    });
    await tester.pumpWidget(_host(_state(api), const BonusHistoryScreen()));
    await tester.pumpAndSettle();
    expect(find.text('50,000 XAF'), findsOneWidget);
    expect(find.text('March 2026'), findsOneWidget);
    await tester.tap(find.text('In earnings'));
    await tester.pumpAndSettle();
    expect(api.uris.last.queryParameters['status'], 'INCLUDED');
  });

  testWidgets('performance shows totals and each month', (tester) async {
    final api = FakeApi({
      'GET /me/targets/performance': (_) => (
            200,
            {
              'total_sales_xaf': 5950000,
              'targets': 3,
              'reached': 1,
              'bonus_xaf': 100000,
              'months': [
                {
                  'month': '2026-01',
                  'sales_xaf': 1900000,
                  'targets': 1,
                  'reached': 0,
                  'avg_progress_pct': 95.0
                },
                {
                  'month': '2026-02',
                  'sales_xaf': 2100000,
                  'targets': 1,
                  'reached': 1,
                  'avg_progress_pct': 105.0
                },
                {
                  'month': '2026-03',
                  'sales_xaf': 1950000,
                  'targets': 1,
                  'reached': 0,
                  'avg_progress_pct': 78.0
                },
              ]
            }
          ),
    });
    await tester.pumpWidget(_host(_state(api), const PerformanceScreen()));
    await tester.pumpAndSettle();
    expect(find.text('5,950,000 XAF'), findsOneWidget);
    expect(find.text('1 / 3'), findsOneWidget);
    expect(find.text('100,000 XAF'), findsOneWidget);
    expect(find.text('105%'), findsOneWidget);
    expect(api.uris.single.queryParameters['count'], '3');
  });

  testWidgets('the trend compares the range with the one before it',
      (tester) async {
    final api = FakeApi({
      'GET /me/targets/performance': (r) {
        final n = int.parse(r.url.queryParameters['count']!);
        return (
          200,
          {
            'total_sales_xaf': 0,
            'targets': n,
            'reached': 0,
            'bonus_xaf': 0,
            'months': [
              for (var i = 0; i < n; i++)
                {
                  'month': '2026-${(i % 12 + 1).toString().padLeft(2, '0')}',
                  'sales_xaf': 1000000,
                  'targets': 1,
                  'reached': 0,
                  // the earlier half averages 50%, the later half 100%
                  'avg_progress_pct': i < n ~/ 2 ? 50.0 : 100.0,
                }
            ]
          }
        );
      },
    });
    await tester.pumpWidget(_host(_state(api), const TargetTrendScreen()));
    await tester.pumpAndSettle();
    expect(api.uris.single.queryParameters['count'], '12');
    expect(find.text('100%'), findsOneWidget);
    expect(find.text('+100%'), findsOneWidget,
        reason: '100% against 50% before');
  });

  testWidgets('the rules explain how targets and bonuses work', (tester) async {
    await tester
        .pumpWidget(_host(_state(FakeApi({})), const TargetRulesScreen()));
    await tester.pumpAndSettle();
    expect(find.textContaining('A bonus is earned when you reach 100%'),
        findsOneWidget);
    expect(find.textContaining('takes the bonus back'), findsOneWidget);
  });
}
