import 'package:flutter_test/flutter_test.dart';
import 'package:lutiano_core/lutiano_core.dart';
import 'package:lutiano_seller/features/earnings/commission_breakdown_screen.dart';
import 'package:lutiano_seller/features/earnings/commission_rules_screen.dart';
import 'package:lutiano_seller/features/earnings/earning_detail_screen.dart';
import 'package:lutiano_seller/features/earnings/earnings_analytics_screen.dart';
import 'package:lutiano_seller/features/earnings/earnings_by_operation_screen.dart';
import 'package:lutiano_seller/features/earnings/earnings_screen.dart';
import 'package:lutiano_seller/features/earnings/payout_details_screen.dart';
import 'package:lutiano_seller/features/earnings/payouts_screen.dart';
import 'package:lutiano_seller/features/earnings/statement_export_screen.dart';
import 'package:lutiano_seller/state/app_scope.dart';
import 'package:lutiano_seller/state/app_state.dart';
import 'package:material_ui/material_ui.dart';

import '../support/fake_api.dart';
import '../support/harness.dart';

Map<String, Object?> _month(String month,
        {int commission = 20000,
        int bonus = 0,
        int adjustments = 0,
        String status = 'FINAL',
        Map<String, Object?> extra = const {}}) =>
    {
      'month': month,
      'starts_on': '$month-01',
      'ends_on': '$month-28',
      'status': status,
      'sales_count': 10,
      'sales_xaf': 400000,
      'commission_xaf': commission,
      'bonus_xaf': bonus,
      'adjustments_xaf': adjustments,
      'total_xaf': commission + bonus + adjustments,
      ...extra,
    };

Map<String, Object?> _page(List<Object?> items,
        {Map<String, Object?> summary = const {}}) =>
    {
      'items': items,
      'page': 1,
      'size': 20,
      'total': items.length,
      'pages': 1,
      'summary': summary
    };

AppState _state(FakeApi api) => AppState(api: api.client());

Widget _host(AppState state, Widget home) =>
    AppScope(state: state, child: testApp(home: home));

void main() {
  testWidgets(
      'the overview shows the month, the change on the month before, that it is provisional, and what is still owed',
      (tester) async {
    final api = FakeApi({
      'GET /me/earnings/overview': (r) => (
            200,
            _month(r.url.queryParameters['month'] ?? currentMonth(),
                commission: 150000,
                bonus: 50000,
                adjustments: 10500,
                status: 'PROVISIONAL',
                extra: {
                  'previous_total_xaf': 188000,
                  'balance': {
                    'owed_xaf': 90000,
                    'paid_xaf': 40000,
                    'pending_xaf': 0,
                    'outstanding_xaf': 50000
                  }
                })
          ),
    });
    await tester.pumpWidget(_host(_state(api), const EarningsScreen()));
    await tester.pumpAndSettle();
    expect(find.text('210,500 XAF'), findsOneWidget);
    expect(find.text('Provisional'), findsOneWidget);
    expect(find.textContaining('+12%'), findsOneWidget,
        reason: '210,500 against 188,000');
    expect(find.text('150,000 XAF'), findsOneWidget);
    expect(find.text('+10,500 XAF'), findsOneWidget);
    expect(find.text('50,000 XAF'), findsWidgets,
        reason: 'the bonus and what is still to be paid');
    expect(find.textContaining('Provisional: amounts become final'),
        findsOneWidget);
    expect(api.uris.single.queryParameters['month'], currentMonth());
  });

  testWidgets('choosing another month asks the server for that month',
      (tester) async {
    final api = FakeApi({
      'GET /me/earnings/overview': (r) =>
          (200, _month(r.url.queryParameters['month']!)),
    });
    await tester.pumpWidget(_host(_state(api), const EarningsScreen()));
    await tester.pumpAndSettle();
    final before = recentMonths(2)[1];
    await tester.tap(find.byType(DropdownButton<String>));
    await tester.pumpAndSettle();
    await tester.tap(find.text(monthLabel(before)).last);
    await tester.pumpAndSettle();
    expect(api.uris.last.queryParameters['month'], before);
    expect(find.text('Final'), findsOneWidget);
  });

  testWidgets(
      'the breakdown splits commission by source and product with shares',
      (tester) async {
    final api = FakeApi({
      'GET /me/earnings/breakdown': (_) => (
            200,
            {
              'commission_xaf': 150000,
              'product_commission_xaf': 135000,
              'addon_commission_xaf': 10000,
              'bonus_xaf': 50000,
              'adjustments_xaf': -500,
              'top_products': [
                {
                  'product_name': 'Cappuccino',
                  'units': 40,
                  'commission_xaf': 60000
                },
                {'product_name': 'Latte', 'units': 30, 'commission_xaf': 45000},
              ]
            }
          ),
    });
    await tester.pumpWidget(
        _host(_state(api), CommissionBreakdownScreen(month: currentMonth())));
    await tester.pumpAndSettle();
    expect(find.text('135,000 XAF'), findsOneWidget);
    expect(find.text('90%'), findsOneWidget);
    expect(find.text('60,000 XAF'), findsOneWidget);
    expect(find.text('40%'), findsOneWidget);
    expect(find.text('-500 XAF'), findsOneWidget);
  });

  testWidgets('a sale shows each line, its rule, and what a refund took back',
      (tester) async {
    final api = FakeApi({
      'GET /me/earnings/sales/s1': (_) => (
            200,
            {
              'sale_id': 's1',
              'reference': 'SL-1',
              'created_at': '2026-11-17T10:00:00Z',
              'total_xaf': 3000,
              'operation_name': 'Bonapriso Cafe',
              'commission_xaf': 150,
              'reversed_xaf': 50,
              'lines': [
                {
                  'product_name': 'Latte',
                  'quantity': 1,
                  'line_value_xaf': 3000,
                  'basis': 'PERCENT',
                  'rate': 500,
                  'commission_xaf': 150,
                  'reversed_xaf': 50
                }
              ]
            }
          ),
    });
    await tester.pumpWidget(
        _host(_state(api), const EarningDetailScreen(saleId: 's1')));
    await tester.pumpAndSettle();
    expect(find.textContaining('Latte × 1'), findsOneWidget);
    expect(find.textContaining('5.0% of the sale value'), findsOneWidget);
    expect(find.text('-50 XAF'), findsOneWidget);
    expect(find.text('Net commission'), findsOneWidget);
    expect(find.text('100 XAF'), findsOneWidget);
  });

  group('payouts', () {
    final payout = {
      'id': 'po1',
      'reference': 'PO-20260305-00003',
      'amount_xaf': 200000,
      'method': 'MTN_MOMO',
      'status': 'PAID',
      'requested_at': '2026-03-05T08:00:00Z',
      'paid_at': '2026-03-05T13:14:00Z',
      'destination_name': 'Martin N.',
      'destination_last4': '4821',
      'provider_reference': 'TRF78456321',
      'period_starts_on': '2026-02-01',
      'period_ends_on': '2026-02-28',
      'period_id': 'ep1',
    };

    testWidgets('the list counts every payout and filters by group',
        (tester) async {
      final api = FakeApi({
        'GET /me/earnings/payouts': (_) => (
              200,
              _page([
                payout
              ], summary: {
                'completed': 3,
                'pending': 1,
                'cancelled': 0,
                'paid_out_xaf': 480000
              })
            ),
      });
      await tester.pumpWidget(_host(_state(api), const PayoutsScreen()));
      await tester.pumpAndSettle();
      expect(find.text('480,000 XAF'), findsOneWidget);
      expect(find.text('PO-20260305-00003'), findsOneWidget);
      await tester.tap(find.text('Pending').first);
      await tester.pumpAndSettle();
      expect(api.uris.last.queryParameters['status'], 'PENDING');
    });

    testWidgets('the details show the account with only its last four digits',
        (tester) async {
      final api = FakeApi({
        'GET /me/earnings/payouts/po1': (_) => (
              200,
              {
                ...payout,
                'statement': {
                  'commission_xaf': 150000,
                  'bonus_xaf': 50000,
                  'adjustments_xaf': 0,
                  'total_xaf': 200000,
                  'payable_xaf': 200000
                }
              }
            ),
      });
      await tester.pumpWidget(
          _host(_state(api), const PayoutDetailsScreen(payoutId: 'po1')));
      await tester.pumpAndSettle();
      expect(find.text('Martin N. •••• 4821'), findsOneWidget);
      expect(find.text('TRF78456321'), findsOneWidget);
      expect(find.text('200,000 XAF'), findsWidgets);
      expect(find.text('Completed'), findsOneWidget);
    });
  });

  testWidgets('commission rules are described in words, with the role note',
      (tester) async {
    final api = FakeApi({
      'GET /me/earnings/rules': (_) => (
            200,
            _page([
              {
                'product_name': 'Latte',
                'category': 'Drinks',
                'basis': 'PERCENT',
                'rate': 500
              },
              {
                'product_name': 'Beans',
                'category': 'Retail',
                'basis': 'FIXED_PER_UNIT',
                'rate': 300
              },
            ])
          ),
    });
    await tester.pumpWidget(_host(_state(api), const CommissionRulesScreen()));
    await tester.pumpAndSettle();
    expect(find.text('5.0% of the sale value'), findsOneWidget);
    expect(find.text('300 XAF per unit'), findsOneWidget);
    expect(find.textContaining('Only validated, non-refunded sales count'),
        findsOneWidget);
  });

  testWidgets('operations show sales, commission and the rate', (tester) async {
    final api = FakeApi({
      'GET /me/earnings/operations': (_) => (
            200,
            [
              {
                'operation_id': 'o1',
                'operation_name': 'Bonapriso',
                'sales_count': 42,
                'sales_xaf': 3000000,
                'commission_xaf': 150000
              }
            ]
          ),
    });
    await tester.pumpWidget(
        _host(_state(api), EarningsByOperationScreen(month: currentMonth())));
    await tester.pumpAndSettle();
    expect(find.text('Bonapriso'), findsOneWidget);
    expect(find.text('5.0%'), findsOneWidget);
    expect(find.text('42'), findsOneWidget);
  });

  testWidgets(
      'trends compare the range with the one before it and say what stands out',
      (tester) async {
    // 12 months requested for a 6 month range: the first 6 are the range before
    final api = FakeApi({
      'GET /me/earnings/months': (r) => (
            200,
            [
              for (var i = 0;
                  i < int.parse(r.url.queryParameters['count']!);
                  i++)
                _month(
                    recentMonths(int.parse(r.url.queryParameters['count']!))
                        .reversed
                        .toList()[i],
                    commission: i >= 6 ? 20000 + (i - 6) * 10000 : 10000,
                    bonus: i == 11 ? 5000 : 0)
            ]
          ),
    });
    await tester
        .pumpWidget(_host(_state(api), const EarningsAnalyticsScreen()));
    await tester.pumpAndSettle();
    expect(api.uris.single.queryParameters['count'], '12');
    expect(
        find.textContaining('compared with the month before'), findsOneWidget);
    expect(find.textContaining('Commissions make up'), findsOneWidget);
    expect(find.textContaining('was your best month'), findsOneWidget);
  });

  testWidgets('the statement can only be exported with at least one section',
      (tester) async {
    tester.view.physicalSize = const Size(800, 1600);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    await tester
        .pumpWidget(_host(_state(FakeApi({})), const StatementExportScreen()));
    await tester.pumpAndSettle();
    expect(find.text('Export Statement (PDF)'), findsOneWidget);
    for (final box
        in tester.widgetList<Checkbox>(find.byType(Checkbox)).toList()) {
      expect(box.value, true);
    }
    for (var i = 0; i < 5; i++) {
      await tester.tap(find.byType(Checkbox).at(i));
    }
    await tester.pumpAndSettle();
    expect(find.text('Choose at least one section.'), findsOneWidget);
    final button = tester.widget<FilledButton>(find.byType(FilledButton));
    expect(button.onPressed, isNull);
  });

  test('the statement request names the months, sections and language',
      () async {
    final api = FakeApi({
      'GET /me/earnings/statement.pdf': (_) => (200, <String, Object?>{}),
    });
    final state = _state(api);
    await state.earnings.statementPdf(
        from: '2026-01',
        to: '2026-03',
        sections: {'SUMMARY', 'SALES'},
        language: 'fr');
    final q = api.uris.single.queryParameters;
    expect(q['from'], '2026-01');
    expect(q['to'], '2026-03');
    expect(q['sections'], 'SUMMARY,SALES');
    expect(q['lang'], 'fr');
  });
}
